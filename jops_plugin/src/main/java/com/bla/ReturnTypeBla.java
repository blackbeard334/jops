package com.bla;

import com.sun.tools.javac.util.Name;

/** @deprecated this should become a marker interface, and we set the returnTypes within the JCExpressions themselves */
@Deprecated
public interface ReturnTypeBla {

    @Deprecated
    Name getReturnType();
}
