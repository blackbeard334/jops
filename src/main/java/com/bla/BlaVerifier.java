package com.bla;

import com.bla.annotation.OperatorOverloading;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;

import java.util.List;

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

    public List<String> getOverloadedClasses(){
        return List.of("x.y.z.Bla");//TODO populate list of nested qualifiying classes
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
}
