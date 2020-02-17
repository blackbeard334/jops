package com.jops;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Operators;
import com.sun.tools.javac.comp.Todo;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sun.source.util.TaskEvent.Kind.PARSE;

/**
 * @version 0.79
 */
public final class JOPSPlugin implements Plugin {
    public static final String NAME = "JOPSPlugin";

    /** here we store the files that were changed BEFORE the PARSE stage */
    private static List<JavaFileObject>       overloadedSources = new ArrayList<>();
    /** a map of all the classes with operator overloading. the value contains all the overloaded methods for said class */
    static         Map<Name, OverloadedClass> overloadedClasses = new HashMap<>();

    // consider this the autowire section.
    /** the symtab has a lot of useful info, but in this particular case we need it for the primitiveType names, which are often not known yet during PARSE */
    static         Symtab    symtab;
    private static Operators operators;

    /** we need this for logging */
    private static Log                 log;
    private static JavacTrees          javacTrees;
    private static CompilationUnitTree currentCompilationUnit;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(JavacTask task, String... args) {
        final Context context = ((BasicJavacTask) task).getContext();
        log = Log.instance(context);
        log.printRawLines("Yow!!!!1One");
        symtab = Symtab.instance(context);
        operators = Operators.instance(context);
        javacTrees = JavacTrees.instance(context);

        task.addTaskListener(new MyTaskListener(context));
    }

