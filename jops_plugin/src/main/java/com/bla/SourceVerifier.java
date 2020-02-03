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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** @version 0.5.3 */
final class SourceVerifier {

    private SourceVerifier() {
    }

    static boolean isValid(final CompilationUnitTree compilationUnit) {//TODO only verify files that have been changed
        return hasImport(compilationUnit);
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

    private static OverloadedClass getOverloadedClass(final JCClassDecl firstClass) {//TODO add a similar method for parsing class files
        final List<JCMethodDecl> allMethods = firstClass.getMembers().stream()
                .filter(JCMethodDecl.class::isInstance)
                .map(JCMethodDecl.class::cast)
                .collect(Collectors.toUnmodifiableList());
        final OverloadedClass overloadedClass = new OverloadedClass(firstClass.getSimpleName());
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

    static List<OverloadedClass> getOverloadedClasses(final CompilationUnitTree compilationUnit) {
        final List<JCClassDecl> allClasses = getAllNestedClasses(compilationUnit.getTypeDecls());

        return allClasses.stream()
                .filter(SourceVerifier::hasAnnotation)
                .map(SourceVerifier::getOverloadedClass)
                .collect(Collectors.toList());
    }

    private static List<JCClassDecl> getAllNestedClasses(final List<? extends Tree> members) {
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

    private static Map<Name, MethodInformation> getMethod(final String methodName, final List<JCMethodDecl> allMethods) {
        return allMethods.stream()
                .filter(n -> methodName.equals(n.getName().toString()))
                .filter(i -> i.getParameters().size() == 1)
                .collect(Collectors.toMap(SourceVerifier::getParamName, i -> new MethodInformation(i.name, i, i.sym)));
    }

    private static Name getParamName(JCMethodDecl decl) {
        return Utils.getName(decl.params.get(0).vartype);
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

    private static boolean hasImport(final CompilationUnitTree compilationUnit) {
        for (ImportTree i : compilationUnit.getImports()) {
            if (OperatorOverloading.class.getName().equals(i.getQualifiedIdentifier().toString())) return true;
        }

        return false;
    }

    private static boolean hasAnnotation(final JCClassDecl classDecl) {
        return classDecl.getModifiers().getAnnotations().stream()
                .map(JCAnnotation::getAnnotationType)
                .map(JCTree::toString)
                .anyMatch(OperatorOverloading.NAME::equals);
    }

}
