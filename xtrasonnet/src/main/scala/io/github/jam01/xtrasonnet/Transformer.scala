package io.github.jam01.xtrasonnet

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.Transformer.{ERROR_LINE_REGEX, handleException, main}
import io.github.jam01.xtrasonnet.document.Document.BasicDocument
import io.github.jam01.xtrasonnet.document.{Document, MediaType, MediaTypes}
import io.github.jam01.xtrasonnet.header.Header
import io.github.jam01.xtrasonnet.spi.Library
import io.github.jam01.xtrasonnet.spi.Library.{dummyPosition, memberOf}
import sjsonnet.Expr.Params
import sjsonnet.ScopedExprTransform.{Scope, ScopedVal, emptyScope}
import sjsonnet.{CachedResolver, DefaultParseCache, Error, EvalScope, Evaluator, Expr, FileScope, Importer, Materializer, ParseCache, ParseError, Path, Position, Settings, StaticOptimizer, Val, ValScope}

import java.util.Collections
import scala.collection.{Seq, immutable, mutable}
import scala.jdk.CollectionConverters.{IterableHasAsScala, MapHasAsScala}
import scala.util.control.NonFatal

object Transformer {
  val main = "(main)"

  // We wrap the script as function in order to pass in payload, and named inputs
  // see the 'Top-level arguments' section in https://jsonnet.org/learning/tutorial.html#parameterize-entire-config
  private def asFunction(script: String, argumentNames: Iterable[String]) =
    (Seq("payload") ++ argumentNames).mkString("function(", ", ", ")\n") + script

  private val ERROR_LINE_REGEX = raw":(\d+):(\d+)".r

  private def handleException[T](f: => T): Either[Error, T] = {
    try Right(f) catch {
      case e: Error => Left(e)
      case NonFatal(e) =>
        Left(new Error("Internal error: " + e.toString, Nil, Some(e)))
    }
  }

  def builder(script: String) = new TransformerBuilder(script)
}

