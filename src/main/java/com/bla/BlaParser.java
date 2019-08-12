package com.bla;

import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.tree.JCTree;

public class BlaParser implements Parser {


    private static final String OVERLOADING_IMPORT     = "abc";
    private static final String OVERLOADING_ANNOTATION = "def";
    /**
     * check for annotation
     * tokenize by space?
     * skip comments
     * *skip after //
     * *skip from /* till *\/
     * skip strings from " till "
     * method check
     * *return type present
     */

    boolean isOverloaded   = false;
    boolean inCommentBlock = false;

    void parse(final String sourceFile) {
        String[] lines = sourceFile.split("\n");
        int[] index = {0};

        hasImport(lines, index);
        hasAnnotation(lines, index);

        if (isOverloaded) {

        }
    }

    private void hasImport(String[] tokens, int[] index) {
        while (!isOverloaded) {
            isOverloaded = tokens[index[0]++].contains(OVERLOADING_IMPORT);
        }
    }

    private void hasAnnotation(String[] tokens, int[] index) {
        while (!isOverloaded) { //TODO make another flag
            isOverloaded = tokens[index[0]++].contains(OVERLOADING_ANNOTATION);
        }
    }

    private String stripComments(final String line) {
        String bla = line;
        int index, leftComment, rightComment;

        //dangling blocks
        if (inCommentBlock) {
            if ((rightComment = bla.indexOf("*/")) != -1) {
                bla = bla.substring(0, rightComment);
                inCommentBlock = false;
            } else
                return "";
        }

        //skip single line comments
        if ((index = bla.indexOf("//")) != -1) bla = bla.substring(0, index);

        //skip blocks
        while ((leftComment = bla.indexOf("/*")) != -1) {
            if ((rightComment = bla.indexOf("*/", leftComment)) != -1) {
                bla = bla.substring(0, leftComment) + bla.substring(leftComment + rightComment);
            } else {
                bla = bla.substring(0, leftComment);
                inCommentBlock = true;
                break;
            }
        }

        return bla;
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
