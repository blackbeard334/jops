package com.bla;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bla.BlaTest.BlaSimpleJavaFileObject;
import static com.sun.source.util.TaskEvent.Kind.PARSE;
import static com.sun.tools.javac.tree.JCTree.JCAssign;
import static com.sun.tools.javac.tree.JCTree.JCBlock;
import static com.sun.tools.javac.tree.JCTree.JCClassDecl;
import static com.sun.tools.javac.tree.JCTree.JCExpression;
import static com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import static com.sun.tools.javac.tree.JCTree.JCForLoop;
import static com.sun.tools.javac.tree.JCTree.JCIf;
import static com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import static com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import static com.sun.tools.javac.tree.JCTree.JCParens;
import static com.sun.tools.javac.tree.JCTree.JCReturn;
import static com.sun.tools.javac.tree.JCTree.JCStatement;

public class BlaPlugin implements Plugin {
    public static final String NAME = "BlaPlugin";

    private static List<JavaFileObject>                      changedFiles      = new ArrayList<>();
    private static Map<Name, Type>                           nameTypeMap       = new HashMap<>();
    private static Map<Name, BlaVerifier.BlaOverloadedClass> overloadedClasses = new HashMap<>();

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
                if (e.getKind() != PARSE) return;


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
                    final CharBuffer src = (CharBuffer) sourceFile.getCharContent(false);
                    BlaParser blaParser = new BlaParser(src.toString());
                    if (blaParser.hasOverloadingShallow()) {
                        final String confusion = blaParser.parseAndReplace();
                        updateClientFileObject(sourceFile, src, CharBuffer.wrap(confusion.toCharArray()));
                        changedFiles.add(sourceFile);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                Log.instance(context).printRawLines(sourceFile.getName());
            }

