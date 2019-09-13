package com.bla;

import com.bla.annotation.OperatorOverloading;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.util.Name;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BlaVerifier {
    private final CompilationUnitTree compilationUnit;

    public BlaVerifier(final CompilationUnitTree compilationUnit) {
        this.compilationUnit = compilationUnit;
    }

    public boolean isValid() {//TODO only verify files that have been changed
        if (hasImport()) {
            for (Tree type : compilationUnit.getTypeDecls()) {
                if (type instanceof JCClassDecl) {
                    if (isValid((JCClassDecl) type)) return true;
                }
            }
        }

        return false;
    }

    public List<BlaOverloadedClass> getOverloadedClasses() {//TODO add a similar method for parsing class files
        final JCClassDecl firstClass = (JCClassDecl) this.compilationUnit.getTypeDecls().get(0);

        final List<JCMethodDecl> allMethods = firstClass.getMembers().stream()
                .filter(JCMethodDecl.class::isInstance)
                .map(JCMethodDecl.class::cast)
                .collect(Collectors.toUnmodifiableList());
        final BlaOverloadedClass overloadedClass = new BlaOverloadedClass(firstClass.getSimpleName());
        overloadedClass.plus = getMethod("oPlus", allMethods);
        overloadedClass.minus = getMethod("oMinus", allMethods);
        overloadedClass.mul = getMethod("oMultiply", allMethods);
        overloadedClass.div = getMethod("oDivide", allMethods);
        return List.of(overloadedClass);//TODO populate list of nested qualifiying ONLY classes
    }

    private Map<Name, BlaOverloadedClass.BlaOverloadedMethod> getMethod(final String methodName, final List<JCMethodDecl> allMethods) {
        return allMethods.stream()
                .filter(n -> methodName.equals(n.getName().toString()))
                .filter(i -> i.getParameters().size() == 1)
                .filter(i -> i.getParameters().get(0).vartype instanceof JCTree.JCIdent)//FIXME temp hack to avoid primitive params
                .collect(Collectors.toMap(BlaVerifier::getParamName, i -> new BlaOverloadedClass.BlaOverloadedMethod(i.name, getParamName(i), ((JCTree.JCIdent) i.restype).name)));
    }

    private static Name getParamName(JCMethodDecl decl) {
        final JCTree.JCExpression firstParam = decl.params.get(0).vartype;
        if (firstParam instanceof JCTree.JCIdent)
            return ((JCTree.JCIdent) firstParam).name;
        else
            return ((JCTree.JCPrimitiveTypeTree) firstParam).type.tsym.name;
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

    public boolean hasImport() {
        for (ImportTree i : compilationUnit.getImports()) {
            if (OperatorOverloading.class.getName().equals(i.getQualifiedIdentifier().toString())) return true;
        }

        return false;
    }

    private boolean isValidOverloadedMethod() {

        return false;
    }

    static class BlaOverloadedClass {
        final Name name;
        Map<Name, BlaOverloadedMethod> plus;
        Map<Name, BlaOverloadedMethod> minus;
        Map<Name, BlaOverloadedMethod> mul;
        Map<Name, BlaOverloadedMethod> div;//TODO generalize this


        public BlaOverloadedClass(final Name name) {
            this.name = name;
        }

        public Name getName() {
            return name;
        }

        BlaOverloadedMethod getMethod(final JCTree.Tag opcode, Name paramType) {
            switch (opcode) {
                case PLUS:
                    return plus.get(paramType);
                case MINUS:
                    return minus.get(paramType);
                case MUL:
                    return mul.get(paramType);
                case DIV:
                    return div.get(paramType);
                default:
                    return null;
            }
        }

        static class BlaOverloadedMethod {
            final Name methodName;
            final Name paramType;
            final Name returnType;

            BlaOverloadedMethod(Name methodName, Name paramType, Name returnType) {
                this.methodName = methodName;
                this.paramType = paramType;
                this.returnType = returnType;
            }
        }
    }
}
