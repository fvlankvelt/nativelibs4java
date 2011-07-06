package scalacl

package impl

import scala.collection._
//import com.nativelibs4java.opencl._


//import CLFunction._
case class CapturedIOs(
  inputBuffers: Array[CLDataIO[Any]] = Array(),
  outputBuffers: Array[CLDataIO[Any]] = Array(),
  scalars: Array[CLDataIO[Any]] = Array()
) { 
  lazy val isEmpty = inputBuffers.isEmpty && outputBuffers.isEmpty && scalars.isEmpty 
}

case class SourceData(
  functionName: String,
  functionSource: String,
  kernelsSource: String,
  includedSources: Array[String]/*,
  outerDeclarations: Seq[String]*/
)

object CLFunctionCode {
  private var nextuid = 1
  protected def newuid = this.synchronized {
    val uid = nextuid
    nextuid += 1
    uid
  }
  def clType[T](implicit dataIO: CLDataIO[T]) = dataIO.clType

  val compositions = new mutable.HashMap[(Long, Long), CLFunctionCode[_, _]]
  val ands = new mutable.HashMap[(Long, Long), CLFunctionCode[_, _]]
  
  protected val indexVar: String = "$i"
  protected val sizeVar: String = "$size"
  
  def buildSourceData[A, B](
    outerDeclarations: Array[String],
    declarations: Array[String],
    expressions: Array[String],
    includedSources: Array[String],
    extraArgsIOs: CapturedIOs = CapturedIOs()
  )(implicit aIO: CLDataIO[A], bIO: CLDataIO[B]) = 
  {
    assert(!expressions.isEmpty)
    
    val uid = newuid
    val functionName = "f" + uid
    
    def toRxb(s: String) = {
      val rx = "(^|\\b)" +
        java.util.regex.Matcher.quoteReplacement(s) + "($|\\b)"
      rx
    }
    val ta = clType[A]
    val tb = clType[B]
  
    val inParams: Seq[String] = aIO.openCLKernelArgDeclarations(CLDataIO.InputPointer, 0)
    val inValueParams: Seq[String] = aIO.openCLKernelArgDeclarations(CLDataIO.Value, 0)
    val extraParams: Seq[String] =
      extraArgsIOs.inputBuffers.flatMap(_.openCLKernelArgDeclarations(CLDataIO.InputPointer, 0)) ++
      extraArgsIOs.outputBuffers.flatMap(_.openCLKernelArgDeclarations(CLDataIO.OutputPointer, 0)) ++
      extraArgsIOs.scalars.flatMap(_.openCLKernelArgDeclarations(CLDataIO.Value, 0))
    val outParams: Seq[String] = bIO.openCLKernelArgDeclarations(CLDataIO.OutputPointer, 0)

    case class DataIOInfo(io: CLDataIO[_], placeholder: String, argType: CLDataIO.ArgType/*isBuffer: Boolean*/, isExtraArg: Boolean)

    val iosAndPlaceholders: Seq[DataIOInfo] =
      (
        (
          extraArgsIOs.inputBuffers.map((_, CLDataIO.InputPointer/*true*/)) ++ 
          extraArgsIOs.outputBuffers.map((_, CLDataIO.OutputPointer/*true*/)) ++ 
          extraArgsIOs.scalars.map((_, CLDataIO.Value/*false*/)): Seq[(CLDataIO[Any], CLDataIO.ArgType)]
        ).toList.zipWithIndex.map {
          case ((io, argType/*isBuffer*/), i) =>
            DataIOInfo(io, "_" + (i + 1), isExtraArg = false, argType = argType/*isBuffer*/)
        }
      ).toSeq ++
      Seq(
        DataIOInfo(aIO, "_", isExtraArg = false, argType/*isBuffer*/ = CLDataIO.InputPointer)
      )
      
    val varExprs = 
      (
        iosAndPlaceholders.flatMap(info => {
          info.io.openCLIntermediateKernelTupleElementsExprs(info.placeholder).map {
            case (expr, tupleIndexes) =>
              (expr, tupleIndexes, info)
          }
        })
      ).sortBy(-_._1.length).toArray // sort by decreasing expression length (enough to avoid conflicts) TODO unhack this !
       
    //println("varExprs = " + varExprs.toSeq)
    
    val indexVarName = "__cl_i"
    
    def replaceForFunction(/*argType: CLDataIO.ArgType, */s: String,  i: String, isRange: Boolean) = if (s == null) null else {
      var r = s.replaceAll(toRxb(indexVar), indexVarName)
      r = r.replaceAll(toRxb(sizeVar), "size")
  
      var iExpr = 0
      val nExprs = varExprs.length
      while (iExpr < nExprs) {
        val (expr, tupleIndexes, DataIOInfo(io, placeholder, argType, isExtraArg)) = varExprs(iExpr)
        def rawith(i: String) = io.openCLIthTupleElementNthItemExpr(argType, 0, tupleIndexes, i) 
        val x = if (isRange && argType != CLDataIO.Value)//isBuffer)
          "(" + rawith("0") + " + (" + i + ") * " + rawith("2") + ")" // buffer contains (from, to, by, inclusive). Value is (from + i * by)  
        else
          rawith(i)
        
        //println("REPLACING '" + expr + "' by '" + x + "'")
        r = r.replaceAll(toRxb(expr), x)
        //println("\t-> '" + r.replaceAll("\n", "\n\t") + "'")
        iExpr += 1
      }
      //r = r.replaceAll(toRxb(inVar), "(*in)")
      r
    }
  
    val indexHeader = """
        int """ + indexVarName + """ = get_global_id(0);
    """
    val sizeHeader =
        indexHeader + """
        if (""" + indexVarName + """ >= size)
            return;
    """
  
    def assignts(argType: CLDataIO.ArgType, i: String, isRange: Boolean) =
      expressions.zip(bIO.openCLKernelNthItemExprs(CLDataIO.OutputPointer, 0, i)).map {
        case (expression, (xOut, indexes)) => xOut + " = " + replaceForFunction(/*argType, */expression, i, isRange)
      }.mkString(";\n\t")
  
    val functionSource = ""
    /*
    val funDecls = declarations.map(replaceForFunction(CLDataIO.Value, _, "0", false)).mkString("\n")
    lazy val funDeclsRange = declarations.map(replaceForFunction(CLDataIO.Value, _, "0", true)).mkString("\n")
    val functionSource = """
        inline void """ + functionName + """(
            """ + (inValueParams ++ extraParams ++ outParams).mkString(", ") + """
        ) {
            """ + indexHeader + """
            """ + funDecls + """
            """ + assignts(CLDataIO.Value, "0", false) + """;
        }
    """
    */
    //println("expressions = " + expressions)
    //println("functionSource = " + functionSource)
  
    //def replaceForKernel(s: String) = if (s == null) null else replaceAllButIn(s).replaceAll(toRxb(inVar), "in[i]")
    val presenceName = "__cl_presence"
    val presenceParams = Seq("__global const " + CLFilteredArray.presenceCLType + "* " + presenceName)
    val kernDecls = declarations.map(replaceForFunction(/*CLDataIO.InputPointer,*/ _, indexVarName, false)).reduceLeftOption(_ + "\n" + _).getOrElse("")
    lazy val kernDeclsRange = declarations.map(replaceForFunction(/*CLDataIO.InputPointer, */_, indexVarName, true)).reduceLeftOption(_ + "\n" + _).getOrElse("")
    val assignt = assignts(CLDataIO.InputPointer, indexVarName, false)
    lazy val assigntRange = assignts(CLDataIO.InputPointer, indexVarName, true)
    var kernelsSource = outerDeclarations.mkString("\n")

    kernelsSource += """
      __kernel void array_array(
          int size,
          """ + (inParams ++ outParams ++ extraParams).mkString(", ") + """
      ) {
          """ + sizeHeader + """
          """ + kernDecls + """
          """ + assignt + """;
      }
      __kernel void filteredArray_filteredArray(
          int size,
          """ + (inParams ++ presenceParams ++ outParams ++ extraParams).mkString(", ") + """
      ) {
          """ + sizeHeader + """
          if (!""" + presenceName + "[" + indexVarName + """])
              return;
          """ + kernDecls + """
          """ + assignt + """;
      }
    """
    
    if (aIO.t.erasure == classOf[Int]) {// || aIO.t.erasure == classOf[Integer])) {
      kernelsSource += """
        __kernel void range_array(
            int size,
            """ + (inParams ++ outParams ++ extraParams).mkString(", ") + """
        ) {
            """ + sizeHeader + """
            """ + kernDeclsRange + """
            """ + assigntRange + """;
        }
      """
    }
    if (verbose)
      println("[ScalaCL] Creating kernel with source <<<\n\t" + kernelsSource.replaceAll("\n", "\n\t") + "\n>>>")
      
    SourceData(
      functionName = functionName,
      functionSource = functionSource,
      kernelsSource = kernelsSource,
      includedSources = includedSources/*,
      outerDeclarations = outerDeclarations*/
    )
  }
}

