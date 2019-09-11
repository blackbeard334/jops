package com.bla;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

class OJCMethodInvocation extends JCTree.JCMethodInvocation {
    protected OJCMethodInvocation(List<JCExpression> typeargs, JCExpression meth, List<JCExpression> args) {
        super(typeargs, meth, args);
    }
}
