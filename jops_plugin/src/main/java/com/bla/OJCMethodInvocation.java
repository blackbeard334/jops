package com.bla;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

class OJCMethodInvocation extends JCTree.JCMethodInvocation {
    @Deprecated
    final Name returnType;

    protected OJCMethodInvocation(List<JCExpression> typeargs, JCExpression meth, List<JCExpression> args) {
        super(typeargs, meth, args);
        this.type = meth.type.getReturnType();
        returnType = this.type.tsym.name;
    }
}
