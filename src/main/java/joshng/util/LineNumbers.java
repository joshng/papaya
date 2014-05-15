package joshng.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Looks up line numbers for classes and their members.
 *
 * @author Chris Nokleberg
 */

public class LineNumbers {

  private final Class type;
  private final Map<String, Integer> lines = Maps.newHashMap();
  private String source;
  private int firstLine = Integer.MAX_VALUE;
  /*if[AOP]*/
  static final LoadingCache<Class<?>, LineNumbers> lineNumbersCache = CacheBuilder.newBuilder().weakKeys().softValues()
          .build(new CacheLoader<Class<?>, LineNumbers>() {
            public LineNumbers load(Class<?> key) {
              try {
                return new LineNumbers(key);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          });
  /*end[AOP]*/

  static final LoadingCache<Member, StackTraceElement> stackTraceCache = CacheBuilder.newBuilder().weakKeys().softValues().build(new CacheLoader<Member, StackTraceElement>() {
    public StackTraceElement load(Member member) {
      Class<?> declaringClass = member.getDeclaringClass();
      LineNumbers lineNumbers = lineNumbersCache.getUnchecked(declaringClass);
      String fileName = lineNumbers.getSource();
      Integer lineNumberOrNull = lineNumbers.getLineNumber(member);
      int lineNumber = lineNumberOrNull == null ? lineNumbers.getFirstLine() : lineNumberOrNull - 1;

      String memberName = member instanceof Constructor ? "<init>" : member.getName();
      return new StackTraceElement(declaringClass.getName(), memberName, fileName, lineNumber);
    }
  });

  public static LineNumbers get(Member member) {
    return lineNumbersCache.getUnchecked(member.getDeclaringClass());
  }

  public static StackTraceElement getStackTraceElement(Member member) {
    return stackTraceCache.getUnchecked(member);
  }

  /**
   * Reads line number information from the given class, if available.
   *
   * @param type the class to read line number information from
   * @throws IllegalArgumentException if the bytecode for the class cannot be found
   * @throws java.io.IOException      if an error occurs while reading bytecode
   */

  private LineNumbers(Class type) throws IOException {
    this.type = type;

    if (!type.isArray()) {
      InputStream in = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class");
      checkArgument(in != null, "Cannot find bytecode for %s", type);
      new ClassReader(in).accept(new LineNumberReader(), ClassReader.SKIP_FRAMES);
    }
  }

  /**
   * Get the source file name as read from the bytecode.
   *
   * @return the source file name if available, or null
   */

  public String getSource() {
    return source;
  }

  /**
   * Get the line number associated with the given member.
   *
   * @param member a field, constructor, or method belonging to the class used during construction
   * @return the wrapped line number, or null if not available
   * @throws IllegalArgumentException if the member does not belong to the class used during
   *                                  construction
   */

  public Integer getLineNumber(Member member) {
    checkArgument(type == member.getDeclaringClass(),
            "Member %s belongs to %s, not %s", member, member.getDeclaringClass(), type);
    return lines.get(memberKey(member));
  }

  /**
   * Gets the first line number.
   */

  public int getFirstLine() {
    return firstLine == Integer.MAX_VALUE ? 1 : firstLine;
  }


  private class LineNumberReader implements ClassVisitor, MethodVisitor, AnnotationVisitor {

    private int line = -1;
    private String pendingMethod;
    private String name;


    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
      this.name = name;
    }


    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
      if ((access & Opcodes.ACC_PRIVATE) != 0) {
        return null;
      }
      pendingMethod = name + desc;
      line = -1;
      return this;
    }


    public void visitSource(String source, String debug) {
      LineNumbers.this.source = source;
    }


    public void visitLineNumber(int line, Label start) {
      if (line < firstLine) {
        firstLine = line;
      }

      this.line = line;
      if (pendingMethod != null) {
        lines.put(pendingMethod, line);
        pendingMethod = null;
      }
    }


    public void visitFieldInsn(int opcode, String owner, String name,
                               String desc) {
      if (opcode == Opcodes.PUTFIELD && this.name.equals(owner)
              && !lines.containsKey(name) && line != -1) {
        lines.put(name, line);
      }
    }


    public void visitEnd() {
    }


    public void visitInnerClass(String name, String outerName, String innerName,
                                int access) {
    }


    public void visitOuterClass(String owner, String name, String desc) {
    }


    public void visitAttribute(Attribute attr) {
    }


    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value) {
      return null;
    }


    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return this;
    }


    public AnnotationVisitor visitAnnotation(String name, String desc) {
      return this;
    }


    public AnnotationVisitor visitAnnotationDefault() {
      return this;
    }


    public AnnotationVisitor visitParameterAnnotation(int parameter,
                                                      String desc, boolean visible) {
      return this;
    }


    public AnnotationVisitor visitArray(String name) {
      return this;
    }


    public void visitEnum(String name, String desc, String value) {
    }


    public void visit(String name, Object value) {
    }


    public void visitCode() {
    }


    public void visitFrame(int type, int nLocal, Object[] local, int nStack,
                           Object[] stack) {
    }


    public void visitIincInsn(int var, int increment) {
    }


    public void visitInsn(int opcode) {
    }


    public void visitIntInsn(int opcode, int operand) {
    }


    public void visitJumpInsn(int opcode, Label label) {
    }


    public void visitLabel(Label label) {
    }


    public void visitLdcInsn(Object cst) {
    }


    public void visitLocalVariable(String name, String desc, String signature,
                                   Label start, Label end, int index) {
    }


    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    }


    public void visitMaxs(int maxStack, int maxLocals) {
    }


    public void visitMethodInsn(int opcode, String owner, String name,
                                String desc) {
    }


    public void visitMultiANewArrayInsn(String desc, int dims) {
    }


    public void visitTableSwitchInsn(int min, int max, Label dflt,
                                     Label[] labels) {
    }


    public void visitTryCatchBlock(Label start, Label end, Label handler,
                                   String type) {
    }


    public void visitTypeInsn(int opcode, String desc) {
    }


    public void visitVarInsn(int opcode, int var) {
    }
  }

  public static String memberKey(Member member) {
    checkNotNull(member, "member");

      /*if[AOP]*/
    if (member instanceof Field) {
      return member.getName();

    } else if (member instanceof Method) {
      return member.getName() + Type.getMethodDescriptor((Method) member);

    } else if (member instanceof Constructor) {
      StringBuilder sb = new StringBuilder().append("<init>(");
      for (Class param : ((Constructor) member).getParameterTypes()) {
        sb.append(Type.getDescriptor(param));
      }
      return sb.append(")V").toString();

    } else {
      throw new IllegalArgumentException(
              "Unsupported implementation class for Member, " + member.getClass());
    }
      /*end[AOP]*/
      /*if[NO_AOP]
      return "<NO_MEMBER_KEY>";
      end[NO_AOP]*/
  }
}
