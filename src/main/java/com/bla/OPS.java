package com.bla;

import com.sun.tools.javac.tree.JCTree;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum OPS {
    PLUS("operator+(", "oPlus(", JCTree.Tag.PLUS),
    MINUS("operator-(", "oMinus(", JCTree.Tag.MINUS),
    MULTIPLY("operator*(", "oMultiply(", JCTree.Tag.MUL),
    DIVIDE("operator/(", "oDivide(", JCTree.Tag.DIV);

    private final String     operator;
    private final String     replacement;
    private final JCTree.Tag tag;

    private static final Set<String>     OPERATORS    = Arrays.stream(OPS.values()).map(i -> i.operator.substring(0, i.operator.length() - 1)).collect(Collectors.toUnmodifiableSet());//TODO make this purty
    private static final Set<String>     REPLACEMENTS = Arrays.stream(OPS.values()).map(i -> i.replacement.substring(0, i.replacement.length() - 1)).collect(Collectors.toUnmodifiableSet());//shane falco!
    private static final Set<JCTree.Tag> TAGS         = Arrays.stream(OPS.values()).map(i -> i.tag).collect(Collectors.toUnmodifiableSet());

    OPS(final String operator, final String replacement, JCTree.Tag tag) {
        this.operator = operator;
        this.replacement = replacement;
        this.tag = tag;
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

    public static Set<JCTree.Tag> getTAGS() {
        return TAGS;
    }
}