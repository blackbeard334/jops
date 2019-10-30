package com.bla;

import com.bla.annotation.OperatorOverloading;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Todo;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

/** @version 0.76 */
public class BlaPlugin implements Plugin {
    public static final String NAME = "BlaPlugin";

    /** here we store the files that were changed BEFORE the PARSE stage */
    private static List<JavaFileObject>                      changedFiles      = new ArrayList<>();
    /** this map contains variable/param/...etc names, coupled with the name of their respective types. very helpful when we traverse trees, and no longer know what the out of scope variable type is */
    private static Map<Name, Name>                           nameTypeMap       = new HashMap<>();//TODO would using name instead of type use for different types with the same name?
    private static Map<Name, Symbol>                         nameClassMap      = new HashMap<>();
    /** a map of all the classes with operator overloading. the value contains all the overloaded methods for said class */
    private static Map<Name, BlaVerifier.BlaOverloadedClass> overloadedClasses = new HashMap<>();
    /** just a list of all the classes we already checked. is extra important when we scan .class files */
    private static Set<Symbol>                               checkedClasses    = new HashSet<>();

    /** consider this an autowire. the symtab has a lot of useful info, but in this particular case we need it for the primitiveType names, which are often not known yet during PARSE */
    static Symtab symtab;
    static Names  names;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(JavacTask task, String... args) {
        Context context = ((BasicJavacTask) task).getContext();
        Log.instance(context).printRawLines("Yow!!!!1One");
        symtab = Symtab.instance(context);
        names = Names.instance(context);
        //init class map
        nameClassMap.put(symtab.byteType.tsym.name, symtab.byteType.tsym);
        nameClassMap.put(symtab.charType.tsym.name, symtab.charType.tsym);
        nameClassMap.put(symtab.shortType.tsym.name, symtab.shortType.tsym);
        nameClassMap.put(symtab.longType.tsym.name, symtab.longType.tsym);
        nameClassMap.put(symtab.floatType.tsym.name, symtab.floatType.tsym);
        nameClassMap.put(symtab.intType.tsym.name, symtab.intType.tsym);
        nameClassMap.put(symtab.doubleType.tsym.name, symtab.doubleType.tsym);
        nameClassMap.put(symtab.booleanType.tsym.name, symtab.booleanType.tsym);
//        names.    getClass

        MultiTaskListener.instance(context);

        // MAGIC asdasdasgafg34wqtrfekjrgk
        Todo.instance(context).forEach(todo -> Attr.instance(context).attrib(todo));

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
//                        final Lower lower = Lower.instance(context);
                        final Todo todos = Todo.instance(context);
                        for (Env<AttrContext> todo : todos) {
                            Attr.instance(context).attrib(todo);
//                            Attr.instance(context).postAttr(todo.toplevel);
//                            final com.sun.tools.javac.util.List<JCTree> translate = lower.translateTopLevelClass(todo, todo.toplevel, TreeMaker.instance(context));
                            int a = 0;
                        }
                        loadClassFiles(e);
                        if (!hasOverloadedClassesImported(e.getCompilationUnit())) {
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
                        final CompilationUnitTree compilationUnit = e.getCompilationUnit();
                        compilationUnit.getImports().stream()
                                .map(ImportTree::getQualifiedIdentifier)
                                .map(JCTree.JCFieldAccess.class::cast)
                                .filter(i -> isOverloadedType(i.name))
                                .forEach(i -> nameTypeMap.put(i.name, i.type.tsym.name)); //TODO init list with imports, and figure out how to reach da root elementz
                        compilationUnit.getImports().stream()
                                .filter(i -> !i.isStatic())
                                .map(ImportTree::getQualifiedIdentifier)
                                .map(JCTree.JCFieldAccess.class::cast)
                                .forEach(i -> nameClassMap.put(i.name, i.type.tsym));
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
        //JCVariableDecl--> int i; means that the init part is null
        if (tree == null) return null;

        switch (tree.getClass().getSimpleName()) {
            case "JCClassDecl":
                JCClassDecl classDecl = (JCClassDecl) tree;
                nameTypeMap.put(((JCClassDecl) tree).name.table.names._this, classDecl.name);//TODO is there an easier way to do this?
                classDecl.defs.forEach(BlaPlugin::bla);
                break;
            case "JCVariableDecl":
                JCVariableDecl variableDecl = (JCVariableDecl) tree;
                //names can be reused across different classes, so remove it just in case
                nameTypeMap.remove(variableDecl.getName()); //TODO maybe we should push/pop this just in case some class with a variable 'm' calls an external class with a reused name? is that even possible...
                nameTypeMap.put(variableDecl.getName(), getTypeName(variableDecl));
                variableDecl.init = (JCExpression) bla(variableDecl.init);
                break;
            case "JCMethodDecl":
                JCMethodDecl methodDecl = (JCMethodDecl) tree;
                methodDecl.getParameters()
                        .forEach(p -> nameTypeMap.put(p.getName(), getTypeName(p)));
                methodDecl.body = (JCBlock) bla(methodDecl.body);
                break;
            case "JCBinary": //TODO check opcodes and types
                JCBinary binary = (JCBinary) tree;
                binary.lhs = (JCExpression) bla(binary.lhs);
                binary.rhs = (JCExpression) bla(binary.rhs);
                //TODO wait for the recursive calls to return to know all the return values
                return parseOperatorExpression(binary);
            case "JCBlock":
                JCBlock block = (JCBlock) tree;
                block.stats.forEach(BlaPlugin::bla);
                break;
            case "JCMethodInvocation": //method params can be (a + b)
                JCMethodInvocation methodInvocation = (JCMethodInvocation) tree;
                methodInvocation.meth = (JCExpression) bla(methodInvocation.meth);
                methodInvocation.args = methodInvocation.args.stream()
                        .map(BlaPlugin::bla)
                        .map(JCExpression.class::cast)
                        .collect(com.sun.tools.javac.util.List.collector());
//                final JCExpression meth = methodInvocation.meth;
//                if (meth.type != null)
//                    methodInvocation.type = meth.type;
//
//                if (meth instanceof JCTree.JCFieldAccess) {
//                    final Name selectedName = getExpressionTypeName(((JCTree.JCFieldAccess) meth).selected);
//                    final Symbol clazz;
//                    if (nameClassMap.containsKey(selectedName)) {//static method
//                        clazz = nameClassMap.get(selectedName);
//                    } else {//if (nameTypeMap.containsKey(selectedName)) {
//                        clazz = nameClassMap.get(nameTypeMap.get(selectedName));
//                    }
//                    final Name methodName = ((JCTree.JCFieldAccess) meth).name;
//                    final Symbol[] args = methodInvocation.args.stream()
//                            .map(t -> t.type)
//                            .filter(Objects::nonNull)//TODO this shouldn't happen once all the types are set properly
//                            .map(Type::asElement)
//                            .toArray(Symbol[]::new);
//                    final Symbol.MethodSymbol methodSymbol = getMethodSymbol(clazz, methodName, args);
//                    methodInvocation.type = methodSymbol.getReturnType();
//                }
//                if (methodInvocation.type == null && nameTypeMap.containsKey(methodInvocation.meth().name)) {
//                    final Name name = nameTypeMap.get(methodInvocation.selected.name);
//                    final Symbol symbol = nameClassMap.get(name);
//                    final Symbol.MethodSymbol methodSymbol = getMethodSymbol(symbol, methodInvocation.meth.name, methodInvocation.args);
//                    methodInvocation.type = methodSymbol.getReturnType();
//                }
                break;
            case "JCParens":
                JCParens parens = (JCParens) tree;
                parens.expr = (JCExpression) bla(parens.expr);
                JCExpression expr = parens.expr;
                if (expr instanceof OJCParens ||
                        expr instanceof OJCMethodInvocation ||
                        expr instanceof OJCFieldAccess) {
//                    final Name returnType;
//                    if (expr instanceof OJCParens)
//                        returnType = ((OJCParens) expr).getReturnType();
//                    else if (expr instanceof OJCMethodInvocation)
//                        returnType = ((OJCMethodInvocation) expr).getReturnType();
//                    else if (expr instanceof JCMethodInvocation) {//we need to check the return type
//                        final Symbol.MethodSymbol method = getMethodSymbol((JCMethodInvocation) expr);
//
//                        returnType = method.getReturnType().tsym.getSimpleName();
//                    } else
//                        returnType = nameTypeMap.get(((JCTree.JCIdent) expr).name);

                    return new OJCParens(expr, parens.type);//, nameClassMap.get(returnType).type);//TODO let's just set the JCParens.type field instead
                }
//                if (expr.type != null)
//                    parens.type = expr.type;
                /**
                 * *return value of a method
                 * *overloaded method
                 * *otherwise don't change anything
                 */
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
                return parseOperatorExpression(assignOp);
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

            case "JCFieldAccess":
                JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) tree;
                fieldAccess.selected = (JCExpression) bla(fieldAccess.selected);

//                if (fieldAccess.selected instanceof ReturnTypeBla) {
//                    final Name returnTypeName = ((ReturnTypeBla) fieldAccess.selected).getReturnType();
//                    fieldAccess.type = nameClassMap.get(returnTypeName).type;
//                }
//                if (fieldAccess.type == null) {
//                    final Optional<Symbol.VarSymbol> varSymbol = getVarSymbol(fieldAccess.selected.type.tsym, fieldAccess.name);
//                    varSymbol.ifPresent(symbol -> fieldAccess.type = symbol.type);//methods types should be assigned in JCMethodInvoke
//                }
                //TODO should a fieldaccess propagate upwards towards the methodinvoke?
                /**
                 * if fieldAccess.selected has a returnType
                 * * then create and return a new OJCFieldAccess with the same returnType
                 */
                break;
            case "JCTypeCast":
                JCTree.JCTypeCast typeCast = (JCTree.JCTypeCast) tree;
                typeCast.expr = (JCExpression) bla(typeCast.expr);
//                if (typeCast.clazz instanceof JCTree.JCIdent)//TODO we can cast to null
//                    typeCast.type = nameClassMap.get(((JCTree.JCIdent) typeCast.clazz).getName()).type;
//                if (typeCast.clazz instanceof JCTree.JCPrimitiveTypeTree)
//                    typeCast.type = nameClassMap.get(BlaVerifier.getPrimitiveType((JCTree.JCPrimitiveTypeTree) typeCast.clazz)).type;
                break;
            case "JCIdent":
//                JCTree.JCIdent jcIdent = (JCTree.JCIdent) tree;
//                final Symbol symbol = nameClassMap.get(nameTypeMap.get(jcIdent.getName()));
//                if (symbol != null)
//                    jcIdent.type = symbol.type;
//                else
//                    jcIdent.type = nameClassMap.get(jcIdent.getName()).type;
                break;
            case "JCArrayAccess":
                JCTree.JCArrayAccess arrayAccess = (JCTree.JCArrayAccess) tree;
                arrayAccess.indexed = (JCExpression) bla(arrayAccess.indexed);
                arrayAccess.index = (JCExpression) bla(arrayAccess.index);//TODO should we check this? always int..right?
//                arrayAccess.type = arrayAccess.getExpression().type;
                if (arrayAccess.indexed instanceof JCTree.JCFieldAccess) {
                    final Symbol.TypeSymbol clazz = ((JCTree.JCFieldAccess) arrayAccess.indexed).selected.type.tsym;
                    final Name name = ((JCTree.JCFieldAccess) arrayAccess.indexed).name;
//                    arrayAccess.type = getVarSymbol(clazz, name).get().type;
//                    if (arrayAccess.type instanceof Type.ArrayType) {
//                        arrayAccess.type = getArrayType(arrayAccess.type);
//                    }
                }
                break;
            case "JCLiteral":
            case "JCUnary":
            default:
                return tree;
        }

        return tree;
    }

