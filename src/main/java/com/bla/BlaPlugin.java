package com.bla;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

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

        task.addTaskListener(new TaskListener() {
            @Override
            public void started(TaskEvent e) {
                if (e.getKind() != TaskEvent.Kind.PARSE) return;

//                CompilationUnitTree bla = new JCTree.JCCompilationUnit();
                Log.instance(context).printRawLines(e.getSourceFile().getName());
            }

            @Override
            public void finished(TaskEvent e) {

            }
        });
    }
}
