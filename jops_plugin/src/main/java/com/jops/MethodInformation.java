package com.jops;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;

final class MethodInformation {
    final Name                methodName;
    final JCTree.JCMethodDecl meth;
    final Symbol.MethodSymbol sym;

    MethodInformation(Name methodName, JCTree.JCMethodDecl meth, Symbol.MethodSymbol sym) {
        this.methodName = methodName;
        this.meth = meth;
        this.sym = sym;
    }

    Symbol.MethodSymbol getSym() {
        // classes don't have a meth field, but they have a sym so we set both
        return sym == null ? meth.sym : sym;
    }
}