    private static Symbol.MethodSymbol getMethodSymbol(JCMethodInvocation expr) {
        final JCTree.JCFieldAccess meth = (JCTree.JCFieldAccess) expr.meth;
        final Name methName = meth.name;

        final Name selectedType = nameTypeMap.get(((JCTree.JCIdent) meth.selected).getName());
        final Symbol selectedSymbol = nameClassMap.get(selectedType);

        Name argType = null;
        if (!expr.getArguments().isEmpty()) {//0 arg method invoke
            final JCExpression arg0 = expr.getArguments().get(0);
            if (arg0 instanceof JCTree.JCIdent)
                argType = nameTypeMap.get(((JCTree.JCIdent) arg0).getName());
            else if (arg0 instanceof JCMethodInvocation)
                argType = nameTypeMap.get(getMethodSymbol((JCMethodInvocation) arg0).getReturnType().tsym.getSimpleName());
        }
        final Symbol argSymbol = nameClassMap.get(argType);

        //TODO get method return type from symbol
        return getMethodSymbol(selectedSymbol, methName, argSymbol);
    }

    private static Symbol.MethodSymbol getMethodSymbol(final Symbol clazz, final Name methName, final Symbol... argSymbol) {
        return clazz.getEnclosedElements().stream()//TODO cache results
                .filter(Symbol.MethodSymbol.class::isInstance)
                .map(Symbol.MethodSymbol.class::cast)
                .filter(m -> m.getSimpleName().equals(methName))
                .filter(m -> m.getParameters().size() == argSymbol.length)
                .filter(m -> List.of(argSymbol).containsAll(
                        m.getParameters().stream()
                                .map(Symbol::asType)
                                .map(Type::asElement)
                                .collect(Collectors.toList())))
                .findFirst()
                .orElseThrow();
    }

