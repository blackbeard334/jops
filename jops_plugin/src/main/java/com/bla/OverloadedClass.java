package com.bla;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;

import java.util.Map;

final class OverloadedClass {
    final Name name;

    Map<Name, MethodInformation> plus;
    Map<Name, MethodInformation> minus;
    Map<Name, MethodInformation> mul;
    Map<Name, MethodInformation> div;

    Map<Name, MethodInformation> plus_asg;
    Map<Name, MethodInformation> minus_asg;
    Map<Name, MethodInformation> mul_asg;
    Map<Name, MethodInformation> div_asg;//TODO generalize this

    public OverloadedClass(final Name name) {
        this.name = name;
    }

    public Name getName() {
        return name;
    }

    MethodInformation getMethod(final JCTree.Tag opcode, final Name paramType) {
        switch (opcode) {
            case PLUS:
                return plus.get(paramType);
            case MINUS:
                return minus.get(paramType);
            case MUL:
                return mul.get(paramType);
            case DIV:
                return div.get(paramType);
            case PLUS_ASG:
                return plus_asg.get(paramType);
            case MINUS_ASG:
                return minus_asg.get(paramType);
            case MUL_ASG:
                return mul_asg.get(paramType);
            case DIV_ASG:
                return div_asg.get(paramType);
            default:
                return null;
        }
    }

    MethodInformation getMethodPolyEdition(final JCTree.Tag opcode, final Type type) {
        if (getMethod(opcode, type.tsym.name) != null)
            return getMethod(opcode, type.tsym.name);
//            if (type instanceof Type.ClassType) {
//                Type.ClassType classType = (Type.ClassType) type;
//                Name paramType = classType.tsym.name;
//                while (getMethod(opcode, paramType) == null) {
//                    if (classType.supertype_field == BlaPlugin.symtab.objectType) {
//                        return null;
//                    }
//                    paramType = classType.tsym.name;
//                    classType = (Type.ClassType) classType.supertype_field;
//                }
//                return getMethod(opcode, paramType);
//            }
        if (type instanceof Type.JCPrimitiveType) {
            //https://docs.oracle.com/javase/specs/jls/se10/html/jls-5.html#jls-5.1.2
            switch (type.getTag()) {
                case CHAR:
                    return getMethodPolyEdition(opcode, BlaPlugin.symtab.intType);
                case BYTE:
                    return getMethodPolyEdition(opcode, BlaPlugin.symtab.shortType);
                case SHORT:
                    return getMethodPolyEdition(opcode, BlaPlugin.symtab.intType);
                case INT:
                    return getMethodPolyEdition(opcode, BlaPlugin.symtab.longType);
                case LONG:
                    return getMethodPolyEdition(opcode, BlaPlugin.symtab.floatType);
                case FLOAT:
                    return getMethodPolyEdition(opcode, BlaPlugin.symtab.doubleType);
                case DOUBLE:
                default:
                    return null;
            }
        }

        return null;
    }

    static boolean isLinealMatch(final Type src, final Type dst) {
        if (dst.tsym == src.tsym) return true;

        if (dst instanceof Type.ClassType) {
            Type.ClassType classType = (Type.ClassType) dst;
            return isLinealMatch(src, classType.supertype_field);
        }

        if (dst instanceof Type.ArrayType) {
            final Type.ArrayType dstType = (Type.ArrayType) dst;
            final Type.ArrayType srcType = (Type.ArrayType) src;
            if (dstType.elemtype instanceof Type.JCPrimitiveType) {//primitive arrays are absolute
                return srcType.elemtype.tsym == dstType.elemtype.tsym;
            }
            return isLinealMatch(srcType.elemtype, dstType.elemtype);
        }

        if (dst instanceof Type.JCPrimitiveType) {
            //https://docs.oracle.com/javase/specs/jls/se10/html/jls-5.html#jls-5.1.2
            switch (dst.getTag()) {
                case CHAR:
                    return isLinealMatch(src, BlaPlugin.symtab.intType);
                case BYTE:
                    return isLinealMatch(src, BlaPlugin.symtab.shortType);
                case SHORT:
                    return isLinealMatch(src, BlaPlugin.symtab.intType);
                case INT:
                    return isLinealMatch(src, BlaPlugin.symtab.longType);
                case LONG:
                    return isLinealMatch(src, BlaPlugin.symtab.floatType);
                case FLOAT:
                    return isLinealMatch(src, BlaPlugin.symtab.doubleType);
                case DOUBLE:
                default:
                    return false;
            }
        }

        return false;
    }
}
