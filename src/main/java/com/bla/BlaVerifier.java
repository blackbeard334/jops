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

    public List<BlaOverloadedClass> getOverloadedClasses() {
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

    private JCMethodDecl getMethod(final String methodName, final List<JCMethodDecl> allMethods) {
        return allMethods.stream()
                .filter(m -> methodName.equals(m.name.toString()))
                .findFirst()
                .orElse(null);
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

    class BlaOverloadedClass {
        final Name name;
        JCMethodDecl plus, minus, mul, div;//TODO generalize this

        public BlaOverloadedClass(final Name name) {
            this.name = name;
        }

        public Name getName() {
            return name;
        }

        JCMethodDecl getMethod(final JCTree.Tag opcode) {
            switch (opcode) {
                case PLUS:
                    return plus;
                case MINUS:
                    return minus;
                case MUL:
                    return mul;
                case DIV:
                    return div;
                default:
                    return null;
            }
        }
    }
}
