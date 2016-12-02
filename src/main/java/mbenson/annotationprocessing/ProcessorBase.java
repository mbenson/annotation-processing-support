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

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.annotation.processing.AbstractProcessor;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;

/**
 * Abstract base class to provide further functionality atop {@link AbstractProcessor}.
 */
public abstract class ProcessorBase extends AbstractProcessor {

    private interface MessageWriter {
        void write(Diagnostic.Kind kind, String message, Object... arguments);
    }

    private static ThreadLocal<MessageWriter> MESSAGE_WRITER = new ThreadLocal<MessageWriter>();

    private MessageWriter defaultMessageWriter = new MessageWriter() {

        @Override
        public void write(Kind kind, String message, Object... arguments) {
            processingEnv.getMessager().printMessage(kind, String.format(message, arguments));
        }
    };

    /**
     * Internal process of a single element. Handles message printing based on constructor args.
     * 
     * @param <T> element type
     */
    public abstract class Process<T extends Element> {
        protected final T element;
        protected final AnnotationMirror annotation;
        protected final AnnotationValue value;
        private final MessageWriter messageWriter;

        /**
         * Create a new Process instance.
         * 
         * @param element
         */
        protected Process(final T element) {
            super();
            this.element = Validate.notNull(element);
            this.annotation = null;
            this.value = null;
            this.messageWriter = new MessageWriter() {

                @Override
                public void write(Kind kind, String message, Object... arguments) {
                    processingEnv.getMessager().printMessage(kind, String.format(message, arguments), element);
                }
            };
        }

        /**
         * Create a new Process instance.
         * 
         * @param element
         * @param annotation
         */
        protected Process(final T element, final AnnotationMirror annotation) {
            super();
            this.element = Validate.notNull(element);
            this.annotation = Validate.notNull(annotation);
            this.value = null;
            this.messageWriter = new MessageWriter() {

                @Override
                public void write(Kind kind, String message, Object... arguments) {
                    processingEnv.getMessager().printMessage(kind, String.format(message, arguments), element,
                        annotation);
                }
            };
        }

        /**
         * Create a new Process instance.
         * 
         * @param element
         * @param annotation
         * @param value
         */
        protected Process(final T element, final AnnotationMirror annotation, final AnnotationValue value) {
            super();
            this.element = Validate.notNull(element);
            this.annotation = Validate.notNull(annotation);
            this.value = Validate.notNull(value);
            this.messageWriter = new MessageWriter() {

                @Override
                public void write(Kind kind, String message, Object... arguments) {
                    processingEnv.getMessager().printMessage(kind, String.format(message, arguments), element,
                        annotation, value);
                }
            };
        }

        /**
         * Process.
         */
        public final void process() {
            MessageWriter orig = MESSAGE_WRITER.get();
            MESSAGE_WRITER.set(messageWriter);
            try {
                processImpl();
            } catch (Throwable t) {
                error(t, "Processing error:");
            } finally {
                MESSAGE_WRITER.set(orig);
            }
        }

        /**
         * Process implementation.
         */
        protected abstract void processImpl() throws Throwable;

    }

    /**
     * Convenience method to access {@link Elements}.
     * 
     * @return Elements
     */
    protected Elements elements() {
        return processingEnv.getElementUtils();
    }

    /**
     * Convenience method to access {@link Types}.
     * 
     * @return Types
     */
    protected Types types() {
        return processingEnv.getTypeUtils();
    }

    /**
     * Validate a condition, printing an error if {@code false}.
     * 
     * @param condition
     * @param message
     * @param arguments
     */
    protected void validate(boolean condition, String message, Object... arguments) {
        if (!condition) {
            printMessage(Kind.ERROR, message, arguments);
        }
    }

    /**
     * Generate an error message.
     * 
     * @param cause
     * @param message
     * @param args
     */
    protected void error(Throwable cause, String message, Object... args) {
        StringWriter msg = new StringWriter();
        PrintWriter w = new PrintWriter(msg);
        w.format(message, args);
        w.println();
        if (cause != null) {
            cause.printStackTrace(w);
        }
        printMessage(Kind.ERROR, msg.toString());
    }

    /**
     * Print a message.
     * 
     * @param kind
     * @param message
     * @param arguments
     */
    protected void printMessage(Kind kind, String message, Object... arguments) {
        ObjectUtils.defaultIfNull(MESSAGE_WRITER.get(), defaultMessageWriter).write(kind, message, arguments);
    }
}
