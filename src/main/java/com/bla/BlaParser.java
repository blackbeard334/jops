package com.bla;

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
    private static final String OVERLOADING_IMPORT     = "abc";
    private static final String OVERLOADING_ANNOTATION = "def";

    private boolean  inCommentBlock = false;
    private int      currentLine    = 0;
    private long     charsThusFar   = 0;
    private String[] lines;

    void parse(final String sourceFile) {
        if (hasOverloadingShallow(sourceFile)) {
            lines = sourceFile.split("\n");
            if (hasOverloadingDeep()) {
                for (; currentLine < lines.length; currentLine++) {
                    //do stuff
                }
            }
        }
    }

    private boolean hasOverloadingShallow(final String sourceFile) {
        return sourceFile.contains(OVERLOADING_IMPORT) && sourceFile.contains(OVERLOADING_ANNOTATION);
    }

    private boolean hasOverloadingDeep() {
        return hasImport() && hasAnnotation();
    }

    private boolean hasImport() { //TODO check import is before class
        for (; currentLine < lines.length; currentLine++) {
            charsThusFar += lines[currentLine].length();
            lines[currentLine] = stripComments(lines[currentLine]);
            lines[currentLine] = stripStringLiterals(lines[currentLine]);
            if (lines[currentLine].contains(OVERLOADING_IMPORT)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnnotation() {  //TODO check the annotation is on class
        for (; currentLine < lines.length; currentLine++) {
            charsThusFar += lines[currentLine].length();
            lines[currentLine] = stripComments(lines[currentLine]);
            lines[currentLine] = stripStringLiterals(lines[currentLine]);
            if (lines[currentLine].contains(OVERLOADING_ANNOTATION)) {
                return true;
            }
        }
        return false;
    }

    private String stripComments(final String line) {
        String bla = line;
        int index, leftComment, rightComment;

        //dangling blocks
        if (inCommentBlock) {
            if ((rightComment = bla.indexOf("*/")) != -1) {
//                bla = bla.substring(0, rightComment);
                final int len = bla.length() - rightComment;
                bla = strcat(bla, fill(len), 0, len);
                inCommentBlock = false;
            } else
                return fill(line.length());
        }

        //skip single line comments
        if ((index = bla.indexOf("//")) != -1) {
            final int len = bla.length() - index;
            bla = strcat(bla, fill(len), index, len);
        }

        //skip blocks
        while ((leftComment = bla.indexOf("/*")) != -1) {
            final int len;
            if ((rightComment = bla.indexOf("*/", leftComment)) != -1) {
                len = rightComment - leftComment;
//                bla = bla.substring(0, leftComment) + bla.substring(leftComment + rightComment);
                bla = strcat(bla, fill(len), leftComment, len);
            } else {
                len = bla.length() - leftComment;
//                bla = bla.substring(0, leftComment);
                bla = strcat(bla, fill(len), leftComment, len);
                inCommentBlock = true;
                break;
            }
        }

        assert (bla.length() == line.length());
        return bla;
    }

    private String stripStringLiterals(final String line) {
        String bla = line.replace("\"", "\t");//remove escaped quotes
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
        return String.format("%1$" + len + "s", "");
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
