package com.bla;

import com.sun.tools.javac.code.Type;
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

    private static Name getPrimitiveTypeName(JCTree.JCPrimitiveTypeTree type) {
        switch (type.typetag) {
            case BYTE:
                return JOPSPlugin.symtab.byteType.tsym.name;
            case CHAR:
                return JOPSPlugin.symtab.charType.tsym.name;
            case SHORT:
                return JOPSPlugin.symtab.shortType.tsym.name;
            case INT:
                return JOPSPlugin.symtab.intType.tsym.name;
            case LONG:
                return JOPSPlugin.symtab.longType.tsym.name;
            case FLOAT:
                return JOPSPlugin.symtab.floatType.tsym.name;
            case DOUBLE:
                return JOPSPlugin.symtab.doubleType.tsym.name;
            case BOOLEAN:
                return JOPSPlugin.symtab.booleanType.tsym.name;
            default:
                return null;
        }
    }

    static Name getName(JCTree.JCExpression type) {
        if (type instanceof JCTree.JCIdent)
            return ((JCTree.JCIdent) type).name;
        else {
            return getPrimitiveTypeName((JCTree.JCPrimitiveTypeTree) type);
        }
    }

    static boolean updateClientFileObject(final CharBuffer src, JavaFileObject sourceFile) {
        Field fileManagerField = null;
        try {
            fileManagerField = sourceFile.getClass().getSuperclass().getDeclaredField("fileManager");
            fileManagerField.setAccessible(true);
            JavacFileManager fileManagerObject = (JavacFileManager) fileManagerField.get(sourceFile);
            fileManagerObject.cache(sourceFile, src);

            return true;
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();//TODO error handling in case we fail
            return false;
        } finally {
            if (fileManagerField != null)
                fileManagerField.setAccessible(false);
        }
    }

    /**
     * The source is alraedy read at com.sun.tools.javac.main.JavaCompiler.parse(javax.tools.JavaFileObject)
     * So we need to overwrite it here T_T
     */
    static boolean updateString(CharBuffer dst, final CharBuffer src) {
        Field hbField = null, offsetField = null, readOnlyField = null;
        Field markField = null, positionField = null, limitField = null, capacityField = null, addessField = null;
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
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();//TODO error handling in case we fail
            return false;
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

    static boolean isLinealMatch(final Type src, final Type dst) {
        if (dst.tsym == src.tsym) return true;

        if (dst instanceof Type.ClassType) {
            Type.ClassType classType = (Type.ClassType) dst;
            return isLinealMatch(src, classType.supertype_field);
        }

        if (dst instanceof Type.ArrayType) {
            final Type.ArrayType dstType = (Type.ArrayType) dst;
            final Type.ArrayType srcType = (Type.ArrayType) src;
            if (dstType.elemtype instanceof Type.JCPrimitiveType) {//primitive arrays are absolute
                return srcType.elemtype.tsym == dstType.elemtype.tsym;
            }
            return isLinealMatch(srcType.elemtype, dstType.elemtype);
        }

        if (dst instanceof Type.JCPrimitiveType) {
            //https://docs.oracle.com/javase/specs/jls/se10/html/jls-5.html#jls-5.1.2
            switch (dst.getTag()) {
                case CHAR:
                    return isLinealMatch(src, JOPSPlugin.symtab.intType);
                case BYTE:
                    return isLinealMatch(src, JOPSPlugin.symtab.shortType);
                case SHORT:
                    return isLinealMatch(src, JOPSPlugin.symtab.intType);
                case INT:
                    return isLinealMatch(src, JOPSPlugin.symtab.longType);
                case LONG:
                    return isLinealMatch(src, JOPSPlugin.symtab.floatType);
                case FLOAT:
                    return isLinealMatch(src, JOPSPlugin.symtab.doubleType);
                case DOUBLE:
                default:
                    return false;
            }
        }

        return false;
    }
}
