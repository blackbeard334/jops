package com.bla;

import com.sun.tools.javac.tree.JCTree;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum OPS {
    PLUS("operator+", "oPlus", JCTree.Tag.PLUS),//TODO maybe these operator names should be randomized?
    MINUS("operator-", "oMinus", JCTree.Tag.MINUS),
    MUL("operator*", "oMultiply", JCTree.Tag.MUL),
    DIV("operator/", "oDivide", JCTree.Tag.DIV),

    PLUS_ASG("operator+=", "oPluSet", JCTree.Tag.PLUS_ASG),
    MINUS_ASG("operator-=", "oMinSet", JCTree.Tag.MINUS_ASG),
    MUL_ASG("operator*=", "oMulSet", JCTree.Tag.MUL_ASG),
    DIV_ASG("operator/=", "oDivSet", JCTree.Tag.DIV_ASG);

    public final String     operator;
    public final String     override;
    public final JCTree.Tag tag;

    private static final Set<String>     OPERATORS = Arrays.stream(OPS.values()).map(i -> i.operator).collect(Collectors.toUnmodifiableSet());//TODO make this purty
    private static final Set<String>     OVERRIDES = Arrays.stream(OPS.values()).map(i -> i.override).collect(Collectors.toUnmodifiableSet());
    private static final Set<JCTree.Tag> TAGS      = Arrays.stream(OPS.values()).map(i -> i.tag).collect(Collectors.toUnmodifiableSet());

    OPS(final String operator, final String override, JCTree.Tag tag) {
        this.operator = operator;
        this.override = override;
        this.tag = tag;
    }

    /** Operator TO Replacement */
    public String otor(final String str) {
        return str.replace(operator + "(", override + "("); //TODO fix the corner case where there's a fucking variable named (*)operator+(
    }

    /** Replacement TO Operator */
    public String roto(final String str) {
        return str.replace(override + "(", operator + "(");
    }

    public static Set<String> getOperators() {
        return OPERATORS;
    }

    public static Set<String> getOverrides() {
        return OVERRIDES;
    }

    public static Set<JCTree.Tag> getTAGS() {
        return TAGS;
    }
}