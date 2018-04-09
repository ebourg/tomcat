/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jasper.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import org.apache.jasper.JasperException;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Class compiler using the standard {@link JavaCompiler} API (JSR 199).
 *
 * @author Emmanuel Bourg
 */
public abstract class JSR199Compiler extends Compiler {

    private final Log log = LogFactory.getLog(JSR199Compiler.class); // must not be static

    @Override
    protected void generateClass(Map<String, SmapStratum> smaps) throws FileNotFoundException, JasperException, Exception {
        
        long t1 = 0;
        if (log.isDebugEnabled()) {
            t1 = System.currentTimeMillis();
        }

        JavaCompiler compiler = getCompiler();

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        Charset charset = Charset.forName(ctxt.getOptions().getJavaEncoding());
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, charset)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(ctxt.getOptions().getScratchDir()));
            fileManager.setLocation(StandardLocation.CLASS_PATH, getClassPath());

            List<File> sourceFiles = Collections.singletonList(new File(ctxt.getServletJavaFileName()));
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
            compiler.getTask(null, fileManager, diagnostics, getOptions(), null, compilationUnits).call();
        }

        if (!ctxt.keepGenerated()) {
            File javaFile = new File(ctxt.getServletJavaFileName());
            if (!javaFile.delete()) {
                throw new JasperException(Localizer.getMessage("jsp.warning.compiler.javafile.delete.fail", javaFile));
            }
        }

        List<JavacErrorDetail> problemList = toJavacErrors(diagnostics);
        if (!problemList.isEmpty()) {
            JavacErrorDetail[] jeds = problemList.toArray(new JavacErrorDetail[problemList.size()]);
            errDispatcher.javacError(jeds);
        }

        if (log.isDebugEnabled()) {
            long t2 = System.currentTimeMillis();
            log.debug("Compiled " + ctxt.getServletJavaFileName() + " " + (t2 - t1) + "ms");
        }

        if (ctxt.isPrototypeMode()) {
            return;
        }

        // JSR45 Support
        if (!this.options.isSmapSuppressed()) {
            SmapUtil.installSmap(smaps);
        }
    }

    /**
     * Returns the compiler implementation.
     */
    protected abstract JavaCompiler getCompiler() throws JasperException;

    /**
     * Returns the compiler options.
     */
    protected abstract List<String> getOptions();

    /**
     * Returns the jar files and directories used for the JSP compilation.
     */
    private List<File> getClassPath() {
        List<File> files = new ArrayList<File>();
        StringTokenizer tokenizer = new StringTokenizer(ctxt.getClassPath(), File.pathSeparator);
        while (tokenizer.hasMoreElements()) {
            files.add(new File(tokenizer.nextToken()));
        }

        return files;
    }

    /**
     * Converts the diagnostic messages into Jasper compilation errors.
     */
    private List<JavacErrorDetail> toJavacErrors(DiagnosticCollector<JavaFileObject> diagnostics) {
        List<JavacErrorDetail> problemList = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                String name = diagnostic.getSource().getName();
                try {
                    problemList.add(ErrorDispatcher.createJavacError(name, pageNodes,
                                    new StringBuilder(diagnostic.getMessage(null)),
                                    (int) diagnostic.getLineNumber(), ctxt));
                } catch (JasperException e) {
                    log.error("Error visiting node", e);
                }
            }
        }
        return problemList;
    }
}