    private static Tree parse(Tree tree) {
        //JCVariableDecl--> int i; means that the init part is null
        if (tree == null) return null;

        switch (tree.getClass().getSimpleName()) {
            case "JCClassDecl":
                JCClassDecl classDecl = (JCClassDecl) tree;
                classDecl.defs.forEach(JOPSPlugin::parse);
                break;
            case "JCVariableDecl":
                JCVariableDecl variableDecl = (JCVariableDecl) tree;
                variableDecl.init = (JCExpression) parse(variableDecl.init);
                break;
            case "JCMethodDecl":
                JCMethodDecl methodDecl = (JCMethodDecl) tree;
                methodDecl.body = (JCBlock) parse(methodDecl.body);
                break;
            case "JCBinary": //TODO check opcodes and types
                JCBinary binary = (JCBinary) tree;
                binary.lhs = (JCExpression) parse(binary.lhs);
                binary.rhs = (JCExpression) parse(binary.rhs);
                return parseOperatorExpression(binary);
            case "JCBlock":
                JCBlock block = (JCBlock) tree;
                block.stats.forEach(JOPSPlugin::parse);
                break;
            case "JCMethodInvocation": //method params can be (a + b)
                JCMethodInvocation methodInvocation = (JCMethodInvocation) tree;
                methodInvocation.args = methodInvocation.args.stream()
                        .map(JOPSPlugin::parse)
                        .map(JCExpression.class::cast)
                        .collect(com.sun.tools.javac.util.List.collector());
                methodInvocation.meth = (JCExpression) parse(methodInvocation.meth);
                /**
                 * methodInvocation.meth instanceof
                 * JCIdent if static import or called with this.bla()
                 * JCFieldAccess otherwise
                 */
                if (!(methodInvocation.type instanceof Type.MethodType)) {
                    JCExpression meth = methodInvocation.meth;
                    if (meth instanceof JCTree.JCFieldAccess) {
                        setMethodSymbolIfERR(methodInvocation, (JCTree.JCFieldAccess) meth);
                        methodInvocation.type = ((JCTree.JCFieldAccess) meth).sym.type.getReturnType();
                        meth.type = ((JCTree.JCFieldAccess) meth).sym.type;
                    } else {
                        methodInvocation.type = ((JCTree.JCIdent) meth).sym.type.getReturnType();
                        meth.type = ((JCTree.JCIdent) meth).sym.type;
                    }
                }
                break;
            case "JCParens":
                JCParens parens = (JCParens) tree;
                parens.expr = (JCExpression) parse(parens.expr);
                JCExpression expr = parens.expr;
                if (expr instanceof OJCParens ||
                        expr instanceof JCMethodInvocation ||
                        expr instanceof JCTree.JCIdent) {
                    return new OJCParens(expr);
                }

                if (parens.type.tsym.kind == Kinds.Kind.ERR) {
                    parens.type = parens.expr.type;
                }
                /**
                 * *return value of a method
                 * *overloaded method
                 * *otherwise don't change anything
                 */
                break;
            case "JCExpressionStatement":
                JCExpressionStatement expressionStatement = (JCExpressionStatement) tree;
                expressionStatement.expr = (JCExpression) parse(expressionStatement.expr);
                break;
            case "JCReturn":
                JCReturn jcReturn = (JCReturn) tree;
                jcReturn.expr = (JCExpression) parse(jcReturn.expr);
                break;
            case "JCAssign": //a unique annoying fucking case
                JCAssign assign = (JCAssign) tree;
                assign.lhs = (JCExpression) parse(assign.lhs);
                assign.rhs = (JCExpression) parse(assign.rhs);
                break;
            case "JCAssignOp":
                JCAssignOp assignOp = (JCAssignOp) tree;
                assignOp.lhs = (JCExpression) parse(assignOp.lhs);
                assignOp.rhs = (JCExpression) parse(assignOp.rhs);
                return parseOperatorExpression(assignOp);
            case "JCIf":
                JCIf jcIf = (JCIf) tree;
                jcIf.cond = (JCExpression) parse(jcIf.cond);
                jcIf.thenpart = (JCStatement) parse(jcIf.thenpart);
                jcIf.elsepart = (JCStatement) parse(jcIf.elsepart);
                break;
            case "JCForLoop":
                JCForLoop forLoop = (JCForLoop) tree;
                forLoop.init = forLoop.init.stream()
                        .map(JOPSPlugin::parse)
                        .map(JCStatement.class::cast)
                        .collect(com.sun.tools.javac.util.List.collector());
                forLoop.cond = (JCExpression) parse(forLoop.cond);
                forLoop.step = forLoop.step.stream()
                        .map(JOPSPlugin::parse)
                        .map(JCExpressionStatement.class::cast)
                        .collect(com.sun.tools.javac.util.List.collector());
                forLoop.body = (JCStatement) parse(forLoop.body);
                break;

            case "JCFieldAccess":
                JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) tree;
                fieldAccess.selected = (JCExpression) parse(fieldAccess.selected);
                if (fieldAccess.selected instanceof JCMethodInvocation && fieldAccess.sym.type instanceof Type.MethodType) {
                    fieldAccess.type = fieldAccess.sym.type.getReturnType();
                }
                if (fieldAccess.selected instanceof JCMethodInvocation && fieldAccess.sym.kind == Kinds.Kind.ERR) {
                    final Symbol selectedSymbol = fieldAccess.selected.type.tsym.getEnclosedElements().stream()
                            .filter(e -> e.name.equals(fieldAccess.name))
                            .findAny()
                            .orElseThrow();
                    fieldAccess.sym = selectedSymbol;
                    fieldAccess.type = selectedSymbol.type;
                }
                break;
            case "JCUnary":
                JCTree.JCUnary jcUnary = (JCTree.JCUnary) tree;
                jcUnary.arg = (JCExpression) parse(jcUnary.arg);
                if (jcUnary.type.tsym.kind == Kinds.Kind.ERR)
                    jcUnary.type = jcUnary.arg.type;
                break;
            case "JCTypeCast":
                JCTree.JCTypeCast typeCast = (JCTree.JCTypeCast) tree;
                typeCast.expr = (JCExpression) parse(typeCast.expr);
                break;
            case "JCNewClass":
                JCTree.JCNewClass jcNewClass = (JCTree.JCNewClass) tree;
                jcNewClass.args = jcNewClass.args.stream()
                        .map(JOPSPlugin::parse)
                        .map(JCExpression.class::cast)
                        .collect(com.sun.tools.javac.util.List.collector());
                if (jcNewClass.constructorType.tsym.kind == Kinds.Kind.ERR) {
                    jcNewClass.constructorType = jcNewClass.constructor.type;
                }
                break;
            case "JCIdent":
            case "JCLiteral":
            default:
                return tree;
        }

