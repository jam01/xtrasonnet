package io.github.jam01.xtrasonnet

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.Transformer.{ERROR_LINE_REGEX, INTERNAL_ERROR_PREFIX, handleException, main}
import io.github.jam01.xtrasonnet.document.Document.BasicDocument
import io.github.jam01.xtrasonnet.document.{Document, MediaType, MediaTypes}
import io.github.jam01.xtrasonnet.header.Header
import io.github.jam01.xtrasonnet.spi.{Library, PluginException}
import sjsonnet.Expr.Member.Visibility
import sjsonnet.Expr.Params
import sjsonnet.Val.Obj
import sjsonnet.stdlib.StdLibModule
import sjsonnet.{CachedResolver, DefaultParseCache, Error, EvalScope, Evaluator, Expr, FileScope, Importer, Interpreter, ParseCache, ParseError, Parser, Path, Position, Settings, StaticResolvedFile, TailstrictModeDisabled, Val, ValScope}

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

  private[xtrasonnet] val INTERNAL_ERROR_PREFIX = "Internal error: "

  private[xtrasonnet] def handleException[T](f: => T): Either[Error, T] = {
    try Right(f) catch {
      case e: Error => Left(e)
      case NonFatal(e) =>
        Left(new Error(INTERNAL_ERROR_PREFIX + e.toString, Nil, Some(e)))
    }
  }

  def builder(script: String) = new TransformerBuilder(script)
}

/**
 * Compiles a transformation script once and evaluates it against payloads.
 *
 * '''A Transformer is not safe to share between threads.''' Compilation happens once, in the
 * constructor, but evaluation mutates state that lives inside sjsonnet: `Val.Obj` memoises field
 * values into an unsynchronized `java.util.HashMap`, and the `std` and `xtr` objects every script
 * touches are built once per Transformer and shared by every call. `Evaluator.cachedImports`, the
 * import cache and `DefaultParseCache` are plain mutable maps for the same reason.
 *
 * Use one Transformer per thread, or pool them -- `camel-xtrasonnet`'s `XtrasonnetExpression` keeps
 * a pool, following `org.apache.camel.language.xpath.XPathBuilder`. Overlapping calls are detected
 * and rejected rather than left to corrupt those caches silently.
 */
