package com.bla;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;

public class OJCParens extends JCTree.JCParens {
    final Name returnType;

    protected OJCParens(JCExpression expr, Name returnType) {
        super(expr);
        this.returnType = returnType;
    }
}
