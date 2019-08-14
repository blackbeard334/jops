package com.bla;

public enum OPS {
    PLUS("operator+(", "oPlus("),
    MINUS("operator-(", "oMinus("),
    MULTIPLY("operator*(", "oMultiply("),
    DIVIDE("operator/(", "oDivide(");

    private final String operator;
    private final String replacement;

    OPS(final String operator, final String replacement) {
        this.operator = operator;
        this.replacement = replacement;
    }

    /** Operator TO Replacement */
    public String otor(final String str) {
        return str.replace(operator, replacement);
    }

    /** Replacement TO Operator */
    public String roto(final String str) {
        return str.replace(replacement, operator);
    }
}