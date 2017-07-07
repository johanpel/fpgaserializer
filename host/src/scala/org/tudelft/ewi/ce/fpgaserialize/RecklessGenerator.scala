package org.tudelft.ewi.ce.fpgaserialize

import java.io.FileOutputStream

class RecklessStruct(val sourceKlass: Class[_], val fields: Array[LayoutField], val isArray: Boolean)

object RecklessGenerator {

  var headerQualifier: String = "inline"
  var sourceQualifier: String = "extern inline"
  var forwardQualifier: String = "inline"

  var sp: Int = 16

  def apply(klass: Class[_], libName: String): Unit = {
    println("Obtaining class information.")
    val rStructs = RecklessGenerator.getClassStructs(klass)

    println("Generating forward function declarations for C header.")
    val sourceDecl = RecklessGenerator.generateSource(rStructs, source = true, forward = true)

    println("Generating C type definitions for C header.")
    val types = RecklessGenerator.generateStructs(rStructs, forward = true)

    println("Generating structs for C header.")
    val structs = RecklessGenerator.generateStructs(rStructs)

    println("Generating inline function implementations for C header.")
    val source = RecklessGenerator.generateSource(rStructs)

    println("Generating extern function declarations for C source.")
    val declarations = RecklessGenerator.generateSource(rStructs, source = true)

    println("Writing to file.")
    val headerFOS = new FileOutputStream("src/c/" + libName + ".h")
    val cFOS = new FileOutputStream("src/c/" + libName + ".c")

    headerFOS.write(RecklessGenerator.headerHeader(libName).getBytes)
    headerFOS.write(types.getBytes)
    headerFOS.write(sourceDecl.getBytes)
    headerFOS.write("// STRUCTS: \n\n".getBytes)
    headerFOS.write(structs.getBytes)

    headerFOS.write("// INLINE FUNCTIONS: \n\n".getBytes)
    headerFOS.write(source.getBytes)
    headerFOS.write(RecklessGenerator.headerFooter.getBytes)

    cFOS.write(RecklessGenerator.sourceHeader(libName).getBytes)
    cFOS.write(declarations.getBytes)

    headerFOS.close()
    cFOS.close()
    println("Code generation completed.")
  }


  def headerHeader(libName: String): String = {
    "#ifndef SRC_" + libName.toUpperCase + "_H\n" +
      "#define SRC_" + libName.toUpperCase + "_H\n\n" +
      "typedef char byte;\n\n"
  }

  val headerFooter: String = "" +
    "\n#endif //SRC_RECKLESS_H\n"

  def sourceHeader(libName: String): String = "" +
    "#include <stdlib.h>\n" +
    "#include \"" + libName + ".h\"\n" +
    "\n"

  def getStructName(name: String): String = {
    val ret = new String(name.getBytes("US-ASCII"))
    ret.replace(".", "_")
    ret.replace("[]", "_Array")
  }

  def getType(name: String): String = {
    name match {
      case "double" => "double"
      case "long" => "long"
      case "float" => "float"
      case "int" => "int"
      case "short" => "short"
      case "char" => "short"
      case "byte" => "byte"
      case "boolean" => "char"
      case _ => getStructName(name)
    }
  }

  def getClassStructs(klass: Class[_]): Array[RecklessStruct] = {
    klass.getTypeParameters.foreach(t => println("Type parameter: " + t.getName))
    klass.getAnnotations.foreach(a => println("Annotation, type=" + a.annotationType().getName))
    val klasses = ReflectionHelper.getAllClasses(klass)
    val structs = klasses.map(klass => new RecklessStruct(klass, CompactLayouter.getFields(klass), klass.isArray))
    structs
  }

  def generateStructs(structs: Array[RecklessStruct], forward: Boolean = false, ptrs: Boolean = false): String = {
    val str = new StringBuffer()
    // Generate all structs in reverse so we don't get undeclared errors
    structs.reverseIterator.foreach(struct => {
      if (!struct.isArray) {
        // Normal objects
        if (!forward) {
          str.append(s"struct _${getStructName(struct.sourceKlass.getSimpleName)} {\n")
          struct.fields.foreach { f =>
            if (!f.typ.isArray) {
              if (ptrs) {
                str.append(f"  ${getType(f.typ.getSimpleName) + "*"}%-24s ${f.name}%s;\n")
              } else {
                str.append(f"  ${getType(f.typ.getSimpleName) + ""}%-24s ${f.name}%s;\n")
              }
            } else {
              if (ptrs) {
                str.append(f"  ${getType(f.typ.getComponentType.getSimpleName) + "_Array*"}%-24s ${f.name}%s;\n")
              } else {
                str.append(f"  ${getType(f.typ.getComponentType.getSimpleName) + "_Array"}%-24s ${f.name}%s;\n")
              }
            }
          }
          str.append(s"};\n") // ${getStructName(struct.sourceKlass.getSimpleName)};\n")
          str.append("\n")
        }
        else {
          str.append(s"typedef struct _${getStructName(struct.sourceKlass.getSimpleName)} ${getStructName(struct.sourceKlass.getSimpleName)};\n")
        }
      } else {
        // Arrays
        val cType = getType(struct.sourceKlass.getComponentType.getSimpleName)
        if (!forward) {
          str.append(s"struct _${cType}_Array {\n")
          if (ptrs) {
            str.append(s"  int*                     size;\n")
          } else {
            str.append(s"  int                      size;\n")
          }
          if (struct.sourceKlass.getComponentType.isPrimitive) {
            str.append(f"  ${cType + "*"}%-24s values;\n")
          } else {
            str.append(f"  ${cType + "**"}%-24s values;\n")
          }
          str.append(s"};\n") // ${cType}_Array;\n")
          str.append("\n")
        } else {
          str.append(s"typedef struct _${cType}_Array ${cType}_Array;\n")
        }
      }
    })
    str.toString
  }