    private static Optional<Symbol.VarSymbol> getVarSymbol(final Symbol clazz, final Name varName) {
        return clazz.getEnclosedElements().stream()//TODO cache results
                .filter(Symbol.VarSymbol.class::isInstance)
                .map(Symbol.VarSymbol.class::cast)
                .filter(m -> m.getSimpleName().equals(varName))
                .findFirst();
    }

    private static Name getTypeName(JCVariableDecl variableDecl) {
        final JCTree type = variableDecl.getType();
        if (type instanceof JCTree.JCPrimitiveTypeTree) {
            return BlaVerifier.getPrimitiveType((JCTree.JCPrimitiveTypeTree) type);
        }
        if (type instanceof JCTree.JCIdent) {
            final JCTree.JCIdent ident = (JCTree.JCIdent) type;
            if (ident.sym != null)//JCNewClass has no symbology..TODO unlikely, but what if we only have JCNewClass for everything???
                nameClassMap.put(ident.getName(), ident.sym);
            return ident.getName();
        }
        if (type instanceof JCTree.JCArrayTypeTree) {
            return getArrayType((JCTree.JCArrayTypeTree) type);
        }
        return ((JCExpression) type).type.tsym.getSimpleName();
    }

    private static Name getArrayType(JCTree.JCArrayTypeTree type) {
        if (type.elemtype instanceof JCTree.JCArrayTypeTree) {//multi-dimensional arrays
            return getArrayType((JCTree.JCArrayTypeTree) type.elemtype);
        }
        if (type.elemtype instanceof JCTree.JCIdent) {
            return ((JCTree.JCIdent) type.elemtype).name;
        }
        return BlaVerifier.getPrimitiveType((JCTree.JCPrimitiveTypeTree) type.elemtype);
    }