        return tree;
    }

    private static void setMethodSymbolIfERR(JCMethodInvocation methodInvocation, JCTree.JCFieldAccess meth) {
        //lookup the method and set the symbol
        //problem is if it's an overloaded method, then we need the args which aren't available here
        if (meth.sym.kind == Kinds.Kind.ERR || methodInvocation.type.tsym.kind == Kinds.Kind.ERR) {
            final Type selectedType = meth.selected.type;
            meth.sym = selectedType.tsym.getEnclosedElements().stream()
                    .filter(Symbol.MethodSymbol.class::isInstance)
                    .filter(sym -> sym.name.equals(meth.name))
                    .filter(sym -> ((Type.MethodType) sym.type).argtypes.size() == methodInvocation.args.size())
                    .filter(sym -> {
                        for (int i = 0; i < methodInvocation.args.size(); i++) {
                            if (!Utils.isLinealMatch(((Type.MethodType) sym.type).argtypes.get(i), methodInvocation.args.get(i).type)) {
                                return false;
                            }
                        }
                        return true;
                    })
                    .findAny()
                    .orElseThrow(() -> new RuntimeException(new NoSuchMethodException()));
        }
    }

    private static Tree parseOperatorExpression(JCTree.JCOperatorExpression expression) {
        final JCExpression left = expression.getOperand(JCTree.JCOperatorExpression.OperandPos.LEFT);
        final JCExpression right = expression.getOperand(JCTree.JCOperatorExpression.OperandPos.RIGHT);

        final OverloadedClass overloadedClass = overloadedClasses.get(getReturnTypeName(left.type));
        if (overloadedClass != null) {
            final MethodInformation method = overloadedClass.getMethodPolyEdition(expression.getTag(), right.type);

            if (method != null) {
                // return new method invoke
                final OJCFieldAccess overriddenMethod = new OJCFieldAccess(left, method.methodName, method.getSym());
                return new OJCMethodInvocation(null, overriddenMethod, com.sun.tools.javac.util.List.of(right));
            } else if (expression.getTag() != JCTree.Tag.NE && expression.getTag() != JCTree.Tag.EQ) {
                final String message = String.format("%s::operator%s(%s) isn't overloaded!", overloadedClass.getName(), getOperatorName(expression.getTag()), right.type.tsym.name);
                printErrorMessageAtSourceLocation(Diagnostic.Kind.ERROR, expression, message);
            }
            // if type has binary.getOperator() && binary.rhs is the correct param type
        }

        if (expression.type.tsym.kind == Kinds.Kind.ERR && left.type instanceof Type.JCPrimitiveType && right.type instanceof Type.JCPrimitiveType) {
            setPrimitiveOperationType((Type.JCPrimitiveType) left.type, (Type.JCPrimitiveType) right.type, expression);
        }
        return expression;
    }

    /** this method outputs the message at the correct location in the file being compiled */
    private static void printErrorMessageAtSourceLocation(final Diagnostic.Kind kind, final JCTree.JCOperatorExpression expression, final String message) {
        javacTrees.printMessage(kind, message, expression, currentCompilationUnit);
    }

    private static String getOperatorName(JCTree.Tag tag) {
        if (tag.isAssignop()) {
            return operators.operatorName(tag.noAssignOp()).toString() + "=";
        }
        return operators.operatorName(tag).toString();
    }

    /**
     * if we don't know that a binary operation contains both primitives cuz one was the result of an
     * overloaded operation(eg <b>1 + a * b</b>, where the result of <b>a * b</b> is obviously a primitive, since we
     * can't overload primitive ops
     */
    private static void setPrimitiveOperationType(final Type.JCPrimitiveType left, final Type.JCPrimitiveType right, JCTree.JCOperatorExpression expression) {
        final List<Type> types = Arrays.asList(left, right);
        // the order of this list is also based on the jls-5.1.2 section //TODO does the order of this list change per operation?
        if (types.contains(JOPSPlugin.symtab.doubleType))
            expression.type = JOPSPlugin.symtab.doubleType;
        else if (types.contains(JOPSPlugin.symtab.floatType))
            expression.type = JOPSPlugin.symtab.floatType;
        else if (types.contains(JOPSPlugin.symtab.longType))
            expression.type = JOPSPlugin.symtab.longType;
        else if (types.contains(JOPSPlugin.symtab.intType))
            expression.type = JOPSPlugin.symtab.intType;
        else if (types.contains(JOPSPlugin.symtab.shortType))
            expression.type = JOPSPlugin.symtab.shortType;
        else if (types.contains(JOPSPlugin.symtab.charType))
            expression.type = JOPSPlugin.symtab.charType;
        else if (types.contains(JOPSPlugin.symtab.byteType))
            expression.type = JOPSPlugin.symtab.byteType;
    }

    private static Name getReturnTypeName(Type type) {
        if (type instanceof Type.MethodType)
            return type.getReturnType().tsym.name;
        return type.tsym.getSimpleName();
    }

    private static final class MyTaskListener implements TaskListener {
        private final Context context;
        private       boolean todosInit;
        private       int     prevMaxErrors;

        MyTaskListener(final Context context) {
            this.context = context;
            this.todosInit = true;
            this.prevMaxErrors = -1;
        }

        @Override
        public void started(TaskEvent e) {
            if (e.getKind() != PARSE) return;

            JavaFileObject sourceFile = e.getSourceFile();
            try {
                final CharBuffer src = (CharBuffer) sourceFile.getCharContent(false);
                SourceParser srcParser = new SourceParser(src.toString());
                if (srcParser.hasOverloadingShallow()) {
                    final String parsedSrc = srcParser.parseAndReplace();
                    Utils.updateClientFileObject(CharBuffer.wrap(parsedSrc.toCharArray()), sourceFile);
                    Utils.updateString(src, CharBuffer.wrap(parsedSrc.toCharArray()));
                    overloadedSources.add(sourceFile);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            log.printRawLines(sourceFile.getName());
        }

        @Override
        public void finished(TaskEvent e) {
            switch (e.getKind()) {
                case PARSE: {
                    if (overloadedSources.contains(e.getSourceFile())) {
                        if (!SourceVerifier.isValid(e.getCompilationUnit())) {
//                        changedFiles.remove(uri);//TODO
                        } else {
                            SourceVerifier.getOverloadedClasses(e.getCompilationUnit()).forEach(clazz -> overloadedClasses.put(clazz.name, clazz));
                        }
                    }
                    break;
                }

                case ENTER:/*the dragon*/ {
                    if (todosInit) {
                        todosInit();
                    }
                    ClassVerifier.loadClassFiles(e);
                    if (!ClassVerifier.hasOverloadedClassesImported(e.getCompilationUnit())) {
                        return;//TODO add some kind of extra check for same package classes taht don't require an import
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
                    currentCompilationUnit = e.getCompilationUnit();
//                        currentCompilationUnit.getImports().stream()
//                                .map(ImportTree::getQualifiedIdentifier)
//                                .map(JCTree.JCFieldAccess.class::cast)
//                                .filter(i -> isOverloadedType(i.name))
//                                .forEach(i -> nameTypeMap.put(i.name, i.type.tsym.name)); //TODO init list with imports, and figure out how to reach da root elementz
                    final List<? extends Tree> typeDecls = currentCompilationUnit.getTypeDecls();
                    typeDecls.forEach(JOPSPlugin::parse);

//                //TODO scan imports for overloaded classes
//                int a = 0;//TODO change the name/path back after compilation?
                }
                default:
                    break;//skip
            }
        }

        /**
         * note that this class temporary disables logging because the {@link Attr#attrib(com.sun.tools.javac.comp.Env)}
         * method generates compilation errors. these errors are irrelevant since we are only using the method to enrich
         * our classes with type information and such.
         */
        private void todosInit() {
            try {
                //temp disable logging
                disableErrorLogging();

                //enrich our classes with type information and such
                for (Env<AttrContext> todo : Todo.instance(context)) {
                    Attr.instance(context).attrib(todo);
                }

            } catch (NoSuchFieldException | IllegalAccessException ex) {
                ex.printStackTrace();
            } finally {
                //re-enable error logging
                enableErrorLogging();
                todosInit = false;
            }
        }

        private void disableErrorLogging() throws IllegalAccessException, NoSuchFieldException {
            Field MaxErrors = null;
            try {
                MaxErrors = log.getClass().getDeclaredField("MaxErrors");
                MaxErrors.setAccessible(true);
                prevMaxErrors = MaxErrors.getInt(log);
                MaxErrors.set(log, 0);
                log.printRawLines("---------------------W-A-R-N-I-N-G-----------------------");
                log.printRawLines(">>>>>>>>>> Error log 'temporarily' disabled... <<<<<<<<<<");
                log.printRawLines("---------------------------------------------------------");
            } finally {
                if (MaxErrors != null)
                    MaxErrors.setAccessible(false);
            }
        }

        private void enableErrorLogging() {
            Field MaxErrors = null;
            if (prevMaxErrors > -1) {
                try {
                    MaxErrors = log.getClass().getDeclaredField("MaxErrors");
                    MaxErrors.setAccessible(true);
                    MaxErrors.set(log, prevMaxErrors);
                    log.printRawLines("---------------------------------------------");
                    log.printRawLines("<<<<<<<<<< Error log re-enabled... >>>>>>>>>>");
                    log.printRawLines("---------------------------------------------");
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                } finally {
                    if (MaxErrors != null)
                        MaxErrors.setAccessible(false);
                    prevMaxErrors = -1;
                }
            }
        }
    }
}