  def generateSource(structs: Array[RecklessStruct], source: Boolean = false, forward: Boolean = false, ptrs: Boolean = false): String = {
    val str = new StringBuffer()
    structs.reverseIterator.foreach { struct =>
      if (!struct.sourceKlass.isArray) {
        // Normal objects
        val name = getStructName(struct.sourceKlass.getSimpleName)
        if (!source) {
          if (ptrs) {
            str.append(s"$headerQualifier $name* get_$name(void* obj) {\n")
            str.append(f"  ${name + "* ret"}%-24s = ($name*)malloc(sizeof($name));\n")
          } else {
            str.append(s"$headerQualifier $name get_$name(void* obj) {\n")
            str.append(f"  ${name + " ret"}%s;\n")
          }
          struct.fields.foreach { f =>
            if (f.typ.isPrimitive) {
              val ftype = getType(f.typ.getSimpleName)
              if (ptrs) {
                str.append(f"  ${"ret->" + f.name}%-24s = ($ftype*)((char*)obj + ${f.offset});\n")
              } else {
                str.append(f"  ${"ret." + f.name}%-24s = *($ftype*)((char*)obj + ${f.offset});\n")
              }
            } else {
              if (!f.typ.isArray) {
                val ftype = getType(f.typ.getSimpleName)
                if (ptrs) {
                  str.append(f"  ${"ret->" + f.name}%-24s = get_$ftype((void*)*(long*)((char*)obj + ${f.offset}));\n")
                } else {
                  str.append(f"  ${"ret." + f.name}%-24s = get_$ftype((void*)*(long*)((char*)obj + ${f.offset}));\n")
                }
              } else {
                val ftype = getType(f.typ.getComponentType.getSimpleName) + "_Array"
                if (ptrs) {
                  str.append(f"  ${"ret->" + f.name}%-24s = get_$ftype((void*)*(long*)((char*)obj + ${f.offset}));\n")
                } else {
                  str.append(f"  ${"ret." + f.name}%-24s = get_$ftype((void*)*(long*)((char*)obj + ${f.offset}));\n")
                }
              }
            }
          }
        }
        else {
          val q = if (forward) forwardQualifier else sourceQualifier
          if (ptrs) {
            str.append(s"$q $name* get_$name(void* obj);\n")
          } else {
            str.append(s"$q $name get_$name(void* obj);\n")
          }
        }
      } else {
        // Arrays

        val name = getType(struct.sourceKlass.getComponentType.getSimpleName)
        val cName = getStructName(name) + "_Array"
        val cType = getType(struct.sourceKlass.getComponentType.getSimpleName)

        if (!source) {
          if (ptrs) {
            str.append(s"$headerQualifier $cName* get_$cName(void* obj) {\n")
            str.append(f"  ${cName + "* ret"}%-24s = ($cName*)malloc(sizeof($cName));\n")
            str.append(f"  ${"ret->size"}%-24s = (int*)((char*)obj + 16);\n")
          } else {
            str.append(s"$headerQualifier $cName get_$cName(void* obj) {\n")
            str.append(f"  ${cName + " ret"}%s;\n")
            str.append(f"  ${"ret.size"}%-24s = *(int*)((char*)obj + 16);\n")
          }
          if (struct.sourceKlass.getComponentType.isPrimitive) {
            if (ptrs) {
              str.append(f"  ${"ret->values"}%-24s = ($cType*)((char*)obj + 24);\n")
            } else {
              str.append(f"  ${"ret.values"}%-24s = ($cType*)((char*)obj + 24);\n")
            }
          } else {
            if (ptrs) {
              str.append(f"  ${"ret->values"}%-24s = ($cType**)((char*)obj + 24);\n")
            } else {
              str.append(f"  ${"ret.values"}%-24s = ($cType**)((char*)obj + 24);\n")
            }
          }
        } else {
          val q = if (forward) forwardQualifier else sourceQualifier
          if (ptrs) {
            str.append(s"$q $cName* get_$cName(void* obj);\n")
          } else {
            str.append(s"$q $cName get_$cName(void* obj);\n")
          }
        }
      }
      if (!source) {
        str.append("  return ret;\n")
        str.append("}\n")
        str.append("\n")
      }
    }
    str.toString
  }
}
