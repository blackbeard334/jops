package com.bla;

import com.bla.annotation.OperatorOverloading;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class ClassVerifier {
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
}
