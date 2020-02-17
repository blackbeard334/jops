package com.bla;

import com.bla.annotation.OperatorOverloading;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TaskEvent;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class ClassVerifier {

    /** just a list of all the classes we already checked. is extra important when we scan .class files */
    private static Set<Symbol> checkedClasses = new HashSet<>();

    private ClassVerifier() {
    }

    static boolean hasOperatorOverloadingAnnotation(Symbol clazz) {
        if (clazz.getMetadata() != null) {
            final com.sun.tools.javac.util.List<Attribute.Compound> annotations = clazz.getMetadata().getDeclarationAttributes();
            return annotations != null && annotations.stream()
                    .map(Attribute.Compound::getAnnotationType)
                    .map(Object::toString)
                    .anyMatch(OperatorOverloading.class.getName()::equals);
        }
        return false;
    }

    static OverloadedClass getOverloadedClass(final Symbol.ClassSymbol classSymbol) {
        final List<Symbol.MethodSymbol> allMethods = classSymbol.getEnclosedElements().stream()
                .filter(Symbol.MethodSymbol.class::isInstance)
                .map(Symbol.MethodSymbol.class::cast)
                .collect(Collectors.toList());
        final OverloadedClass overloadedClass = new OverloadedClass(classSymbol.getSimpleName());

        overloadedClass.plus = getMethod(OPS.PLUS.override, allMethods);
        overloadedClass.minus = getMethod(OPS.MINUS.override, allMethods);
        overloadedClass.mul = getMethod(OPS.MUL.override, allMethods);
        overloadedClass.div = getMethod(OPS.DIV.override, allMethods);

        overloadedClass.plus_asg = getMethod(OPS.PLUS_ASG.override, allMethods);
        overloadedClass.minus_asg = getMethod(OPS.MINUS_ASG.override, allMethods);
        overloadedClass.mul_asg = getMethod(OPS.MUL_ASG.override, allMethods);
        overloadedClass.div_asg = getMethod(OPS.DIV_ASG.override, allMethods);
        return overloadedClass;
    }

    private static Map<Name, MethodInformation> getMethod(final String methodName, final List<Symbol.MethodSymbol> allMethods) {

        return allMethods.stream()
                .filter(hasName(methodName))
                .filter(i -> i.getParameters().size() == 1)
                .collect(Collectors.toMap(i -> i.params.get(0).type.tsym.name, i -> new MethodInformation(i.name, null, i)));
    }

    private static Predicate<Symbol.MethodSymbol> hasName(String methodName) {
        return n -> methodName.equals(n.getSimpleName().toString());
    }

    private static boolean isOverloadedType(Name name) {//TODO rename
        return JOPSPlugin.overloadedClasses.containsKey(name);
    }

    static boolean hasOverloadedClassesImported(final CompilationUnitTree compilationUnit) {
        /*TODO how do we check files in the same package that don't require imports
            since we can't always rely on imports for stuff in same package:
            ((Scope.ScopeImpl) ((JCTree.JCPackageDecl) e.getCompilationUnit().getPackage()).packge.members()).table
         */
        return hasOverloadedImport(compilationUnit) || hasOverloadedPackageMate(compilationUnit);
    }

    private static boolean hasOverloadedImport(CompilationUnitTree compilationUnit) {
        return compilationUnit.getImports().stream()
                .map(ImportTree::getQualifiedIdentifier)
                .map(JCTree.JCFieldAccess.class::cast)
                .map(JCTree.JCFieldAccess::getIdentifier)
                .anyMatch(ClassVerifier::isOverloadedType);
    }

    private static boolean hasOverloadedPackageMate(CompilationUnitTree compilationUnit) {
        for (Symbol symbol : ((JCTree.JCPackageDecl) compilationUnit.getPackage()).packge.members().getSymbols()) {
            if (ClassVerifier.isOverloadedType(symbol.getSimpleName()))
                return true;
        }
        return false;
    }

    /** this method checks all class files for possible classes with our overloading annotation */
    static void loadClassFiles(TaskEvent e) {
        for (Symbol packge : ((JCTree.JCCompilationUnit) e.getCompilationUnit()).modle.getEnclosedElements()) {
            for (Symbol clazz : ((Symbol.PackageSymbol) packge).members_field.getSymbols()) {
                if (checkedClasses.contains(clazz) || isOverloadedType(clazz.getSimpleName())) continue;

                loadNestedClasses(clazz);
            }
        }
    }

    private static void loadNestedClasses(final Symbol classSymbol) {
        for (Symbol.ClassSymbol clazz : getNestedClasses((Symbol.ClassSymbol) classSymbol)) {
            if (ClassVerifier.hasOperatorOverloadingAnnotation(clazz)) {
                //parse and add to some list
                JOPSPlugin.overloadedClasses.put(clazz.getSimpleName(), ClassVerifier.getOverloadedClass(clazz));
            }
            checkedClasses.add(clazz);
        }
    }

    private static List<Symbol.ClassSymbol> getNestedClasses(final Symbol.ClassSymbol clazz) {
        List<Symbol.ClassSymbol> classes = new ArrayList<>();
        classes.add(clazz);

        classes.addAll(clazz.getEnclosedElements().stream()
                .filter(Symbol.ClassSymbol.class::isInstance)
                .map(Symbol.ClassSymbol.class::cast)
                .map(ClassVerifier::getNestedClasses)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));

        return classes;
    }
}
