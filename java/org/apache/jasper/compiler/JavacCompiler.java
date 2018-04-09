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

import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.jasper.JasperException;

/**
 * Class compiler using the system Java compiler (only available with the full
 * JDK or the jdk.compiler module).
 */
public class JavacCompiler extends JSR199Compiler {

    protected JavaCompiler getCompiler() throws JasperException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new JasperException(Localizer.getMessage("jsp.error.system_compiler_not_found"));
        }

        return compiler;
    }

    protected List<String> getOptions() {
        List<String> options = new ArrayList<>();

        if (ctxt.getOptions().getClassDebugInfo()) {
            options.add("-g");
        } else {
            options.add("-g:none");
        }

        // Source JVM
        options.add("-source");
        if (ctxt.getOptions().getCompilerSourceVM() != null) {
            options.add(ctxt.getOptions().getCompilerSourceVM());
        } else {
            // Default to 1.8
            options.add("1.8");
        }

        // Target JVM
        options.add("-target");
        if (ctxt.getOptions().getCompilerTargetVM() != null) {
            options.add(ctxt.getOptions().getCompilerTargetVM());
        } else {
            // Default to 1.8
            options.add("1.8");
        }

        return options;
    }
}
