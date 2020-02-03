package com.bla;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;

import javax.tools.JavaFileObject;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.CharBuffer;

final class Utils {
    private Utils() {
    }

    static Name getPrimitiveType(JCTree.JCPrimitiveTypeTree type) {
        switch (type.typetag) {
            case BYTE:
                return BlaPlugin.symtab.byteType.tsym.name;
            case CHAR:
                return BlaPlugin.symtab.charType.tsym.name;
            case SHORT:
                return BlaPlugin.symtab.shortType.tsym.name;
            case INT:
                return BlaPlugin.symtab.intType.tsym.name;
            case LONG:
                return BlaPlugin.symtab.longType.tsym.name;
            case FLOAT:
                return BlaPlugin.symtab.floatType.tsym.name;
            case DOUBLE:
                return BlaPlugin.symtab.doubleType.tsym.name;
            case BOOLEAN:
                return BlaPlugin.symtab.booleanType.tsym.name;
            default:
                return null;
        }
    }

    static Name getName(JCTree.JCExpression type) {
        if (type instanceof JCTree.JCIdent)
            return ((JCTree.JCIdent) type).name;
        else {
            return getPrimitiveType((JCTree.JCPrimitiveTypeTree) type);
        }
    }

    static boolean updateClientFileObject(CharBuffer dst, final CharBuffer src, JavaFileObject sourceFile) {
        Field fileManagerField = null;
        try {
            fileManagerField = sourceFile.getClass().getSuperclass().getDeclaredField("fileManager");
            fileManagerField.setAccessible(true);
            JavacFileManager fileManagerObject = (JavacFileManager) fileManagerField.get(sourceFile);
            fileManagerObject.cache(sourceFile, src);

            /*
             The source is alraedy read at com.sun.tools.javac.main.JavaCompiler.parse(javax.tools.JavaFileObject)
             So we need to change it here
             */
            return updateString(dst, src);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            if (fileManagerField != null)
                fileManagerField.setAccessible(false);
        }
    }

    private static boolean updateString(CharBuffer dst, final CharBuffer src) throws NoSuchFieldException, IllegalAccessException {
        Field hbField = null, offsetField = null, readOnlyField = null, markField = null, positionField = null, limitField = null, capacityField = null, addessField = null;
        try {
            hbField = CharBuffer.class.getDeclaredField("hb");
            hbField.setAccessible(true);
            hbField.set(dst, hbField.get(src));

            offsetField = CharBuffer.class.getDeclaredField("offset");
            offsetField.setAccessible(true);
            offsetField.set(dst, offsetField.get(src));

            readOnlyField = CharBuffer.class.getDeclaredField("isReadOnly");
            readOnlyField.setAccessible(true);
            readOnlyField.set(dst, readOnlyField.get(src));

            {
                markField = Buffer.class.getDeclaredField("mark");
                markField.setAccessible(true);
                markField.set(dst, markField.get(src));

                positionField = Buffer.class.getDeclaredField("position");
                positionField.setAccessible(true);
                positionField.set(dst, positionField.get(src));

                limitField = Buffer.class.getDeclaredField("limit");
                limitField.setAccessible(true);
                limitField.set(dst, limitField.get(src));

                capacityField = Buffer.class.getDeclaredField("capacity");
                capacityField.setAccessible(true);
                capacityField.set(dst, capacityField.get(src));

                addessField = Buffer.class.getDeclaredField("address");
                addessField.setAccessible(true);
                addessField.set(dst, addessField.get(src));
            }
            return true;
        } finally {
            if (hbField != null)
                hbField.setAccessible(false);
            if (offsetField != null)
                offsetField.setAccessible(false);
            if (readOnlyField != null)
                readOnlyField.setAccessible(false);
            {
                if (markField != null)
                    markField.setAccessible(false);
                if (positionField != null)
                    positionField.setAccessible(false);
                if (limitField != null)
                    limitField.setAccessible(false);
                if (capacityField != null)
                    capacityField.setAccessible(false);
                if (addessField != null)
                    addessField.setAccessible(false);
            }
        }
    }
}
