package com.bla;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

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
                if (sourceFile instanceof BlaTest.BlaSimpleJavaFileObject) {
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
                        final String s = blaParser.parseAndReplace();
                        Path tempFile = Files.createTempFile("bla", ".java");
                        Files.writeString(tempFile, s);

                        final BlaTest.BlaSimpleJavaFileObject blaSimpleJavaFileObject = new BlaTest.BlaSimpleJavaFileObject(tempFile.toUri(), JavaFileObject.Kind.SOURCE, s);
                        updateClientFileObject(sourceFile, blaSimpleJavaFileObject);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                Log.instance(context).printRawLines(sourceFile.getName());
            }

            @Override
            public void finished(TaskEvent e) {
                int a = 0;
            }
        });
    }

    private FileObject updateClientFileObject(JavaFileObject sourceFile, SimpleJavaFileObject bla) {
        Field clientFileObject = null;
        try {
            clientFileObject = sourceFile.getClass().getSuperclass().getDeclaredField("clientFileObject");
            clientFileObject.setAccessible(true);
            clientFileObject.set(sourceFile, bla);
            return (FileObject) clientFileObject.get(sourceFile);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (clientFileObject != null)
                clientFileObject.setAccessible(false);
        }
    }
}
