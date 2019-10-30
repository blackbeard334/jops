package com.bla;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

public class OJCParens extends JCTree.JCParens {
    protected OJCParens(JCExpression expr, Type type) {
        super(expr);
        this.type = type;
    }
}
