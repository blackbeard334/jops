package com.jops;

import com.sun.tools.javac.tree.JCTree;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

enum OPS {
    PLUS("operator+", "oPlus", JCTree.Tag.PLUS),//TODO maybe these operator names should be randomized?
    MINUS("operator-", "oMinus", JCTree.Tag.MINUS),
    MUL("operator*", "oMultiply", JCTree.Tag.MUL),
    DIV("operator/", "oDivide", JCTree.Tag.DIV),

    PLUS_ASG("operator+=", "oPluSet", JCTree.Tag.PLUS_ASG),
    MINUS_ASG("operator-=", "oMinSet", JCTree.Tag.MINUS_ASG),
    MUL_ASG("operator*=", "oMulSet", JCTree.Tag.MUL_ASG),
    DIV_ASG("operator/=", "oDivSet", JCTree.Tag.DIV_ASG);

    final String     operator;
    final String     override;
    final JCTree.Tag tag;

    private static final Set<String>     OPERATORS = Arrays.stream(OPS.values()).map(i -> i.operator).collect(Collectors.toUnmodifiableSet());//TODO make this purty
    private static final Set<String>     OVERRIDES = Arrays.stream(OPS.values()).map(i -> i.override).collect(Collectors.toUnmodifiableSet());
    private static final Set<JCTree.Tag> TAGS      = Arrays.stream(OPS.values()).map(i -> i.tag).collect(Collectors.toUnmodifiableSet());

    OPS(final String operator, final String override, JCTree.Tag tag) {
        this.operator = operator;
        this.override = override;
        this.tag = tag;
    }

    /** Operator TO Replacement */
    String otor(final String str) {
        return str.replace(operator + "(", override + "("); //TODO fix the corner case where there's a fucking variable named (*)operator+(
    }

    /** Replacement TO Operator */
    String roto(final String str) {
        return str.replace(override + "(", operator + "(");
    }

    static Set<String> getOperators() {
        return OPERATORS;
    }

    static Set<String> getOverrides() {
        return OVERRIDES;
    }

    static Set<JCTree.Tag> getTAGS() {
        return TAGS;
    }
}