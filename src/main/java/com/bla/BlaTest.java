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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BlaTest {
    public static void main(String[] args) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//        SimpleFileManager fileManager = new SimpleFileManager(compiler.getStandardFileManager(null, null, null));
//        List<BlaSimpleJavaFileObject> compilationUnits = List.of(new BlaSimpleJavaFileObject("C:\\Users\\mabdelghany\\Desktop\\Bla.java", JavaFileObject.Kind.SOURCE));
//        List<String> arguments = List.of("-classpath", "/Temp/jops/target/classes", "-Xplugin:" + BlaPlugin.NAME);
//        StringWriter output = new StringWriter();

//        JavaCompiler.CompilationTask task = compiler.getTask(output, fileManager, null, arguments, null, compilationUnits);
//        task.call();
//        fileManager.getCompiled().iterator().next().getCompiledBinaries();

        compiler.run(null, System.out, System.err, "-classpath", "/Temp/jops/target/classes", "-Xplugin:" + BlaPlugin.NAME, "C:\\Users\\mabdelghany\\Desktop\\Bla.java");

//        BlaParser bla = new BlaParser(Files.readString(Paths.get("C:\\Users\\mabdelghany\\Desktop\\Bla.java")));
//        String parse = bla.parseAndReplace();
        System.out.println("Yahooooo!");
    }

    static class BlaSimpleJavaFileObject extends SimpleJavaFileObject {
        private String fileContent;

        protected BlaSimpleJavaFileObject(final String filename, final Kind kind) {
            super(new File(filename).toURI(), kind);
            try {
                fileContent = Files.readString(Paths.get(filename));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return fileContent;
        }
    }

    static class BlaSimpleClassFile extends SimpleJavaFileObject {
        private ByteArrayOutputStream out;

        protected BlaSimpleClassFile(URI uri, Kind kind) {
            super(uri, kind);
        }

        @Override
        public OutputStream openOutputStream() {
            return out = new ByteArrayOutputStream();
        }

        public byte[] getCompiledBinaries() {
            return out.toByteArray();
        }
    }

    static class SimpleFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

        private final List<BlaSimpleClassFile> compiled = new ArrayList<>();

        public SimpleFileManager(StandardJavaFileManager delegate) {
            super(delegate);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            final BlaSimpleClassFile result = new BlaSimpleClassFile(URI.create("string://" + className), JavaFileObject.Kind.CLASS);
            compiled.add(result);
            return result;
        }

        /**
         * @return compiled binaries processed by the current class
         */
        public List<BlaSimpleClassFile> getCompiled() {
            return compiled;
        }
    }
}
