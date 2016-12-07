/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mbenson.annotationprocessing;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import org.apache.commons.lang3.StringUtils;

import com.helger.jcodemodel.AbstractCodeWriter;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JPackage;
import com.helger.jcodemodel.writer.PrologCodeWriter;

import mbenson.annotationprocessing.util.CodeModel;

/**
 * {@link ProcessorBase} that primarily uses a {@link JCodeModel}.
 */
public abstract class CodeModelProcessorBase extends ProcessorBase {
    /**
     * Java source file extension.
     */
    private static final String JAVA_FILE_EXTENSION = ".java";

    public abstract class CodeModelProcess<T extends Element> extends Process<T> {
        protected final JCodeModel codeModel;

        protected CodeModelProcess(T element, AnnotationMirror annotation, AnnotationValue value,
            JCodeModel codeModel) {
            super(element, annotation, value);
            this.codeModel = codeModel;
        }

        protected CodeModelProcess(T element, AnnotationMirror annotation, JCodeModel codeModel) {
            super(element, annotation);
            this.codeModel = codeModel;
        }

        protected CodeModelProcess(T element, JCodeModel codeModel) {
            super(element);
            this.codeModel = codeModel;
        }

        protected <TYPE extends AbstractJType> TYPE naiveType(String name) {
            return CodeModel.naiveType(codeModel, name);
        }
    }

    @Override
    public final boolean process(final Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final JCodeModel codeModel = new JCodeModel();
        boolean result;
        try {
            result = processTo(codeModel, annotations, roundEnv);
        } catch (Throwable t) {
            error(t, "Error creating code model");
            result = false;
        }
        if (result) {
            try {
                final AbstractCodeWriter codeWriter = new AbstractCodeWriter(Charset.forName("UTF-8"), System.getProperty("line.separator")) {

                    @Override
                    public OutputStream openBinary(JPackage pkg, String fileName) throws IOException {
                        final JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(
                            StringUtils.join(pkg.name(), ".", StringUtils.removeEnd(fileName, JAVA_FILE_EXTENSION)),
                            annotations.toArray(new TypeElement[annotations.size()]));
                        return sourceFile.openOutputStream();
                    }

                    @Override
                    public void close() throws IOException {
                    }
                };
                codeModel
                    .build(new PrologCodeWriter(codeWriter, String.format("generated by %s\n", getClass().getName())));
            } catch (IOException e) {
                error(e, "Error generating code");
            }
        }
        return result;
    }

    /**
     * Process into a {@link JCodeModel}.
     * 
     * @param codeModel
     * @param annotations
     * @param roundEnv
     * @return whether the annotations were claimed
     */
    protected abstract boolean processTo(JCodeModel codeModel, Set<? extends TypeElement> annotations,
        RoundEnvironment roundEnv) throws Throwable;
}
