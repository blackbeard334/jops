package com.bla;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum OPS {
    PLUS("operator+(", "oPlus("),
    MINUS("operator-(", "oMinus("),
    MULTIPLY("operator*(", "oMultiply("),
    DIVIDE("operator/(", "oDivide(");

    private final String operator;
    private final String replacement;

    private static final Set<String> OPERATORS    = Arrays.stream(OPS.values()).map(i -> i.operator.substring(0, i.operator.length() - 1)).collect(Collectors.toUnmodifiableSet());//TODO make this purty
    private static final Set<String> REPLACEMENTS = Arrays.stream(OPS.values()).map(i -> i.replacement.substring(0, i.replacement.length() - 1)).collect(Collectors.toUnmodifiableSet());//shane falco!

    OPS(final String operator, final String replacement) {
        this.operator = operator;
        this.replacement = replacement;
    }

    /** Operator TO Replacement */
    public String otor(final String str) {
        return str.replace(operator, replacement); //TODO fix the corner case where there's a fucking variable named (*)operator+(
    }

    /** Replacement TO Operator */
    public String roto(final String str) {
        return str.replace(replacement, operator);
    }

    public static Set<String> getOperators() {
        return OPERATORS;
    }

    public static Set<String> getReplacements() {
        return REPLACEMENTS;
    }
}