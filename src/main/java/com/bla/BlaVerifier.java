package com.bla;

import com.bla.annotation.OperatorOverloading;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;

public class BlaVerifier {
    private final JCCompilationUnit compilationUnit;

    public BlaVerifier(final JCCompilationUnit compilationUnit) {
        this.compilationUnit = compilationUnit;
    }

    public boolean isValid() {//TODO only verify files that have been changed
        if (hasImport()) {
            for (JCTree type : compilationUnit.getTypeDecls()) {
                if (type instanceof JCClassDecl) {
                    if (isValid((JCClassDecl) type)) return true;
                }
            }
        }

        return false;
    }

    private boolean isValid(JCClassDecl type) {
        final JCClassDecl type1 = type;
        for (JCAnnotation a : type1.getModifiers().getAnnotations()) {
            if (OperatorOverloading.ANNOTATION.equals(a.toString())) {
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
        for (JCImport i : compilationUnit.getImports()) {
            if (OperatorOverloading.class.getName().equals(i.toString())) return true;
        }

        return false;
    }

    private boolean isValidOverloadedMethod() {

        return false;
    }
}
