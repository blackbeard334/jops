package com.bla;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;

/**
 * convenience class because {@link com.sun.tools.javac.tree.JCTree.JCFieldAccess} doesn't have a public constructor
 */
final class OJCFieldAccess extends JCTree.JCFieldAccess {
    protected OJCFieldAccess(JCExpression selected, Name name, Symbol sym) {
        super(selected, name, sym);
        this.type = sym.type;
    }
}