            @Override
            public void finished(TaskEvent e) {
                switch (e.getKind()) {
                    case PARSE: {
                        final BlaVerifier blaVerifier = new BlaVerifier(e.getCompilationUnit());
                        if (changedFiles.contains(e.getSourceFile())) {
                            if (!blaVerifier.isValid()) {
//                        changedFiles.remove(uri);//TODO
                            } else {
                                blaVerifier.getOverloadedClasses().forEach(clazz -> overloadedClasses.put(clazz.name, clazz));
                            }
                        }
                        break;
                    }

                    case ENTER:/*the dragon*/ {
                        if (!hasOverloadedClassesImported(e.getCompilationUnit())) {
                            return;
                        }

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
                        final CompilationUnitTree compilationUnit = e.getCompilationUnit();
                        compilationUnit.getImports().stream()
                                .map(ImportTree::getQualifiedIdentifier)
                                .map(JCTree.JCFieldAccess.class::cast)
                                .filter(i -> isOverloadedType(i.name))
                                .forEach(i -> nameTypeMap.put(i.name, i.type)); //TODO init list with imports, and figure out how to reach da root elementz
                        final List<? extends Tree> typeDecls = compilationUnit.getTypeDecls();
                        typeDecls.forEach(BlaPlugin::bla);

//                //TODO scan imports for overloaded classes
//                int a = 0;//TODO change the name/path back after compilation?
                    }
                    default:
                        break;//skip
                }
            }
        });
    }


    private static Tree bla(Tree tree) {
        switch (tree.getClass().getSimpleName()) {
            case "JCClassDecl":
                JCClassDecl classDecl = (JCClassDecl) tree;
                classDecl.defs.forEach(BlaPlugin::bla);
                break;
            case "JCVariableDecl":
                JCVariableDecl variableDecl = (JCVariableDecl) tree;
                if (variableDecl.getType() instanceof JCTree.JCIdent) {  //TODO optimize later
                    final JCTree.JCIdent type = (JCTree.JCIdent) variableDecl.getType();
                    if (isOverloadedType(type)) {
                        nameTypeMap.put(variableDecl.getName(), type.type);
                    }
                }
                variableDecl.init = (JCExpression) bla(variableDecl.init);
                break;
            case "JCMethodDecl":
                JCMethodDecl methodDecl = (JCMethodDecl) tree;
                methodDecl.getParameters().stream()
                        .filter(p -> p.getType() instanceof JCTree.JCIdent)
                        .forEach(p -> {
                            final JCTree.JCIdent type = (JCTree.JCIdent) p.getType();
                            if (isOverloadedType(type)) {
                                nameTypeMap.put(p.getName(), type.type);
                            }
                        });
                methodDecl.body = (JCBlock) bla(methodDecl.body);
                break;
            case "JCBinary": //TODO check opcodes and types
                JCBinary binary = (JCBinary) tree;
                binary.lhs = (JCExpression) bla(binary.lhs);
                binary.rhs = (JCExpression) bla(binary.rhs);
                //TODO wait for the recursive calls to return to know all the return values
                if (binary.lhs instanceof JCTree.JCIdent) {
                    JCTree.JCIdent lhs = (JCTree.JCIdent) binary.lhs;
                    if (nameTypeMap.containsKey(lhs.getName())) {
                        final Type type = nameTypeMap.get(lhs.getName());
                        final BlaVerifier.BlaOverloadedClass overloadedClass = overloadedClasses.get(type.tsym.name);
                        final JCMethodDecl method = overloadedClass.getMethod(binary.getTag());

                        if (method != null) {
                            // return new method invoke
                            final OJCFieldAccess overriddenMethod = new OJCFieldAccess(binary.lhs, method.getName());
                            return new OJCMethodInvocation(null, overriddenMethod, com.sun.tools.javac.util.List.of(binary.rhs));
                        }
                        // if type has binary.getOperator() && binary.rhs is the correct param type
                    }
                }
                break;
            case "JCBlock":
                JCBlock block = (JCBlock) tree;
                block.stats.forEach(BlaPlugin::bla);
                break;
            case "JCMethodInvocation": //method params can be (a + b)
                JCMethodInvocation methodInvocation = (JCMethodInvocation) tree;
                methodInvocation.args = methodInvocation.args.stream()
                        .map(BlaPlugin::bla)
                        .map(JCExpression.class::cast)
                        .collect(com.sun.tools.javac.util.List.collector());
                break;
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
            case "JCAssignOp":
                JCAssignOp assignOp = (JCAssignOp) tree;
                assignOp.lhs = (JCExpression) bla(assignOp.lhs);
                assignOp.rhs = (JCExpression) bla(assignOp.rhs);
                break;
            case "JCIf":
                JCIf jcIf = (JCIf) tree;
                jcIf.cond = (JCExpression) bla(jcIf.cond);
                jcIf.thenpart = (JCStatement) bla(jcIf.thenpart);
                break;
            case "JCForLoop":
                JCForLoop forLoop = (JCForLoop) tree;
                forLoop.init = forLoop.init.stream()
                        .map(BlaPlugin::bla)
                        .map(JCStatement.class::cast)
                        .collect(com.sun.tools.javac.util.List.collector());
                forLoop.cond = (JCExpression) bla(forLoop.cond);
                forLoop.step = forLoop.step.stream()
                        .map(BlaPlugin::bla)
                        .map(JCExpressionStatement.class::cast)
                        .collect(com.sun.tools.javac.util.List.collector());
                forLoop.body = (JCStatement) bla(forLoop.body);
                break;

            case "JCIdent":
            case "JCLiteral":
            case "JCFieldAccess":
            case "JCUnary":
            default:
                return tree;
        }

        return tree;
    }

    private static boolean isOverloadedType(Name name) {//TODO rename
        return overloadedClasses.containsKey(name);
    }

    private static boolean isOverloadedType(JCTree.JCIdent ident) {
        return isOverloadedType(ident.getName());
    }

    private boolean updateClientFileObject(final JavaFileObject sourceFile, CharBuffer originalSourceCode, final CharBuffer replacedSourceCode) {
        Field fileManagerField = null;
        Field hbField = null, offsetField = null, readOnlyField = null, markField = null, positionField = null, limitField = null, capacityField = null, addessField = null;
        try {
            fileManagerField = sourceFile.getClass().getSuperclass().getDeclaredField("fileManager");
            fileManagerField.setAccessible(true);
            JavacFileManager fileManagerObject = (JavacFileManager) fileManagerField.get(sourceFile);
            fileManagerObject.cache(sourceFile, replacedSourceCode);
            {
                /*
                 The source is alraedy read at com.sun.tools.javac.main.JavaCompiler.parse(javax.tools.JavaFileObject)
                 So we need to change it here
                 */
                hbField = CharBuffer.class.getDeclaredField("hb");
                hbField.setAccessible(true);
                hbField.set(originalSourceCode, hbField.get(replacedSourceCode));

                offsetField = CharBuffer.class.getDeclaredField("offset");
                offsetField.setAccessible(true);
                offsetField.set(originalSourceCode, offsetField.get(replacedSourceCode));

                readOnlyField = CharBuffer.class.getDeclaredField("isReadOnly");
                readOnlyField.setAccessible(true);
                readOnlyField.set(originalSourceCode, readOnlyField.get(replacedSourceCode));

                {
                    markField = Buffer.class.getDeclaredField("mark");
                    markField.setAccessible(true);
                    markField.set(originalSourceCode, markField.get(replacedSourceCode));

                    positionField = Buffer.class.getDeclaredField("position");
                    positionField.setAccessible(true);
                    positionField.set(originalSourceCode, positionField.get(replacedSourceCode));

                    limitField = Buffer.class.getDeclaredField("limit");
                    limitField.setAccessible(true);
                    limitField.set(originalSourceCode, limitField.get(replacedSourceCode));

                    capacityField = Buffer.class.getDeclaredField("capacity");
                    capacityField.setAccessible(true);
                    capacityField.set(originalSourceCode, capacityField.get(replacedSourceCode));

                    addessField = Buffer.class.getDeclaredField("address");
                    addessField.setAccessible(true);
                    addessField.set(originalSourceCode, addessField.get(replacedSourceCode));
                }
            }
            return true;
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            if (fileManagerField != null)
                fileManagerField.setAccessible(false);
            {
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

    private boolean hasOverloadedClassesImported(final CompilationUnitTree compilationUnit) {
        return compilationUnit.getImports().stream()
                .map(ImportTree::getQualifiedIdentifier)
                .map(JCTree.JCFieldAccess.class::cast)
                .map(JCTree.JCFieldAccess::getIdentifier)
                .anyMatch(BlaPlugin::isOverloadedType);
    }
}
