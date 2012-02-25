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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

import org.apache.commons.lang3.reflect.FieldUtils;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JGenerifiable;
import com.sun.codemodel.JMod;

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
         * Translate {@link Modifier}s to a {@link JMod} {@code int}.
         * 
         * @param mods
         * @return int
         */
        public int translateModifiers(Iterable<Modifier> mods) {
            int result = 0;
            for (Modifier mod : mods) {
                try {
                    result |= ((Integer) FieldUtils.readDeclaredStaticField(JMod.class, mod.name())).intValue();
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            return result;
        }

        /**
         * Copy a set of {@link TypeParameterElement}s to a set of target
         * {@link JGenerifiable}s.
         * 
         * @param typeParameters
         * @param targets
         */
        public void copyTo(Iterable<? extends TypeParameterElement> typeParameters, JGenerifiable... targets) {
            for (TypeParameterElement t : typeParameters) {
                List<? extends TypeMirror> bounds = t.getBounds();
                JClass bound = (JClass) CodeModel.naiveType(codeModel, bounds.get(0).toString());
                for (JGenerifiable target : targets) {
                    if (bounds.isEmpty()) {
                        target.generify(t.getSimpleName().toString());
                    } else {
                        target.generify(t.getSimpleName().toString(), bound);
                    }
                }
            }
        }
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
     * Filter {@link Element}s that match a set of modifiers.
     * 
     * @param elements
     * @param modifiers
     * @return Iterable<T>
     */
    public static <T extends Element> Iterable<T> filterByModifier(Iterable<T> elements, Modifier... modifiers) {
        final Set<Modifier> modifierSet;
        if (modifiers != null && modifiers.length > 0) {
            modifierSet = new HashSet<Modifier>();
        } else {
            modifierSet = Collections.emptySet();
        }

        Collections.addAll(modifierSet, modifiers);

        ArrayList<T> result = new ArrayList<T>();
        for (T element : elements) {
            if (element.getModifiers().containsAll(modifierSet)) {
                result.add(element);
            }
        }
        return result;
    }

}
