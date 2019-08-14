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
 * *return type present
 * *find a way to rename compilation errors back to their operator+ variants instead of oPlus
 */
public class BlaParser implements Parser {
    private static final String  OVERLOADING_IMPORT     = OperatorOverloading.class.getName();
    private static final String  OVERLOADING_ANNOTATION = "@OperatorOverloading";
    private static final boolean DEBUG                  = true;

    private boolean  inCommentBlock = false;
    private int      currentLine    = 0;
    private long     charsThusFar   = 0;
    private String[] lines;

    void parse(final String sourceFile) {
        String noCommentOrStringSource = stripStringLiterals(stripComments(sourceFile));
        if (hasOverloadingShallow(noCommentOrStringSource)) {
//            lines = sourceFile.split("\n");
            for (OPS operator : OPS.values()) {
                noCommentOrStringSource = operator.otor(noCommentOrStringSource);
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
    }

    private boolean hasOverloadingShallow(final String sourceFile) {
        // remove break lines from source
        String sourceFileNoBreaks = sourceFile.replaceAll("\\s", "");
        return sourceFileNoBreaks.contains(OVERLOADING_IMPORT) && sourceFileNoBreaks.contains(OVERLOADING_ANNOTATION);
    }

    private String stripComments(final String source) {
        String bla = source;
        int index, leftComment, rightComment;

        //skip single line comments
        if ((index = bla.indexOf("//")) != -1) {
            final int len = bla.indexOf('\n', index) - index;
            bla = strcat(bla, fill(len), index, len);
        }

        //skip blocks
        while ((leftComment = bla.indexOf("/*")) != -1) {
            final int len;
            if ((rightComment = bla.indexOf("*/", leftComment)) != -1) {
                len = rightComment - leftComment + 2;
                bla = strcat(bla, fill(len), leftComment, len);
            } else {
                len = bla.length() - leftComment;
                bla = strcat(bla, fill(len), leftComment, len);
                inCommentBlock = true;
                break;
            }
        }

        assert (bla.length() == source.length());
        return bla;
    }

    private String stripStringLiterals(final String source) {
        String bla = source.replace("\"", "\t");//remove escaped quotes
        int leftQuotes, rightQuotes;

        while ((leftQuotes = bla.indexOf('"')) != -1) {
            rightQuotes = bla.indexOf('"', leftQuotes);
            if (rightQuotes != -1) {
                final int len = rightQuotes - leftQuotes;
                bla = strcat(bla, fill(len), leftQuotes, len);
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
