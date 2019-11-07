package com.bla;

import com.bla.annotation.OperatorOverloading;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.util.Name;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** @version 0.5.1 */
public class BlaVerifier {
    private final CompilationUnitTree compilationUnit;

    public BlaVerifier(final CompilationUnitTree compilationUnit) {
        this.compilationUnit = compilationUnit;
    }

    public boolean isValid() {//TODO only verify files that have been changed
        return hasImport();
//        if (hasImport()) {
//            for (Tree type : compilationUnit.getTypeDecls()) {
//                if (type instanceof JCClassDecl) {
//                    if (isValid((JCClassDecl) type)) return true;
//                }
//            }
//        }
//
//        return false;
    }

    private BlaOverloadedClass getOverloadedClass(final JCClassDecl firstClass) {//TODO add a similar method for parsing class files
        final List<JCMethodDecl> allMethods = firstClass.getMembers().stream()
                .filter(JCMethodDecl.class::isInstance)
                .map(JCMethodDecl.class::cast)
                .collect(Collectors.toUnmodifiableList());
        final BlaOverloadedClass overloadedClass = new BlaOverloadedClass(firstClass.getSimpleName());
        overloadedClass.plus = getMethod(OPS.PLUS.override, allMethods);
        overloadedClass.minus = getMethod(OPS.MINUS.override, allMethods);
        overloadedClass.mul = getMethod(OPS.MUL.override, allMethods);
        overloadedClass.div = getMethod(OPS.DIV.override, allMethods);

        overloadedClass.plus_asg = getMethod(OPS.PLUS_ASG.override, allMethods);
        overloadedClass.minus_asg = getMethod(OPS.MINUS_ASG.override, allMethods);
        overloadedClass.mul_asg = getMethod(OPS.MUL_ASG.override, allMethods);
        overloadedClass.div_asg = getMethod(OPS.DIV_ASG.override, allMethods);
        return overloadedClass;//TODO populate list of nested qualifiying ONLY classes
    }

    public List<BlaOverloadedClass> getOverloadedClasses() {
        final List<JCClassDecl> allClasses = getAllNestedClasses(this.compilationUnit.getTypeDecls());

        return allClasses.stream()
                .filter(this::hasAnnotation)
                .map(this::getOverloadedClass)
                .collect(Collectors.toList());
    }

    private List<JCClassDecl> getAllNestedClasses(final List<? extends Tree> members) {
        List<JCClassDecl> classList = members.stream()
                .filter(JCClassDecl.class::isInstance)
                .map(JCClassDecl.class::cast)
                .collect(Collectors.toList());

        List<JCClassDecl> nestedClasses = new ArrayList<>();
        for (JCClassDecl classDecl : classList) {
            nestedClasses.addAll(getAllNestedClasses(classDecl.getMembers()));//recursion biatches!
        }

        classList.addAll(nestedClasses);
        return classList;
    }

    private Map<Name, BlaOverloadedClass.BlaOverloadedMethod> getMethod(final String methodName, final List<JCMethodDecl> allMethods) {
        return allMethods.stream()
                .filter(n -> methodName.equals(n.getName().toString()))
                .filter(i -> i.getParameters().size() == 1)
                .collect(Collectors.toMap(BlaVerifier::getParamName, i -> new BlaOverloadedClass.BlaOverloadedMethod(i.name, i, i.sym)));
    }

    private static Name getParamName(JCMethodDecl decl) {
        return getName(decl.params.get(0).vartype);
    }

    private static Name getName(JCTree.JCExpression type) {
        if (type instanceof JCTree.JCIdent)
            return ((JCTree.JCIdent) type).name;
        else {
            return getPrimitiveType((JCTree.JCPrimitiveTypeTree) type);
        }
    }

    public static Name getPrimitiveType(JCTree.JCPrimitiveTypeTree type) {
        switch (type.typetag) {
            case BYTE:
                return BlaPlugin.symtab.byteType.tsym.name;
            case CHAR:
                return BlaPlugin.symtab.charType.tsym.name;
            case SHORT:
                return BlaPlugin.symtab.shortType.tsym.name;
            case LONG:
                return BlaPlugin.symtab.longType.tsym.name;
            case FLOAT:
                return BlaPlugin.symtab.floatType.tsym.name;
            case INT:
                return BlaPlugin.symtab.intType.tsym.name;
            case DOUBLE:
                return BlaPlugin.symtab.doubleType.tsym.name;
            case BOOLEAN:
                return BlaPlugin.symtab.booleanType.tsym.name;
            default:
                return null;
        }
    }

    private boolean isValid(JCClassDecl type) {
        final JCClassDecl type1 = type;
        for (JCAnnotation a : type1.getModifiers().getAnnotations()) {
            if (OperatorOverloading.NAME.equals(a.getAnnotationType().toString())) {
                //TODO check the methods
                for (JCTree member : type1.getMembers()) {
                    if (member instanceof JCMethodDecl) {
                        final JCMethodDecl member1 = (JCMethodDecl) member;
                        if (OPS.getOperators().contains(member1.getName().toString())) {
                            if ("void".equals(member1.getReturnType().toString())) {
                                //bad method
                            }
                            if (member1.getParameters().isEmpty()) {
                                //bad little doggie
                            }
                        }
                    }
                    if (member instanceof JCClassDecl) {
                        if (!isValid((JCClassDecl) member)) return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean hasImport() {
        for (ImportTree i : compilationUnit.getImports()) {
            if (OperatorOverloading.class.getName().equals(i.getQualifiedIdentifier().toString())) return true;
        }

        return false;
    }

    private boolean hasAnnotation(final JCClassDecl classDecl) {
        return classDecl.getModifiers().getAnnotations().stream()
                .map(JCAnnotation::getAnnotationType)
                .map(JCTree::toString)
                .anyMatch(OperatorOverloading.NAME::equals);
    }

    private boolean isValidOverloadedMethod() {

        return false;
    }

    static class BlaOverloadedClass {
        final Name name;

        Map<Name, BlaOverloadedMethod> plus;
        Map<Name, BlaOverloadedMethod> minus;
        Map<Name, BlaOverloadedMethod> mul;
        Map<Name, BlaOverloadedMethod> div;

        Map<Name, BlaOverloadedMethod> plus_asg;
        Map<Name, BlaOverloadedMethod> minus_asg;
        Map<Name, BlaOverloadedMethod> mul_asg;
        Map<Name, BlaOverloadedMethod> div_asg;//TODO generalize this

        public BlaOverloadedClass(final Name name) {
            this.name = name;
        }

        public Name getName() {
            return name;
        }

        BlaOverloadedMethod getMethod(final JCTree.Tag opcode, final Name paramType) {
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

        BlaOverloadedMethod getMethodPolyEdition(final JCTree.Tag opcode, final Type type) {
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

        static class BlaOverloadedMethod {
            final Name                methodName;
            final JCMethodDecl        meth;
            final Symbol.MethodSymbol sym;

            BlaOverloadedMethod(Name methodName, JCMethodDecl meth, Symbol.MethodSymbol sym) {
                this.methodName = methodName;
                this.meth = meth;
                this.sym = sym;
            }

            public Symbol.MethodSymbol getSym() {
                /*
                classes don't have a meth field, but they have a sym
                so we set both
                */
                return sym == null ? meth.sym : sym;
            }
        }
    }
}