import CLFunctionCode._

class CLFunctionCode[A, B](
  val sourceData: SourceData,
  val extraArgsIOs: CapturedIOs = CapturedIOs()
)(
  implicit 
  val aIO: CLDataIO[A], 
  val bIO: CLDataIO[B]
) 
extends CLCode 
{  
  import sourceData._
  
  val uid = newuid
  
  val sourcesToInclude = 
    //if (sourceData == null) null else 
    includedSources ++ Array(functionSource)
    
  override val sources = 
    //if (sourceData == null) null else 
    sourcesToInclude ++ Array(kernelsSource)
    
  override val macros = Map[String, String]()
  override val compilerArguments = Array[String]()

  //override def isOnlyInScalaSpace = sourceData == null
  
  protected def throwIfCapture(f: CLFunctionCode[_, _]) = {
    if (!extraArgsIOs.isEmpty || !extraArgsIOs.isEmpty)
      throw new UnsupportedOperationException("Cannot compose functions that capture external variables !")
  }
  
  def compose[C](f: CLFunctionCode[C, A])(implicit cIO: CLDataIO[C]) = {
    throwIfCapture(f)
    
    compositions.synchronized {
      compositions.getOrElseUpdate((uid, f.uid), {
        // TODO FIXME !
        new CLFunctionCode[C, B](
          sourceData = buildSourceData[C, B](
            outerDeclarations = Array(),// TODO ??? outerDeclarations ++ f.outerDeclarations, 
            declarations = Array(),
            expressions = Array(functionName + "(" + f.sourceData.functionName + "(_))"),
            includedSources = sourcesToInclude ++ f.sourcesToInclude
          )(f.aIO, bIO)
        ).asInstanceOf[CLFunctionCode[_, _]]
      }).asInstanceOf[CLFunctionCode[C, B]]
    }
  }
  
  def and(f: CLFunctionCode[A, B]) = {
    ands.synchronized {
      ands.getOrElseUpdate((uid, f.uid), {
        // TODO FIXME !
        new CLFunctionCode[A, B](
          sourceData = buildSourceData[A, B](
            outerDeclarations = Array(),  
            declarations = Array(), 
            expressions = Array("(" + functionName + "(_) && " + f.sourceData.functionName + "(_))"), 
            includedSources = sourcesToInclude ++ f.sourcesToInclude
          )
        ).asInstanceOf[CLFunctionCode[_, _]]
      }).asInstanceOf[CLFunctionCode[A, B]]
    }
  }
}