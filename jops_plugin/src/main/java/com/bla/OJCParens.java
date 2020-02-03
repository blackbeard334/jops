package com.bla;

import com.sun.tools.javac.tree.JCTree;

/**
 * convenience class because {@link com.sun.tools.javac.tree.JCTree.JCParens} doesn't have a public constructor
 */
final class OJCParens extends JCTree.JCParens {

    protected OJCParens(JCExpression expr) {
        super(expr);
        this.type = expr.type;
    }
}
