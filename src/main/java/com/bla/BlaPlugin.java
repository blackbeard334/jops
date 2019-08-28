package com.bla;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.bla.BlaTest.BlaSimpleJavaFileObject;
import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.tree.JCTree.JCBlock;
import static com.sun.tools.javac.tree.JCTree.JCClassDecl;
import static com.sun.tools.javac.tree.JCTree.JCExpression;
import static com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import static com.sun.tools.javac.tree.JCTree.JCReturn;
import static com.sun.tools.javac.tree.JCTree.JCStatement;

public class BlaPlugin implements Plugin {
    public static final String NAME = "BlaPlugin";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(JavacTask task, String... args) {
        Context context = ((BasicJavacTask) task).getContext();
        Log.instance(context).printRawLines("Yow!!!!1One");

        MultiTaskListener.instance(context);
        task.addTaskListener(new TaskListener() {
            @Override
            public void started(TaskEvent e) {
                if (e.getKind() != TaskEvent.Kind.PARSE) return;


//                CompilationUnitTree bla = new JCTree.JCCompilationUnit();
                JavaFileObject sourceFile = e.getSourceFile();
                if (sourceFile instanceof BlaSimpleJavaFileObject) {
                    int x = 0;
                }
                /**
                 * if we read from disk, then we need to point to a different file
                 * * or we could reflect and replace e.getSourceFile()
                 *
                 * if it's in-memory, then reflection?
                 */
                try {
                    BlaParser blaParser = new BlaParser(sourceFile.getCharContent(false).toString());
                    if (blaParser.hasOverloadingShallow()) {
                        final String confusion = blaParser.parseAndReplace();
                        final Path tempFilePath = createTempFile(sourceFile, confusion);

                        updateClientFileObject(sourceFile, confusion, tempFilePath);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                Log.instance(context).printRawLines(sourceFile.getName());
            }

            @Override
            public void finished(TaskEvent e) {
                /**
                 * 1-we need a list of valid overloaded classes
                 * 2-we then check every class for the imports
                 *  a-only parse the classes with overridden imports
                 *  b-create a list of all the imported overridden classes to compare with
                 *  c-scan every type I guess?
                 *  d-and replace the binary tree with equivalents operations
                 *   i-we need to check parameter types and such
                 *   ii-return types could be important for chained calls
                 *   iii-TODO would replacing pre-compilation be better performance-wise!?
                 * 3-a corner case that import scanning wouldn't be able to find is overloaded operations within the defining class, so the imports should just be implied then I guess
                 */
                //((com.sun.tools.javac.util.List)((JCClassDecl)((com.sun.tools.javac.util.List)((JCCompilationUnit)e.unit).defs).get(2)).defs).get(5)
                final CompilationUnitTree compilationUnit = e.getCompilationUnit();
                final List<? extends Tree> typeDecls = compilationUnit.getTypeDecls();
                final com.sun.tools.javac.util.List<JCTree> members = ((JCClassDecl) typeDecls.get(0)).getMembers();
                final JCBlock body = ((JCMethodDecl) members.get(5)).getBody();
                final com.sun.tools.javac.util.List<JCStatement> statements = body.getStatements();
                final JCExpression expression = ((JCReturn) statements.get(0)).getExpression();
                if (expression instanceof JCBinary) {
                    final JCBinary expression1 = (JCBinary) expression;
                    final Symbol.OperatorSymbol operator = expression1.getOperator();
//                    expression1.getLeftOperand()
                }
                //TODO scan imports for overloaded classes
                int a = 0;//TODO change the name/path back after compilation?
            }
        });
    }

    private JCTree bla(JCTree tree) {
        switch (tree.getClass().getSimpleName()) {
            case "JCClassDecl":
                JCClassDecl classDecl = (JCClassDecl) tree;
                classDecl.defs.replaceAll(this::bla);
                break;
            case "JCVariableDecl":
                JCVariableDecl variableDecl = (JCVariableDecl) tree;
                variableDecl.init = (JCExpression) bla(variableDecl.init);
                break;
            case "JCMethodDecl":
                JCMethodDecl methodDecl = (JCMethodDecl) tree;
                methodDecl.body = (JCBlock) bla(methodDecl.body);
                break;
            case "JCBinary": //TODO check opcodes and types
                JCBinary binary = (JCBinary) tree;
                binary.lhs = (JCExpression) bla(binary.lhs);
                binary.rhs = (JCExpression) bla(binary.rhs);
                break;
            case "JCBlock":
                JCBlock block = (JCBlock) tree;
                block.stats.replaceAll(tree1 -> (JCStatement) bla(tree1));
                break;
            case "JCMethodInvocation": //method params can be (a + b)
                JCMethodInvocation methodInvocation = (JCMethodInvocation) tree;
                methodInvocation.args.replaceAll(m -> (JCExpression) bla(m));
                break;
            case "JCIdent":
            case "JCLiteral":
                return tree;
            case "JCParens":
                JCParens parens = (JCParens) tree;
                parens.expr = (JCExpression) bla(parens.expr);
                break;
            case "JCExpressionStatement":
                JCExpressionStatement expressionStatement = (JCExpressionStatement) tree;
                expressionStatement.expr = (JCExpression) bla(expressionStatement.expr);
                break;
            case "JCReturn":
                JCReturn jcReturn = (JCReturn) tree;
                jcReturn.expr = (JCExpression) bla(jcReturn.expr);
                break;
            case "JCAssign": //a unique annoying fucking case
                JCAssign assign = (JCAssign) tree;
                assign.lhs = (JCExpression) bla(assign.lhs);
                assign.rhs = (JCExpression) bla(assign.rhs);
                break;
            case "JCIf":
                JCIf jcIf = (JCIf) tree;
                jcIf.cond = (JCExpression) bla(jcIf.cond);
                jcIf.thenpart = (JCStatement) bla(jcIf.thenpart);
                break;
            case "JCForLoop":
                JCForLoop forLoop = (JCForLoop) tree;
                forLoop.init.replaceAll(i -> (JCStatement) bla(i));
                forLoop.cond = (JCExpression) bla(forLoop.cond);
                forLoop.step.replaceAll(s -> (JCExpressionStatement) bla(s));
                forLoop.body = (JCStatement) bla(forLoop.body);
                break;
            default:
                throw new UnsupportedOperationException(tree.getClass().getSimpleName());
        }

        return tree;
    }

    @Deprecated
    private JCBinary bla(JCTree lhs, JCTree rhs) {
        //TODO check JCParens
        //TODO check stuff like if statements, ternary, variable definition/assignment...etc
        if (lhs instanceof JCBinary) {
            final JCBinary lhs1 = (JCBinary) lhs;
            lhs = bla(lhs1.lhs, lhs1.rhs);//lhs & rhs are public \(^o^)/
        }
        if (rhs instanceof JCBinary) {
            final JCBinary rhs1 = (JCBinary) rhs;
            rhs = bla(rhs1.lhs, rhs1.rhs);
        }

        return null;
    }

    private boolean updateClientFileObject(final JavaFileObject sourceFile, final String str, final Path path) {
        Field clientFileObjectField = null;
        Field uriField = null, fileContentField = null;
        Field stringValueField = null, stringCoderField = null, stringHashField = null;
        try {
            clientFileObjectField = sourceFile.getClass().getSuperclass().getDeclaredField("clientFileObject");
            clientFileObjectField.setAccessible(true);
            FileObject clientFileObject = (FileObject) clientFileObjectField.get(sourceFile);
            {
                uriField = SimpleJavaFileObject.class.getDeclaredField("uri");
                uriField.setAccessible(true);
                uriField.set(clientFileObject, path.toUri());

                fileContentField = BlaSimpleJavaFileObject.class.getDeclaredField("fileContent");
                fileContentField.setAccessible(true);
                String fileContent = (String) fileContentField.get(clientFileObject);
                {
                    /*
                     The source is alraedy read at com.sun.tools.javac.main.JavaCompiler.parse(javax.tools.JavaFileObject)
                     So we need to change it here
                     */
                    stringValueField = String.class.getDeclaredField("value");
                    stringValueField.setAccessible(true);
                    stringValueField.set(fileContent, stringValueField.get(str));

                    stringCoderField = String.class.getDeclaredField("coder");
                    stringCoderField.setAccessible(true);
                    stringCoderField.set(fileContent, stringCoderField.get(str));

                    stringHashField = String.class.getDeclaredField("hash");
                    stringHashField.setAccessible(true);
                    stringHashField.set(fileContent, stringHashField.get(str));
                }
            }
            return true;
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            if (clientFileObjectField != null)
                clientFileObjectField.setAccessible(false);
            {
                if (uriField != null)
                    uriField.setAccessible(false);
                if (fileContentField != null)
                    fileContentField.setAccessible(false);
                {
                    if (stringValueField != null)
                        stringValueField.setAccessible(false);
                    if (stringCoderField != null)
                        stringCoderField.setAccessible(false);
                    if (stringHashField != null)
                        stringHashField.setAccessible(false);
                }
            }
        }
    }

    private Path createTempFile(final JavaFileObject sourceFile, final CharSequence csq) throws IOException {
        final Path fileName = Paths.get(sourceFile.toUri()).getFileName();
        final Path tempDir = Files.createTempDirectory("");
        final Path tempFile = Files.createFile(tempDir.resolve(fileName));

        return Files.writeString(tempFile, csq);
    }
}
