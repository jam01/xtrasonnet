package com.github.jam01.xtrasonnet

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import Mapper.{ERROR_LINE_REGEX, handleException, main}
import com.github.jam01.xtrasonnet.document.{DefaultDocument, Document, MediaType, MediaTypes}
import com.github.jam01.xtrasonnet.header.Header
import com.github.jam01.xtrasonnet.spi.{DataFormatService, Library}
import Library.{dummyPosition, memberOf}
import sjsonnet.Expr.Params
import sjsonnet.ScopedExprTransform.{Scope, ScopedVal, emptyScope}
import sjsonnet.{CachedResolver, DefaultParseCache, Error, EvalScope, Evaluator, Expr, FileScope, Importer, Materializer, ParseError, Path, Position, Settings, StaticOptimizer, Val, ValScope}

import java.util.Collections
import scala.collection.{Seq, immutable, mutable}
import scala.io.Source
import scala.jdk.CollectionConverters.{IterableHasAsScala, MapHasAsScala}
import scala.util.Using
import scala.util.control.NonFatal

object Mapper {
  private val main = "(main)"

  // We wrap the script as function based on advise from Jsonnet
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
}

// Significantly based on {@link sjsonnet.Interpreter Interpreter.class}
class Mapper(var script: String,
             inputNames: java.lang.Iterable[String] = Collections.emptySet(),
             imports: java.util.Map[String, String] = Collections.emptyMap(),
             asFunction: Boolean = true,
             additionalLibs: java.util.List[Library] = Collections.emptyList(),
             dataFormats: DataFormatService = DataFormatService.DEFAULT,
             defaultOutput: MediaType = MediaTypes.APPLICATION_JSON) {

  def this(script: String,
           inputNames: java.lang.Iterable[String],
           imports: java.util.Map[String, String],
           asFunction: Boolean,
           additionalLibs: java.util.List[Library]) = {
    this(script, inputNames, imports, asFunction, additionalLibs, DataFormatService.DEFAULT)
  }

  def this(script: String,
           inputNames: java.lang.Iterable[String],
           imports: java.util.Map[String, String],
           asFunction: Boolean) = {
    this(script, inputNames, imports, asFunction, Collections.emptyList())
  }

  def this(script: String,
           inputNames: java.lang.Iterable[String],
           imports: java.util.Map[String, String]) = {
    this(script, inputNames, imports, true, Collections.emptyList())
  }

  def this(script: String,
           inputNames: java.lang.Iterable[String]) = {
    this(script, inputNames, Collections.emptyMap())
  }

  def this(script: String) = {
    this(script, Collections.emptySet())
  }

  val header: Header = Header.parseHeader(script)
  if (asFunction) script = Mapper.asFunction(script, inputNames.asScala)
  private val parseCache = new DefaultParseCache
  private val settings = new Settings(preserveOrder = header.isPreserveOrder)
  private val allLibs: IndexedSeq[Library] = IndexedSeq(XTR).appendedAll(additionalLibs.asScala)

  // an importer that will resolve scripts from the classpath
  private val classpathImporter: Importer = new Importer {
    override def resolve(docBase: Path, importName: String): Option[Path] = docBase match {
      case ClasspathPath("") => Some(ClasspathPath(importName))
      case ClasspathPath(_) => Some(docBase / importName)
      case _ => None
    }

    override def read(path: Path): Option[String] = {
      val p = "/" + path.asInstanceOf[ClasspathPath].path
      getClass.getResource(p) match {
        case null => Option(imports.get(path.asInstanceOf[ClasspathPath].path))
        case _ => Using.resource(getClass.getResourceAsStream(p)) { stream =>
          Some(Source.fromInputStream(stream).mkString)
        }
      }
    }
  }

  private val resolver: CachedResolver = new CachedResolver(classpathImporter, parseCache) {
    override def process(expr: Expr, fs: FileScope): Either[Error, (Expr, FileScope)] =
      handleException((optimizer.optimize(expr), fs))
  }
  private val evaluator = new Evaluator(resolver, Map.empty, ClasspathPath(""), settings, null)
  // reserving indices for DS and additional libraries, the reservations must be exactly the same in ValScope
  private val optimizer = new StaticOptimizer(evaluator)
  optimizer.scope = new Scope(immutable.HashMap.from(allLibs
    .map(lib => (lib.namespace(), ScopedVal(Library.emptyObj, emptyScope, -1)))
    .toMap), additionalLibs.size + 1)

  // reserving indices for DS and additional libraries
  private val libsScope = ValScope.empty.extendBy(additionalLibs.size + 1)
  private val libMappingsMap: Map[String, ScopedVal] = allLibs
    .map(composeLib)
    .map { case (k, obj) => (k, ScopedVal(obj, emptyScope, -1)) }
    .toMap

  // rely on StaticOptimizer to provide the libraries directly without VisitValidId, by populating its scope
  // see StaticOptimizer#transform{case e @ Id(pos, name)}
  optimizer.scope = new Scope(immutable.HashMap.from(optimizer.scope.mappings).concat(libMappingsMap), optimizer.scope.size)

  private val scriptFn: Val.Func = evaluate(script, ClasspathPath(main)) match {
    case Right(value) => value match {
      case func: Val.Func => if (func.params.names.length < 1)
        throw new IllegalArgumentException("Top Level Function must have at least one argument.")
      else func
      case _ => throw new IllegalArgumentException("Not a valid script. Transformation scripts must have a Top Level Function.")
    }
    case Left(error) => error match {
      case pErr: ParseError => throw new IllegalArgumentException("Could not parse transformation script...", processError(pErr))
      case err: Error if err.getMessage.contains("Internal Error") => throw new IllegalArgumentException("Unexpected internal error while evaluating the transformation script, " +
        "consider opening an issue with the xtrasonnet project.", processError(err))
      case err: Error => throw new IllegalArgumentException("Could not evaluate transformation script... ", err)
    }
  }

  def evaluate(txt: String, path: Path): Either[Error, Val] = {
    resolver.cache(path) = txt
    for {
      res <- resolver.parse(path, txt)(evaluator)
      (parsed, _) = res
      res0 <- handleException(evaluator.visitExpr(parsed)(libsScope))
    } yield res0
  }

  // If the requested type is ANY then look in the header, default to JSON
  private def effectiveOutput(output: MediaType): MediaType = {
    if (output.equalsTypeAndSubtype(MediaTypes.ANY)) {
      val fromHeader = header.getDefaultOutput
      if (fromHeader.isPresent && !fromHeader.get.equalsTypeAndSubtype(MediaTypes.ANY)) header.combineOutputParams(fromHeader.get)
      else header.combineOutputParams(defaultOutput)
    } else {
      header.combineOutputParams(output)
    }
  }

  // If the input type is UNKNOWN then look in the header, default to JAVA
  private def effectiveInput[T](name: String, input: Document[T]): Document[T] = {
    if (input.getMediaType.equalsTypeAndSubtype(MediaTypes.UNKNOWN)) {
      val fromHeader = header.getDefaultNamedInput(name)
      if (fromHeader.isPresent) header.combineInputParams(name, input.withMediaType(fromHeader.get))
      else header.combineInputParams(name, input.withMediaType(MediaTypes.APPLICATION_JAVA))
    } else {
      header.combineInputParams(name, input)
    }
  }

  // supports a Map[String, Document] to enable a scenario where documents are grouped into a single input
  private def resolveInput(name: String, input: Document[_]): ujson.Value = {
    if (!input.getContent.isInstanceOf[java.util.Map[_, _]]) return dataFormats.mandatoryRead(effectiveInput(name, input))

    val entrySet = input.getContent.asInstanceOf[java.util.Map[_, _]].entrySet()
    if (entrySet.isEmpty) return dataFormats.mandatoryRead(effectiveInput(name, input))

    val it = entrySet.iterator
    val firstEntry = it.next
    if (!firstEntry.getKey.isInstanceOf[String] || !firstEntry.getValue.isInstanceOf[Document[_]])
      return dataFormats.mandatoryRead(effectiveInput(name, input))

    val builder = mutable.LinkedHashMap.newBuilder[String, ujson.Value]
    val key = firstEntry.getKey.asInstanceOf[String]
    builder.addOne((key, dataFormats.mandatoryRead(effectiveInput(name + "." + key, firstEntry.getValue.asInstanceOf[Document[_]]))))
    while (it.hasNext) {
      val entry = it.next
      val key1 = entry.getKey.asInstanceOf[String]
      builder.addOne((key1, dataFormats.mandatoryRead(effectiveInput(name + "." + key1, entry.getValue.asInstanceOf[Document[_]]))))
    }
    ujson.Obj(builder.result)
  }

  private def composeLib(lib: Library): (String, Val.Obj) =
    (lib.namespace(), Val.Obj.mk(dummyPosition,
      lib.functions(dataFormats, header, classpathImporter).asScala.toSeq.map {
        case (key, value) => (key, memberOf(value))
      }
        ++ lib.modules(dataFormats, header, classpathImporter).asScala.toSeq.map {
        case (key, value) => (key, memberOf(value))
      }
        ++ lib.libsonnets().asScala.toSeq.map {
        key => (key, memberOf(evalLibsonnet(key)))
      }: _*)
    )

  private def evalLibsonnet(name: String): Val = {
    val fName = s"$name.libsonnet"
    val path = ClasspathPath(fName)
    classpathImporter.read(path) match {
      case None => throw new IllegalArgumentException("libsonnet not found: " + fName)
      case Some(txt) => evaluate(txt, path) match {
        case Left(err) => throw new IllegalArgumentException("Error parsing libsonnet", processError(err))
        case Right(value) => value
      }
    }
  }

  private def processError(err: Error): Error = {
    if (!asFunction) return err

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
          el.getFileName.substring(0, lineIdx + 1)
            + (Integer.parseInt(el.getFileName.substring(lineIdx + 1)) - 1),
          el.getLineNumber)
      }
    })

    err2.setStackTrace(trace2)
    err2
  }

  def transform(payload: String): String = {
    transform(new DefaultDocument[String](payload)).getContent
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
    val payloadExpr = Materializer.toExpr(dataFormats.mandatoryRead(effectiveInput("payload", payload)))(evaluator)
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
    dataFormats.mandatoryWrite(materialized, effectiveOut, target)
  }
}
