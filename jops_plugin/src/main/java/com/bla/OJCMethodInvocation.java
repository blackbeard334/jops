package com.bla;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

class OJCMethodInvocation extends JCTree.JCMethodInvocation {

    protected OJCMethodInvocation(List<JCExpression> typeargs, JCExpression meth, List<JCExpression> args, Type type) {
        super(typeargs, meth, args);
        this.type = type;
    }
}
