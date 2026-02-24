package io.github.jam01.xtrasonnet

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.Transformer.{ERROR_LINE_REGEX, handleException, main}
import io.github.jam01.xtrasonnet.document.Document.BasicDocument
import io.github.jam01.xtrasonnet.document.{Document, MediaType, MediaTypes}
import io.github.jam01.xtrasonnet.header.Header
import io.github.jam01.xtrasonnet.spi.{Library, PluginException}
import sjsonnet.Expr.Member.Visibility
import sjsonnet.Expr.Params
import sjsonnet.Val.Obj
import sjsonnet.stdlib.StdLibModule
import sjsonnet.{DefaultParseCache, Error, EvalScope, Evaluator, Expr, FileScope, Importer, Interpreter, ParseCache, ParseError, Path, Position, Settings, StaticResolvedFile, TailstrictModeDisabled, Val, ValScope}

import java.util.Collections
import scala.jdk.CollectionConverters.{IterableHasAsScala, MapHasAsScala}
import scala.util.control.NonFatal

object Transformer {
  val main = "(main)"

  // We wrap the script as function in order to pass in payload, and named inputs
  // see the 'Top-level arguments' section in https://jsonnet.org/learning/tutorial.html#parameterize-entire-config
  private def asFunction(script: String, argumentNames: Iterable[String]): String =
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
                  private var settings: TransformerSettings = null,
                  std: Val.Obj = StdLibModule.Default.module) {

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
  settings = if (settings != null) settings else new TransformerSettings(Settings(preserveOrder = header.isPreserveOrder))

  private val allLibs: IndexedSeq[Library] = IndexedSeq(new Xtr(formats, header)).appendedAll(libs.asScala)
  private val allLibsMap: Map[String, Val.Obj] = allLibs.map(lib => (lib.name, lib.module)).toMap

  private val interpreter = Interpreter(
    Map.empty,
    Map.empty,
    ResourcePath(main),
    importer,
    parseCache,
    settings.sjsSettings,
    std = std,
    variableResolver = ext => {
      allLibsMap.get(ext)
    }
  )
  private val evaluator: Evaluator = interpreter.evaluator

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
      val resolvedImport = StaticResolvedFile(txt)
      interpreter.resolver.cache(path) = resolvedImport
      interpreter.resolver.parse(path, resolvedImport)(evaluator) flatMap { case (expr, x) =>
        handleException(evaluator.visitExpr(expr)(ValScope.empty))
      }
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
  private def resolveInput(name: String, input: Document[_]): Expr = {
    if (!input.getContent.isInstanceOf[java.util.Map[_, _]]) return formats.mandatoryRead(effectiveInput(name, input), evaluator.emptyMaterializeFileScopePos)

    val entrySet = input.getContent.asInstanceOf[java.util.Map[_, _]].entrySet()
    if (entrySet.isEmpty) return formats.mandatoryRead(effectiveInput(name, input), evaluator.emptyMaterializeFileScopePos)

    val it = entrySet.iterator
    val firstEntry = it.next
    if (!firstEntry.getKey.isInstanceOf[String] || !firstEntry.getValue.isInstanceOf[Document[_]])
      return formats.mandatoryRead(effectiveInput(name, input), evaluator.emptyMaterializeFileScopePos)

    val builder = new java.util.LinkedHashMap[String, Val.Obj.Member]()
    val key = firstEntry.getKey.asInstanceOf[String]
    builder.put(key, memberOf(formats.mandatoryRead(effectiveInput(name + "." + key, firstEntry.getValue.asInstanceOf[Document[_]]), evaluator.emptyMaterializeFileScopePos)))
    while (it.hasNext) {
      val entry = it.next
      val key1 = entry.getKey.asInstanceOf[String]
      builder.put(key1, memberOf(formats.mandatoryRead(effectiveInput(name + "." + key1, entry.getValue.asInstanceOf[Document[_]]), evaluator.emptyMaterializeFileScopePos)))
    }

    new Val.Obj(Position(null, 0), builder, false, null, null)
  }

  private def memberOf(value: Val): Obj.Member = new Obj.ConstMember(false, Visibility.Normal, value)

  private def composeLibs(lib: Library): (String, Val.Obj) = (lib.name, lib.module)

  private def processError(err: Error): Error = {
    val trace = err.getStackTrace
    val msg2 = if (trace.isEmpty || !trace(0).getFileName.contains(main)) err.getMessage
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
    val payloadExpr = formats.mandatoryRead(effectiveInput("payload", payload), evaluator.emptyMaterializeFileScopePos)
    val inputExprs = inputs.asScala.map { case (name, input) => resolveInput(name, input) }.toArray

    val fnDefaultArgs = scriptFn.params.defaultExprs.clone()

    fnDefaultArgs(0) = payloadExpr

    var i = 0
    while (i < inputExprs.length) {
      fnDefaultArgs(i + 1) = inputExprs(i)
      i += 1
    }

    val scriptFn2 = new Val.Func(scriptFn.pos, scriptFn.defSiteValScope, Params(scriptFn.params.names, fnDefaultArgs)) {
      override def evalRhs(vs: ValScope, es: EvalScope, fs: FileScope, pos: Position): Val =
        scriptFn.evalRhs(vs, es, fs, pos)

      override def evalDefault(expr: Expr, vs: ValScope, es: EvalScope): Val =
        scriptFn.evalDefault(expr, vs, es)
    }

    val effectiveOut = effectiveOutput(output)

    handleException(formats.mandatoryWrite(scriptFn2.apply0(scriptFn.pos)(evaluator, TailstrictModeDisabled), effectiveOut, target, evaluator)) match {
      case Right(value) => value
      case Left(err) => err match {
        case pErr: ParseError => throw new IllegalArgumentException("Could not parse transformation script...", processError(pErr))
        case err: Error =>
          if (err.getCause.isInstanceOf[PluginException]) throw err.getCause // materialization successful until this point, make this the root exc
          throw new IllegalArgumentException("Error evaluating xtrasonnet transformation...", processError(err))
      }
    }
  }
}
