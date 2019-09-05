package com.bla;

import com.bla.annotation.OperatorOverloading;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.tree.JCTree;

/**
 * check for annotation
 * tokenize by space?
 * skip comments
 * *skip after //
 * *skip from /* till *\/
 * skip strings from " till "
 * * rather than skip, we should fill the range with something
 * method check
 * *ONE PARAM
 * *return type present
 * *find a way to rename compilation errors back to their operator+ variants instead of oPlus
 */
public class BlaParser implements Parser {
    private static final String  OVERLOADING_IMPORT     = OperatorOverloading.class.getName();
    private static final String  OVERLOADING_ANNOTATION = OperatorOverloading.ANNOTATION;
    private static final boolean DEBUG                  = false;

    private boolean inCommentBlock = false;
    private int     currentLine    = 0;
    private long    charsThusFar   = 0;

    private final String sourceFile;
    private final String noComments;
    private final String noCommentsOrStrings;
    private final String noCommentsOrStringsOrWhiteSpace;

    public BlaParser(final String sourceFile) {
        this.sourceFile = sourceFile;
        noComments = stripComments(sourceFile);
        noCommentsOrStrings = stripStringLiterals(noComments);
        noCommentsOrStringsOrWhiteSpace = noCommentsOrStrings.replaceAll("\\s", "");
    }

    /** seek & destroy */
    public String parseAndReplace() {
        String temp = noCommentsOrStrings;
        if (hasOverloadingShallow()) {
//            lines = sourceFile.split("\n");
            for (OPS operator : OPS.values()) {
                temp = operator.otor(temp);//TODO check operator locations after the comments/strings have been stripped, and then replace them in the original
            }
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

    public boolean hasOverloadingShallow() {
        // remove break lines from source
        return noCommentsOrStringsOrWhiteSpace.contains(OVERLOADING_IMPORT) && noCommentsOrStringsOrWhiteSpace.contains(OVERLOADING_ANNOTATION);
    }

    private String stripComments(final String source) {
        String bla = source;
        int index, leftComment, rightComment = -1;

        //skip single line comments
        if ((index = bla.indexOf("//")) != -1) {
            final int len = bla.indexOf('\n', index) - index - 2;
            bla = strcat(bla, fill(len), index + 2, len);
        }

        //skip blocks
        while ((leftComment = bla.indexOf("/*", rightComment)) != -1) {
            final int len;
            if ((rightComment = bla.indexOf("*/", leftComment + 2)) != -1) {
                len = rightComment - leftComment - 2;
                bla = strcat(bla, fill(len), leftComment + 2, len);
            } else {//TODO broken comment with no closing tag
                len = bla.length() - leftComment;
                bla = strcat(bla, fill(len), leftComment, len);
                inCommentBlock = true;
                break;
            }
        }

        assert (bla.length() == source.length());
        return bla;
    }

    private String stripStringLiterals(final String source) {//TODO empty strings throw a null pointer
        String bla = source.replace("\\\"", "\t");//remove escaped quotes
        int leftQuotes, rightQuotes = -1;

        while ((leftQuotes = bla.indexOf('"', rightQuotes + 1)) != -1) {
            rightQuotes = bla.indexOf('"', leftQuotes + 1);
            if (rightQuotes != -1) {
                final int len = rightQuotes - leftQuotes - 1;
                bla = strcat(bla, fill(len), leftQuotes + 1, len);
            }
        }

        return bla;
    }

    private String fill(final int len) {
        String paddedString = String.format("%1$" + len + "s", "");
        if (DEBUG) return paddedString.replace(" ", "X");
        return paddedString;
    }

    private String strcat(final String src, final String str, final int position, final int len) {
        StringBuilder temp = new StringBuilder(src);
        temp.replace(position, position + len, str);
        return temp.toString();
    }

    @Override
    public JCTree.JCCompilationUnit parseCompilationUnit() {
        return null;
    }

    @Override
    public JCTree.JCExpression parseExpression() {
        return null;
    }

    @Override
    public JCTree.JCStatement parseStatement() {
        return null;
    }

    @Override
    public JCTree.JCExpression parseType() {
        return null;
    }
}
