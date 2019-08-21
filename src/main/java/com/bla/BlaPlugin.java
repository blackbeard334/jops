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
import java.nio.file.Paths;

import static com.bla.BlaTest.BlaSimpleJavaFileObject;

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
                int a = 0;
            }
        });
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
