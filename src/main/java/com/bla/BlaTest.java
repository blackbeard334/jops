package com.bla;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BlaTest {
    public static void main(String[] args) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        SimpleFileManager fileManager = new SimpleFileManager(compiler.getStandardFileManager(null, null, null));
        List<BlaSimpleJavaFileObject> compilationUnits = List.of(new BlaSimpleJavaFileObject("C:\\Users\\mabdelghany\\Desktop\\Bla.java", JavaFileObject.Kind.SOURCE));
        List<String> arguments = List.of("-classpath", "/Temp/jops/target/classes", "-Xplugin:" + BlaPlugin.NAME);
        StringWriter output = new StringWriter();

        JavaCompiler.CompilationTask task = compiler.getTask(output, fileManager, null, arguments, null, compilationUnits);
        task.call();

        compiler.run(null, System.out, System.err, "C:\\Users\\mabdelghany\\Desktop\\Bla.java");

        BlaParser bla = new BlaParser(Files.readString(Paths.get("C:\\Users\\mabdelghany\\Desktop\\Bla.java")));
        String parse = bla.parseAndReplace();
        System.out.println("Yahooooo!");
    }

    static class BlaSimpleJavaFileObject extends SimpleJavaFileObject {
        private String                fileContent;
        private ByteArrayOutputStream out;

        protected BlaSimpleJavaFileObject(final String filename, final Kind kind) {
            super(new File(filename).toURI(), kind);
            try {
                fileContent = new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public BlaSimpleJavaFileObject(URI uri, Kind kind, String fileContent) {
            super(uri, kind);
            this.fileContent = fileContent;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return fileContent;
        }

        public void setFileContent(String fileContent) {
            this.fileContent = fileContent;
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return out = new ByteArrayOutputStream();
        }

        public byte[] getCompiledBinaries() {
            return out.toByteArray();
        }
    }


    static class SimpleFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

        private final List<BlaSimpleJavaFileObject> compiled = new ArrayList<>();

        public SimpleFileManager(StandardJavaFileManager delegate) {
            super(delegate);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            BlaSimpleJavaFileObject result = new BlaSimpleJavaFileObject(URI.create("string://" + className).toString(), JavaFileObject.Kind.CLASS);
            compiled.add(result);
            return result;
        }

        /**
         * @return compiled binaries processed by the current class
         */
        public List<BlaSimpleJavaFileObject> getCompiled() {
            return compiled;
        }
    }
}