    private static Type getArrayType(Type type) {
        if (type instanceof Type.ArrayType) {
            return getArrayType(((Type.ArrayType) type).elemtype);
        }
        return type;
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
        /*TODO how do we check files in the same package that don't require imports
            since we can't always rely on imports for stuff in same package:
            ((Scope.ScopeImpl) ((JCTree.JCPackageDecl) e.getCompilationUnit().getPackage()).packge.members()).table
         */
        return hasOverloadedImport(compilationUnit) || hasOverloadedPackageMate(compilationUnit);
    }

    private boolean hasOverloadedImport(CompilationUnitTree compilationUnit) {
        return compilationUnit.getImports().stream()
                .map(ImportTree::getQualifiedIdentifier)
                .map(JCTree.JCFieldAccess.class::cast)
                .map(JCTree.JCFieldAccess::getIdentifier)
                .anyMatch(BlaPlugin::isOverloadedType);
    }

    private boolean hasOverloadedPackageMate(CompilationUnitTree compilationUnit) {
        for (Symbol symbol : ((JCTree.JCPackageDecl) compilationUnit.getPackage()).packge.members().getSymbols()) {
            if (BlaPlugin.isOverloadedType(symbol.getSimpleName()))
                return true;
        }
        return false;
    }


    /** this method checks all class files for possible classes with our overloading annotation */
    private void loadClassFiles(TaskEvent e) {
        for (Symbol packge : ((JCTree.JCCompilationUnit) e.getCompilationUnit()).modle.getEnclosedElements()) {
            for (Symbol clazz : ((Symbol.PackageSymbol) packge).members_field.getSymbols()) {
                if (checkedClasses.contains(clazz) || isOverloadedType(clazz.getSimpleName())) continue;

                if (hasOperatorOverloadingAnnotation(clazz)) {
                    //parse and add to some list
                    BlaClassVerifier classVerifier = new BlaClassVerifier((Symbol.ClassSymbol) clazz);
                    overloadedClasses.put(clazz.getSimpleName(), classVerifier.getOverloadedClass());
                }
                checkedClasses.add(clazz);
            }
        }
    }

