package com.jops;

import com.jops.annotation.OperatorOverloading;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * check for annotation
 * tokenize by space?
 * skip comments
 * *skip after //
 * *skip from /* till *\/
 * skip strings from " till "
 * * rather than skip, we should fill the range with something
 * method check <--TODO
 * *NOT static
 * *ONE PARAM
 * *return type present
 * *find a way to rename compilation errors back to their operator+ variants instead of oPlus
 *
 * @version 3.7
 */
final class SourceParser {
    private static final String  OVERLOADING_IMPORT     = OperatorOverloading.class.getName();
    private static final String  OVERLOADING_ANNOTATION = OperatorOverloading.ANNOTATION;
    private static final boolean DEBUG                  = false;

    private final String sourceFile;
    private final String noComments;
    private final String noCommentsOrStrings;
    private final String noCommentsOrStringsOrWhiteSpace;

    private final Map<String, String> blaMap = new HashMap<>();

    SourceParser(final String sourceFile) {
        this.sourceFile = sourceFile;
        noComments = stripComments(sourceFile);
        noCommentsOrStrings = stripStringLiterals(noComments);
        noCommentsOrStringsOrWhiteSpace = noCommentsOrStrings.replaceAll("\\s", "");
    }

    /** seek & destroy */
    String parseAndReplace() {
        String temp = noCommentsOrStrings;
        if (hasOverloadingShallow()) {
//            lines = sourceFile.split("\n");
            for (OPS operator : OPS.values()) {
                temp = operator.otor(temp);//TODO check operator locations after the comments/strings have been stripped, and then replace them in the original
            }
            temp = restoreStringLiterals(temp);
            /**
             * we can just check the rest of the lines in the same way for:
             * * import
             * * annotation
             * * _class_ <--underscores = spaces
             * * * classes can be nested, so we need to make sure the annotation is on the actual class
             * * operator
             * * * replace operator on line
             * then merge lines to whole, and replace operators from merged lines to original:
             * * position 123 in lines = oPlus()
             * * position 123 in original should have operator+(), which we replace with oPlus()
             */
//            JCTree.JCCompilationUnit bla =
        }
        return temp;
    }

    boolean hasOverloadingShallow() {
        // remove break lines from source
        return noCommentsOrStringsOrWhiteSpace.contains(OVERLOADING_IMPORT) && noCommentsOrStringsOrWhiteSpace.contains(OVERLOADING_ANNOTATION);
    }

    private String stripComments(final String source) {
        String bla = source;
        int index = -1, leftComment, rightComment = -1;

        //skip single line comments
        while ((index = bla.indexOf("//", index + 1)) != -1) {
            final int len = bla.indexOf('\n', index) - index - 2;
            bla = strcat(bla, fill(len), index + 2, len);
        }
        //TODO merge both single line/blocks into a single loop
        //skip blocks
        while ((leftComment = bla.indexOf("/*", rightComment)) != -1) {
            final int len;
            if ((rightComment = bla.indexOf("*/", leftComment + 2)) != -1) {
                len = rightComment - leftComment - 2;
                bla = strcat(bla, fill(len), leftComment + 2, len);
            } else {//TODO broken comment with no closing tag
                len = bla.length() - leftComment;
                bla = strcat(bla, fill(len), leftComment, len);
                break;
            }
        }

        assert (bla.length() == source.length());
        return bla;
    }

    private String stripStringLiterals(final String source) {//TODO empty strings throw a null pointer
        StringBuilder bla = new StringBuilder(source.replace("\\\"", "\t"));//remove escaped quotes
        int leftQuotes, rightQuotes = -1;

        while ((leftQuotes = bla.indexOf("\"", rightQuotes + 1)) != -1) {
            rightQuotes = bla.indexOf("\"", leftQuotes + 1);
            if (rightQuotes != -1) {
                final String uuid = UUID.randomUUID().toString();
                final String str = bla.substring(leftQuotes + 1, rightQuotes);
                blaMap.put(uuid, str);
                bla.delete(leftQuotes + 1, rightQuotes);
                bla.insert(leftQuotes + 1, uuid);

                rightQuotes = leftQuotes + 1 + uuid.length() + 1;
            }
        }

        return bla.toString();
    }

    private String restoreStringLiterals(String temp) {
        for (Map.Entry<String, String> entry : blaMap.entrySet()) {
            temp = temp.replace(entry.getKey(), entry.getValue());
        }
        return temp;
    }

    private static String fill(final int len) {
        if (len == 0) return "";//format %1$0s breaks

        String paddedString = String.format("%1$" + len + "s", "");
        if (DEBUG) return paddedString.replace(" ", "X");
        return paddedString;
    }

    private static String strcat(final String src, final String str, final int position, final int len) {
        StringBuilder temp = new StringBuilder(src);
        temp.replace(position, position + len, str);
        return temp.toString();
    }
}
