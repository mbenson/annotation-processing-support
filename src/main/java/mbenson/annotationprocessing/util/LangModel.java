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
package mbenson.annotationprocessing.util;

import static java.lang.reflect.Modifier.ABSTRACT;
import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.NATIVE;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PROTECTED;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;
import static java.lang.reflect.Modifier.STRICT;
import static java.lang.reflect.Modifier.SYNCHRONIZED;
import static java.lang.reflect.Modifier.TRANSIENT;
import static java.lang.reflect.Modifier.VOLATILE;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.IJGenerifiable;
import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JTypeVar;

/**
 * Hosts utility methods for working with {@code javax.lang.model}.
 */
public class LangModel {

    /**
     * Utility methods for working with {@link JCodeModel}.
     */
    public static class ToCodeModel {

        private final JCodeModel codeModel;

        private ToCodeModel(JCodeModel codeModel) {
            this.codeModel = codeModel;
        }

        /**
         * Copy a set of {@link TypeParameterElement}s to a set of target {@link JGenerifiable}s.
         * 
         * @param typeParameters
         * @param targets
         */
        public void copyTo(Iterable<? extends TypeParameterElement> typeParameters, IJGenerifiable... targets) {
            for (TypeParameterElement t : typeParameters) {

                final Set<AbstractJClass> bounds =
                    t.getBounds().stream().allMatch(b -> Object.class.getName().equals(b.toString()))
                        ? Collections.emptySet()
                        : t.getBounds().stream().<AbstractJClass> map(b -> CodeModel.naiveType(codeModel, b.toString()))
                            .collect(Collectors.toCollection(LinkedHashSet::new));

                for (IJGenerifiable target : targets) {
                    final JTypeVar typeVar = target.generify(t.getSimpleName().toString());
                    bounds.forEach(typeVar::bound);
                }
            }
        }

    }

    private static final Map<Modifier, Integer> MODIFIER_CONSTANTS;
    static {
        final Map<Modifier, Integer> m = new EnumMap<>(Modifier.class);
        m.put(Modifier.PUBLIC, PUBLIC);
        m.put(Modifier.PRIVATE, PRIVATE);
        m.put(Modifier.PROTECTED, PROTECTED);
        m.put(Modifier.STATIC, STATIC);
        m.put(Modifier.FINAL, FINAL);
        m.put(Modifier.SYNCHRONIZED, SYNCHRONIZED);
        m.put(Modifier.VOLATILE, VOLATILE);
        m.put(Modifier.TRANSIENT, TRANSIENT);
        m.put(Modifier.NATIVE, NATIVE);
        m.put(Modifier.ABSTRACT, ABSTRACT);
        m.put(Modifier.STRICTFP, STRICT);
        MODIFIER_CONSTANTS = Collections.unmodifiableMap(m);
    }

    /**
     * Get a {@link ToCodeModel} instance for the specified {@link JCodeModel}.
     * 
     * @param codeModel
     * @return {@link ToCodeModel}
     */
    public static ToCodeModel to(JCodeModel codeModel) {
        return new ToCodeModel(codeModel);
    }

    /**
     * Filter {@link Element}s that match a set of modifiers. Returns elements with all specified modifiers.
     * 
     * @param elements
     * @param modifiers to match
     * @return Iterable<T>
     */
    public static <T extends Element> Iterable<T> filterByModifier(Iterable<T> elements, Modifier... modifiers) {
        return filterByModifiers(elements, ModifierMatch.ALL, modifiers);
    }

    /**
     * Filter {@link Element}s that exactly match a set of modifiers. Returns elements with only the specified
     * modifiers.
     * 
     * @param elements
     * @param modifiers to match
     * @return Iterable<T>
     */
    public static <T extends Element> Iterable<T> filterByExactModifiers(Iterable<T> elements, Modifier... modifiers) {
        return filterByModifiers(elements, ModifierMatch.EXACT, modifiers);
    }

    private enum ModifierMatch {
                                ALL {
                                    @Override
                                    protected Predicate<Element> filter(Set<Modifier> modifiers) {
                                        return e -> e.getModifiers().containsAll(modifiers);
                                    }
                                },
                                EXACT {
                                    @Override
                                    protected Predicate<Element> filter(Set<Modifier> modifiers) {
                                        return e -> e.getModifiers().equals(modifiers);
                                    }
                                };

        protected abstract Predicate<Element> filter(Set<Modifier> modifiers);
    }

    private static <T extends Element> Iterable<T> filterByModifiers(Iterable<T> elements, ModifierMatch behavior,
        Modifier... modifiers) {
        final Set<Modifier> modifierSet =
            modifiers == null ? Collections.emptySet() : Stream.of(modifiers).collect(Collectors.toSet());

        final Stream.Builder<T> bld = Stream.builder();
        elements.iterator().forEachRemaining(bld);
        return bld.build().filter(behavior.filter(modifierSet)).collect(Collectors.toList());
    }

    /**
     * Encode the specified set of {@link Modifier}s to an {@code int}.
     * 
     * @param modifiers
     * @return OR'ed int
     */
    public static int encodeModifiers(Modifier... modifiers) {
        return encodeModifiers(Arrays.asList(modifiers));
    }

    /**
     * Encode the specified set of {@link Modifier}s to an {@code int}.
     * 
     * @param modifiers
     * @return OR'ed int
     */
    public static int encodeModifiers(Iterable<Modifier> modifiers) {
        Stream<Modifier> stream;
        if (modifiers instanceof Collection<?>) {
            stream = ((Collection<Modifier>) modifiers).stream();
        } else {
            final Stream.Builder<Modifier> bld = Stream.builder();
            modifiers.iterator().forEachRemaining(bld);
            stream = bld.build();
        }
        return stream.mapToInt(MODIFIER_CONSTANTS::get).reduce(0, (l, r) -> l | r);
    }
}
