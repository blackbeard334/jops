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
//                    if (classType.supertype_field == JOPSPlugin.symtab.objectType) {
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
                    return getMethodPolyEdition(opcode, JOPSPlugin.symtab.intType);
                case BYTE:
                    return getMethodPolyEdition(opcode, JOPSPlugin.symtab.shortType);
                case SHORT:
                    return getMethodPolyEdition(opcode, JOPSPlugin.symtab.intType);
                case INT:
                    return getMethodPolyEdition(opcode, JOPSPlugin.symtab.longType);
                case LONG:
                    return getMethodPolyEdition(opcode, JOPSPlugin.symtab.floatType);
                case FLOAT:
                    return getMethodPolyEdition(opcode, JOPSPlugin.symtab.doubleType);
                case DOUBLE:
                default:
                    return null;
            }
        }

        return null;
    }
}
