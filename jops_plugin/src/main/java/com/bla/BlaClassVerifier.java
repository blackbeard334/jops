package com.bla;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BlaClassVerifier {
    private final Symbol.ClassSymbol classSymbol;

    public BlaClassVerifier(final Symbol.ClassSymbol classSymbol) {
        this.classSymbol = classSymbol;
    }

    public BlaVerifier.BlaOverloadedClass getOverloadedClass() {
        final List<Symbol.MethodSymbol> allMethods = this.classSymbol.getEnclosedElements().stream()
                .filter(Symbol.MethodSymbol.class::isInstance)
                .map(Symbol.MethodSymbol.class::cast)
                .collect(Collectors.toList());
        final BlaVerifier.BlaOverloadedClass overloadedClass = new BlaVerifier.BlaOverloadedClass(classSymbol.getSimpleName());
        overloadedClass.plus = getMethod("oPlus", allMethods);
        overloadedClass.minus = getMethod("oMinus", allMethods);
        overloadedClass.mul = getMethod("oMultiply", allMethods);
        overloadedClass.div = getMethod("oDivide", allMethods);
        return overloadedClass;
    }

    private Map<Name, BlaVerifier.BlaOverloadedClass.BlaOverloadedMethod> getMethod(final String methodName, final List<Symbol.MethodSymbol> allMethods) {

        return allMethods.stream()
                .filter(n -> methodName.equals(n.getSimpleName().toString()))
                .filter(i -> i.getParameters().size() == 1)
                .collect(Collectors.toMap(i -> i.params.get(0).type.tsym.name, i -> new BlaVerifier.BlaOverloadedClass.BlaOverloadedMethod(i.name, i.params.get(0).type.tsym.name, i.getReturnType().tsym.name)));
    }
}
