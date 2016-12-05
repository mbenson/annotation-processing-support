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
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

/**
 * Abstract base class to provide further functionality atop {@link AbstractProcessor}.
 */
public abstract class ProcessorBase extends AbstractProcessor {

    private interface MessageWriter {
        void write(Diagnostic.Kind kind, String message, Object... arguments);
    }

    private static ThreadLocal<MessageWriter> MESSAGE_WRITER = new ThreadLocal<>();

    private MessageWriter defaultMessageWriter =
        (kind, message, arguments) -> processingEnv.getMessager().printMessage(kind, String.format(message, arguments));

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
            this.element = Objects.requireNonNull(element, "element");
            this.annotation = null;
            this.value = null;
            this.messageWriter = (kind, message, arguments) -> processingEnv.getMessager().printMessage(kind,
                String.format(message, arguments), element);
        }

        /**
         * Create a new Process instance.
         * 
         * @param element
         * @param annotation
         */
        protected Process(final T element, final AnnotationMirror annotation) {
            super();
            this.element = Objects.requireNonNull(element, "element");
            this.annotation = Objects.requireNonNull(annotation, "annotation");
            this.value = null;
            this.messageWriter = (kind, message, arguments) -> processingEnv.getMessager().printMessage(kind,
                String.format(message, arguments), element, annotation);
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
            this.element = Objects.requireNonNull(element, "element");
            this.annotation = Objects.requireNonNull(annotation, "annotation");
            this.value = Objects.requireNonNull(value, "value");
            this.messageWriter = (kind, message, arguments) -> processingEnv.getMessager().printMessage(kind,
                String.format(message, arguments), element, annotation, value);
        }

        /**
         * Process.
         */
        public final void process() {
            final MessageWriter orig = MESSAGE_WRITER.get();
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
        final StringWriter msg = new StringWriter();
        final PrintWriter w = new PrintWriter(msg);
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
        Optional.ofNullable(MESSAGE_WRITER.get()).orElse(defaultMessageWriter).write(kind, message, arguments);
    }

    /**
     * Handle {@link MirroredTypeException} reading a class-valued annotation attribute from a language {@link Element}.
     * 
     * @param a
     * @param classAttribute
     * @return TypeMirror
     */
    protected <A extends Annotation> TypeMirror getTypeMirror(A a, Function<A, Class<?>> classAttribute) {
        try {
            return Optional.of(classAttribute.apply(a)).map(this::mirror).get();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }

    /**
     * Handle {@link MirroredTypesException} reading a class array-valued annotation attribute from a language
     * {@link Element}.
     * 
     * @param a
     * @param classArrayAttribute
     * @return {@link List} of {@link TypeMirror}
     */
    protected <A extends Annotation> List<? extends TypeMirror> getTypeMirrors(A a,
        Function<A, Class<?>[]> classArrayAttribute) {
        try {
            return Stream.of(classArrayAttribute.apply(a)).map(this::mirror).collect(Collectors.toList());
        } catch (MirroredTypesException e) {
            return e.getTypeMirrors();
        }
    }

    /**
     * Handle {@link MirroredTypeException} reading a class-valued annotation attribute from a language {@link Element}.
     * 
     * @param a
     * @param classAttribute
     * @return String
     */
    protected <A extends Annotation> String getClassName(A a, Function<A, Class<?>> classAttribute) {
        try {
            return classAttribute.apply(a).getName();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    /**
     * Handle {@link MirroredTypesException} reading a class array-valued annotation attribute from a language
     * {@link Element}.
     * 
     * @param a
     * @param classArrayAttribute
     * @return {@link List} of {@link String}
     */
    protected <A extends Annotation> List<String> getClassNames(A a, Function<A, Class<?>[]> classAttribute) {
        Stream<String> stream;
        try {
            stream = Stream.of(classAttribute.apply(a)).map(Class::getName);
        } catch (MirroredTypesException e) {
            stream = Stream.of(e.getTypeMirrors()).map(Object::toString);
        }
        return stream.collect(Collectors.toList());
    }

    private TypeMirror mirror(Class<?> cls) {
        return types().getDeclaredType(elements().getTypeElement(cls.getName()));
    }
}