// Significantly based on {@link sjsonnet.Interpreter Interpreter.class}
class Transformer(script: String,
                  inputNames: java.util.Set[String] = Collections.emptySet(),
                  libs: java.util.Set[Library] = Collections.emptySet(),
                  formats: DataFormatService = DataFormatService.DEFAULT,
                  wd: Path = ResourcePath.root,
                  parseCache: ParseCache = new DefaultParseCache,
                  importer: Importer = ResourcePath.importer,
                  settings: TransformerSettings = null,
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

  // vals, not vars reassigned in the constructor body: non-final fields assigned there can be seen
  // as null by a thread that observes a racily-published Transformer
  private val fnScript: String = Transformer.asFunction(script, inputNames.asScala)
  private val effSettings: TransformerSettings =
    if (settings != null) settings else new TransformerSettings(Settings(preserveOrder = header.isPreserveOrder))

  private val allLibs: IndexedSeq[Library] = IndexedSeq(new Xtr(formats, header)).appendedAll(libs.asScala)
  private val allLibsMap: Map[String, Val.Obj] = allLibs.map(lib => (lib.name, lib.module)).toMap

  private val interpreter = FluentInterpreter(
    ResourcePath(main),
    importer,
    parseCache,
    effSettings.sjsSettings,
    std = std,
    variableResolver = ext => {
      allLibsMap.get(ext)
    })

  private val evaluator: Evaluator = interpreter.evaluator

  private val scriptFn: Val.Func = evaluate(fnScript, ResourcePath(main)) match {
    case Right(value) => value match {
      case func: Val.Func => func
      case _ => throw new XtrasonnetParseException("Not a valid script. Transformation scripts must be a Top Level Function.") // shouldn't happen since we're wrapping in Top Level Func
    }
    case Left(error) => error match {
      case pErr: ParseError =>
        val processed = processError(pErr)
        throw new XtrasonnetParseException("Could not parse transformation script: " + processed.getMessage, processed)
      // handleException produces "Internal error: ", so testing for "Internal Error" never matched
      // and internal failures fell through to the generic message below
      case err: Error if err.getMessage != null && err.getMessage.startsWith(INTERNAL_ERROR_PREFIX) =>
        val processed = processError(err)
        throw new XtrasonnetException("Unexpected internal error while compiling the transformation script: " + processed.getMessage, processed)
      case err: Error =>
        val processed = processError(err)
        throw new XtrasonnetEvaluationException("Could not evaluate transformation script: " + processed.getMessage, processed)
    }
  }

  // Position of each top level parameter, by name. Inputs are bound by name rather than by the
  // iteration order of the given Map, which is unspecified and, for Map.of, randomized per JVM.
  private val paramIndices: Map[String, Int] = scriptFn.params.names.zipWithIndex.toMap

  // private: it exists to compile the wrapped script once, from the constructor, and it writes into
  // the resolver's mutable cache. Exposing it handed callers a way to mutate that from any thread.
  private def evaluate(txt: String, path: Path): Either[Error, Val] = {
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

    effSettings.defOutputMediaType
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

    input.withMediaType(effSettings.defInputMediaType)
  }

  // supports a Map[String, Document] to enable a scenario where documents are grouped into a single input
  private def resolveInput(name: String, input: Document[_]): Expr = {
    if (!input.getContent.isInstanceOf[java.util.Map[_, _]]) return formats.mandatoryRead(effectiveInput(name, input), evaluator.emptyMaterializeFileScopePos)

    val entrySet = input.getContent.asInstanceOf[java.util.Map[_, _]].entrySet()
    if (entrySet.isEmpty) return formats.mandatoryRead(effectiveInput(name, input), evaluator.emptyMaterializeFileScopePos)

    // every entry must be a (String, Document) for this to be a group of nested documents, otherwise
    // the Map is read as a single document. Checking only the first entry leaves the rest to fail
    // with an opaque ClassCastException.
    val entries = entrySet.asScala.toSeq
    if (!entries.forall(entry => entry.getKey.isInstanceOf[String] && entry.getValue.isInstanceOf[Document[_]]))
      return formats.mandatoryRead(effectiveInput(name, input), evaluator.emptyMaterializeFileScopePos)

    val builder = new java.util.LinkedHashMap[String, Val.Obj.Member]()
    entries.foreach { entry =>
      val key = entry.getKey.asInstanceOf[String]
      builder.put(key, memberOf(formats.mandatoryRead(effectiveInput(name + "." + key, entry.getValue.asInstanceOf[Document[_]]), evaluator.emptyMaterializeFileScopePos)))
    }

    new Val.Obj(Position(null, 0), builder, false, null, null)
  }

  private def memberOf(value: Val): Obj.Member = new Obj.ConstMember(false, Visibility.Normal, value)

  // getFileName is nullable, and an unguarded call here meant a NullPointerException while
  // formatting an error -- losing the original failure entirely
  private def inMainScript(el: StackTraceElement): Boolean =
    el.getFileName != null && el.getFileName.contains(main)

  private def processError(err: Error): Error = {
    val trace = err.getStackTrace
    val msg2 = if (err.getMessage == null || trace.isEmpty || !inMainScript(trace(0))) err.getMessage
    else {
      ERROR_LINE_REGEX.replaceAllIn(err.getMessage, _ match {
        case ERROR_LINE_REGEX(fline, fcolumn) =>
          ":" + (Integer.parseInt(fline) - 1) + ":" + fcolumn
      })
    }

    val err2 = new Error(msg2, underlying = Option(err.getCause))
    val trace2 = trace.map(el => {
      if (!inMainScript(el)) el
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

  // The evaluation state this guards lives inside sjsonnet and cannot be made concurrent from here
  // (see the class doc). Two threads overlapping in transform corrupt unsynchronized HashMaps, which
  // shows up later as a lost memo, a null where a value exists, or a thread spinning -- never as a
  // clean failure at the point of the mistake. So say so at the point of the mistake instead.
  private val owner = new java.util.concurrent.atomic.AtomicReference[Thread]()

  private def exclusively[T](f: => T): T = {
    val self = Thread.currentThread()
    val prev = owner.compareAndExchange(null, self)
    if (prev != null && prev != self) throw new XtrasonnetException(
      "This Transformer is already in use by thread '" + prev.getName + "'. A Transformer holds " +
        "evaluation state that cannot be shared: build one per thread, or pool them -- see " +
        "camel-xtrasonnet's XtrasonnetExpression. Sharing one would corrupt the evaluator's " +
        "caches silently rather than failing here.")

    // prev == self means an outer call on this thread already owns it; that frame does the release
    if (prev != null) f else try f finally owner.set(null)
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
                   target: Class[T]): Document[T] = exclusively {
    val payloadExpr = formats.mandatoryRead(effectiveInput("payload", payload), evaluator.emptyMaterializeFileScopePos)

    val fnDefaultArgs = scriptFn.params.defaultExprs.clone()

    fnDefaultArgs(0) = payloadExpr

    inputs.asScala.foreach { case (name, input) =>
      // "payload" names the first parameter, bound above from the payload argument. It is never a
      // declared input either: the wrapper prepends the parameter, so withInputNames cannot introduce it.
      if (name == "payload") throw new XtrasonnetException(
        "'payload' is not a named input: it is the payload argument of transform. Pass it there, or " +
          "rename this input.")

      val idx = paramIndices.getOrElse(name, throw new XtrasonnetException(
        "Unknown input '" + name + "'. Declared inputs are: " +
          scriptFn.params.names.drop(1).mkString(", ") +
          ". Declare it with TransformerBuilder.withInputNames before transforming."))
      fnDefaultArgs(idx) = resolveInput(name, input)
    }

    val scriptFn2 = new Val.Func(scriptFn.pos, scriptFn.defSiteValScope, Params(scriptFn.params.names, fnDefaultArgs)) {
      override def evalRhs(vs: ValScope, es: EvalScope, fs: FileScope, pos: Position): Val =
        scriptFn.evalRhs(vs, es, fs, pos)

      override def evalDefault(expr: Expr, vs: ValScope, es: EvalScope): Val =
        scriptFn.evalDefault(expr, vs, es)
    }

    val effectiveOut = effectiveOutput(output)

    val result = unwrap(handleException(scriptFn2.apply0(scriptFn.pos)(evaluator, TailstrictModeDisabled)))

    // Checked before materializing. sjsonnet has a good message for this ("Couldn't manifest
    // function..."), but builds the error frame from the value's position, and its builtins carry a
    // null one -- so materializing a builtin throws NullPointerException from inside the error
    // reporting and the real message never appears. Referencing a builtin without calling it is an
    // easy typo, so name it here.
    result match {
      case f: Val.Func =>
        throw new XtrasonnetEvaluationException(
          "The transformation produced a function, which cannot be written as " + effectiveOut +
            ". If you meant to call it, add parentheses: xtr.datetime.now() rather than xtr.datetime.now.")
      case _ =>
    }

    unwrap(handleException(formats.mandatoryWrite(result, effectiveOut, target, evaluator)))
  }

  private def unwrap[T](result: Either[Error, T]): T = result match {
    case Right(value) => value
    case Left(err) => err match {
      case pErr: ParseError =>
        val processed = processError(pErr)
        throw new XtrasonnetParseException("Could not parse transformation script: " + processed.getMessage, processed)
      case err: Error =>
        if (err.getCause.isInstanceOf[PluginException]) throw err.getCause // materialization successful until this point, make this the root exc
        val processed = processError(err)
        throw new XtrasonnetEvaluationException("Error evaluating xtrasonnet transformation: " + processed.getMessage, processed)
    }
  }
}

final class FluentInterpreter(path: Path,
                importer: Importer,
                parseCache: ParseCache,
                settings: Settings,
                std: Val.Obj,
                variableResolver: String => Option[Expr]) extends Interpreter(
  Map.empty,
  Map.empty,
  path,
  importer,
  parseCache,
  settings,
  std = std,
  variableResolver = variableResolver) {
  override def createResolver(parseCache: ParseCache): CachedResolver = new CachedResolver(
    importer,
    parseCache,
    internedStrings,
    internedStaticFieldSets,
    settings
  ) {
    override def process(expr: Expr, fs: FileScope): Either[Error, (Expr, FileScope)] = {
      handleException(
        (
          createOptimizer(evaluator, std, internedStrings, internedStaticFieldSets).optimize(expr),
          fs
        )
      )
    }

    override protected def parser(path: Path): Parser =
      new FluentParser(path, internedStrings, internedStaticFieldSets, settings)
  }
}