    private boolean hasOperatorOverloadingAnnotation(Symbol clazz) {
        if (clazz.getMetadata() != null) {
            final com.sun.tools.javac.util.List<Attribute.Compound> annotations = clazz.getMetadata().getDeclarationAttributes();
            return annotations != null && annotations.stream()
                    .map(Attribute.Compound::getAnnotationType)
                    .map(Object::toString)
                    .anyMatch(OperatorOverloading.class.getName()::equals);
        }
        return false;
    }

    private static Tree parseOperatorExpression(JCTree.JCOperatorExpression expression) {
        final JCExpression left = expression.getOperand(JCTree.JCOperatorExpression.OperandPos.LEFT);
        final JCExpression right = expression.getOperand(JCTree.JCOperatorExpression.OperandPos.RIGHT);
        if (left instanceof JCTree.JCIdent) {
            final Name type = getExpressionTypeName(left);
            if (type != null) {
                final BlaVerifier.BlaOverloadedClass overloadedClass = overloadedClasses.get(type);
                if (overloadedClass != null) {
                    final BlaVerifier.BlaOverloadedClass.BlaOverloadedMethod method = overloadedClass.getMethod(expression.getTag(), getExpressionTypeName(right));
                    if (method != null) {
                        // return new method invoke
                        final OJCFieldAccess overriddenMethod = new OJCFieldAccess(left, method.methodName);
                        final Symbol returnSymbol = nameClassMap.get(method.returnType);
                        return new OJCMethodInvocation(null, overriddenMethod, com.sun.tools.javac.util.List.of(right), returnSymbol.type);
                    }
                    // if type has binary.getOperator() && binary.rhs is the correct param type
                }
            }
        } else if (/*left instanceof ReturnTypeBla ||*/ left.type != null) {
            final Name returnType = getExpressionTypeName(left);

            if (overloadedClasses.containsKey(returnType)) {
                final BlaVerifier.BlaOverloadedClass overloadedClass = overloadedClasses.get(returnType);
                final BlaVerifier.BlaOverloadedClass.BlaOverloadedMethod method = overloadedClass.getMethod(expression.getTag(), getExpressionTypeName(right));

                if (method != null) {
                    final OJCFieldAccess overriddenMethod = new OJCFieldAccess(left, method.methodName);
                    final Symbol returnSymbol = nameClassMap.get(method.returnType);
                    return new OJCMethodInvocation(null, overriddenMethod, com.sun.tools.javac.util.List.of(right), returnSymbol.type);
                }
            }
        }

//        if (expression.type == null && left.type instanceof Type.JCPrimitiveType && right.type instanceof Type.JCPrimitiveType) {
//            final TypeTag lt = left.type.getTag();
//            final TypeTag rt = right.type.getTag();
//            if (lt == TypeTag.BYTE || rt == TypeTag.BYTE)
//                expression.type = BlaPlugin.symtab.byteType;
//            if (lt == TypeTag.SHORT || rt == TypeTag.SHORT)
//                expression.type = BlaPlugin.symtab.shortType;
//            if (lt == TypeTag.INT || rt == TypeTag.INT)
//                expression.type = BlaPlugin.symtab.intType;
//            if (lt == TypeTag.LONG || rt == TypeTag.LONG)
//                expression.type = BlaPlugin.symtab.floatType;
//            if (lt == TypeTag.FLOAT || rt == TypeTag.FLOAT)
//                expression.type = BlaPlugin.symtab.floatType;
//            if (lt == TypeTag.DOUBLE || rt == TypeTag.DOUBLE)
//                expression.type = BlaPlugin.symtab.doubleType;
//        }
        return expression;
    }

    private static Name getExpressionTypeName(JCExpression expression) {
        if (expression instanceof JCTree.JCLiteral) {
            if (((JCTree.JCLiteral) expression).typetag == TypeTag.BOT) {
                return symtab.botType.tsym.name;
            }
            return null;
        }
        if (expression instanceof JCTree.JCIdent) {
            final Name name = ((JCTree.JCIdent) expression).getName();
            if (nameTypeMap.containsKey(name)) {
                return nameTypeMap.get(name);
            }
            if (nameClassMap.containsKey(name)) {
                return name;
            }
            return null;
        }
//        if (expression instanceof ReturnTypeBla)
//            return ((ReturnTypeBla) expression).getReturnType();
//        else
        return expression.type.tsym.name;
    }
}
