package com.bla;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;


/**
 * convenience class because {@link com.sun.tools.javac.tree.JCTree.JCMethodInvocation} doesn't have a public constructor
 */
final class OJCMethodInvocation extends JCTree.JCMethodInvocation {

    protected OJCMethodInvocation(List<JCExpression> typeargs, JCExpression meth, List<JCExpression> args) {
        super(typeargs, meth, args);
        this.type = meth.type.getReturnType();
    }
}