// Significantly based on {@link sjsonnet.Interpreter Interpreter.class}
class Transformer(private var script: String,
                  inputNames: java.util.Set[String] = Collections.emptySet(),
                  libs: java.util.Set[Library] = Collections.emptySet(),
                  formats: DataFormatService = DataFormatService.DEFAULT,
                  wd: Path = ResourcePath.root,
                  parseCache: ParseCache = new DefaultParseCache,
                  importer: Importer = ResourcePath.importer,
                  private var settings: TransformerSettings = null) {

  def this(script: String,
           inputNames: java.util.Set[String],
           libs: java.util.Set[Library],
           formats: DataFormatService) = {
    this(script, inputNames, libs, formats, ResourcePath.root, new DefaultParseCache)
  }

  def this(script: String,
           inputNames: java.util.Set[String],
           libs: java.util.Set[Library]) = {
    this(script, inputNames, libs, DataFormatService.DEFAULT)
  }

  def this(script: String,
           inputNames: java.util.Set[String]) = {
    this(script, inputNames, Collections.emptySet())
  }

  def this(script: String) = {
    this(script, Collections.emptySet())
  }

  val header: Header = Header.parseHeader(script)
  script = Transformer.asFunction(script, inputNames.asScala)
  settings = if (settings != null) settings else new TransformerSettings(preserveOrder = header.isPreserveOrder)
  private val allLibs: IndexedSeq[Library] = IndexedSeq(Xtr).appendedAll(libs.asScala)

  private val resolver: CachedResolver = new CachedResolver(importer, parseCache) {
    override def process(expr: Expr, fs: FileScope): Either[Error, (Expr, FileScope)] =
      handleException((optimizer.optimize(expr), fs))
  }

  private val evaluator = new Evaluator(resolver, Map.empty, wd, settings, null)
  // rely on StaticOptimizer to provide the libraries directly without VisitValidId, by populating its scope
  // see StaticOptimizer#transform{case e @ Id(pos, name)}
  private val optimizer = new StaticOptimizer(evaluator)
  optimizer.scope = new Scope(immutable.HashMap.from(allLibs
    .map(lib => (lib.namespace(), ScopedVal(composeLib(lib)._2, emptyScope, -1)))
    .toMap), 0)

  private val scriptFn: Val.Func = evaluate(script, ResourcePath(main)) match {
    case Right(value) => value match {
      case func: Val.Func => func
      case _ => throw new IllegalArgumentException("Not a valid script. Transformation scripts must be a Top Level Function.") // shouldn't happen since we're wrapping in Top Level Func
    }
    case Left(error) => error match {
      case pErr: ParseError => throw new IllegalArgumentException("Could not parse transformation script...", processError(pErr))
      case err: Error if err.getMessage.contains("Internal Error") => throw new IllegalArgumentException("Unexpected internal error while evaluating the transformation script", processError(err))
      case err: Error => throw new IllegalArgumentException("Could not evaluate transformation script... ", err)
    }
  }

  def evaluate(txt: String, path: Path): Either[Error, Val] = {
    resolver.cache(path) = txt
    for {
      res <- resolver.parse(path, txt)(evaluator)
      (parsed, _) = res
      res0 <- handleException(evaluator.visitExpr(parsed)(ValScope.empty))
    } yield res0
  }

  // If the requested type is ANY then look in the header, default to JSON
  private def effectiveOutput(output: MediaType): MediaType = {
    if (!output.equalsTypeAndSubtype(MediaTypes.ANY)) {
      return output
    }

    val fromHeader = header.getOutput
    if (fromHeader.isPresent && !fromHeader.get.equalsTypeAndSubtype(MediaTypes.ANY)) {
      return fromHeader.get()
    }

    settings.defOutputMediaType
  }

  // If the input type is UNKNOWN then look in the header, default to JSON
  private def effectiveInput[T](name: String, input: Document[T]): Document[T] = {
    if (!input.getMediaType.equalsTypeAndSubtype(MediaTypes.UNKNOWN)) {
      return input
    }

    val fromHeader = header.getInput(name)
    if (fromHeader.isPresent) {
      return input.withMediaType(fromHeader.get())
    }

    input.withMediaType(settings.defInputMediaType)
  }

  // supports a Map[String, Document] to enable a scenario where documents are grouped into a single input
  private def resolveInput(name: String, input: Document[_]): ujson.Value = {
    if (!input.getContent.isInstanceOf[java.util.Map[_, _]]) return formats.mandatoryRead(effectiveInput(name, input))

    val entrySet = input.getContent.asInstanceOf[java.util.Map[_, _]].entrySet()
    if (entrySet.isEmpty) return formats.mandatoryRead(effectiveInput(name, input))

    val it = entrySet.iterator
    val firstEntry = it.next
    if (!firstEntry.getKey.isInstanceOf[String] || !firstEntry.getValue.isInstanceOf[Document[_]])
      return formats.mandatoryRead(effectiveInput(name, input))

    val builder = mutable.LinkedHashMap.newBuilder[String, ujson.Value]
    val key = firstEntry.getKey.asInstanceOf[String]
    builder.addOne((key, formats.mandatoryRead(effectiveInput(name + "." + key, firstEntry.getValue.asInstanceOf[Document[_]]))))
    while (it.hasNext) {
      val entry = it.next
      val key1 = entry.getKey.asInstanceOf[String]
      builder.addOne((key1, formats.mandatoryRead(effectiveInput(name + "." + key1, entry.getValue.asInstanceOf[Document[_]]))))
    }
    ujson.Obj(builder.result)
  }

  private def composeLib(lib: Library): (String, Val.Obj) =
    (lib.namespace(), Val.Obj.mk(dummyPosition,
      lib.functions(formats, header, importer).asScala.toSeq.map {
        case (key, value) => (key, memberOf(value))
      }
        ++ lib.modules(formats, header, importer).asScala.toSeq.map {
        case (key, value) => (key, memberOf(value))
      }
        ++ lib.libsonnets().asScala.toSeq.map {
        key => (key, memberOf(evalLibsonnet(key)))
      }: _*)
    )

  private def evalLibsonnet(name: String): Val = {
    val fName = s"$name.libsonnet"
    val path = ResourcePath(fName)
    importer.read(path) match {
      case None => throw new IllegalArgumentException("libsonnet not found: " + fName)
      case Some(txt) => evaluate(txt, path) match {
        case Left(err) => throw new IllegalArgumentException("Error parsing libsonnet", processError(err))
        case Right(value) => value
      }
    }
  }

  private def processError(err: Error): Error = {
    val trace = err.getStackTrace
    val msg2 = if (!trace(0).getFileName.contains(main)) err.getMessage
    else {
      ERROR_LINE_REGEX.replaceAllIn(err.getMessage, _ match {
        case ERROR_LINE_REGEX(fline, fcolumn) =>
          ":" + (Integer.parseInt(fline) - 1) + ":" + fcolumn
      })
    }

    val err2 = new Error(msg2, underlying = Option(err.getCause))
    val trace2 = trace.map(el => {
      if (!el.getFileName.contains(main)) el
      else {
        val lineIdx = el.getFileName.lastIndexOf(":")
        new StackTraceElement(el.getClassName,
          el.getMethodName,
          el.getFileName.substring(0, lineIdx + 1) + (Integer.parseInt(el.getFileName.substring(lineIdx + 1)) - 1),
          el.getLineNumber)
      }
    })

    err2.setStackTrace(trace2)
    err2
  }

  def transform(payload: String): String = {
    transform(new BasicDocument[String](payload)).getContent
  }

  def transform(payload: Document[_]): Document[String] = {
    transform(payload, Collections.emptyMap(), MediaTypes.ANY, classOf[String])
  }

  def transform(payload: Document[_],
                inputs: java.util.Map[String, Document[_]],
                output: MediaType): Document[String] = {
    transform(payload, inputs, output, classOf[String])
  }

  def transform[T](payload: Document[_],
                   inputs: java.util.Map[String, Document[_]],
                   output: MediaType,
                   target: Class[T]): Document[T] = {
    val payloadExpr = Materializer.toExpr(formats.mandatoryRead(effectiveInput("payload", payload)))(evaluator)
    val inputExprs = inputs.asScala.map { case (name, input) => Materializer.toExpr(resolveInput(name, input))(evaluator) }.toArray

    val fnDefaultArgs = scriptFn.params.defaultExprs.clone()

    fnDefaultArgs(0) = payloadExpr

    var i = 0
    while (i < inputExprs.length) {
      fnDefaultArgs(i + 1) = inputExprs(i)
      i += 1
    }

    val scriptFn2 = new Val.Func(scriptFn.pos, scriptFn.defSiteValScope, Params(scriptFn.params.names, fnDefaultArgs)) {
      override def evalRhs(vs: ValScope, es: EvalScope, fs: FileScope, pos: Position): Val = scriptFn.evalRhs(vs, es, fs, pos)

      override def evalDefault(expr: Expr, vs: ValScope, es: EvalScope): Val = scriptFn.evalDefault(expr, vs, es)
    }

    val materialized = handleException(Materializer.apply(scriptFn2)(evaluator)) match {
      case Right(value) => value
      case Left(err) => err match {
        case pErr: ParseError => throw new IllegalArgumentException("Could not parse transformation script...", processError(pErr))
        case err: Error =>
          throw new IllegalArgumentException("Error evaluating xtrasonnet transformation...", processError(err))
      }
    }

    val effectiveOut = effectiveOutput(output)
    formats.mandatoryWrite(materialized, effectiveOut, target)
  }
}
