package com.bla;

import com.sun.tools.javac.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BlaTest {
    public static void main(String[] args) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        List<BlaSimpleJavaFileObject> compilationUnits = List.of(new BlaSimpleJavaFileObject("C:\\Users\\mabdelghany\\Desktop\\Bla.java", JavaFileObject.Kind.SOURCE));
        List<String> arguments = List.of("-classpath", "/Temp/jops/target/classes", "-Xplugin:" + BlaPlugin.NAME);
        StringWriter output = new StringWriter();

        JavaCompiler.CompilationTask task = compiler.getTask(output, fileManager, null, arguments, null, compilationUnits);
        task.call();
        
        compiler.run(null, System.out, System.err, "C:\\Users\\mabdelghany\\Desktop\\Bla.java");

        System.out.println("Yahooooo!");
    }

    static class BlaSimpleJavaFileObject extends SimpleJavaFileObject {
        private String fileContent;

        protected BlaSimpleJavaFileObject(final String filename, final Kind kind) {
            super(new File(filename).toURI(), kind);
            try {
                fileContent = new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return fileContent;
        }
    }
}
