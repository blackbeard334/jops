package com.bla;

import com.sun.tools.javac.tree.JCTree;

public class OJCParens extends JCTree.JCParens {

    protected OJCParens(JCExpression expr) {
        super(expr);
        this.type = expr.type;
    }
}
