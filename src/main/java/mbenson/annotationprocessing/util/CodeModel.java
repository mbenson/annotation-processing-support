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

import org.apache.commons.lang3.SystemUtils;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JCommentPart;
import com.sun.codemodel.JType;

/**
 * Hosts utility methods for working with {@link JCodeModel}.
 */
public class CodeModel {

    /**
     * Fluently add a child block to {@code parent}.
     * 
     * @param parent
     * @param child
     * @return child
     */
    public static JBlock addTo(JBlock parent, JBlock child) {
        parent.add(child);
        return child;
    }

    /**
     * Naively parse type; works for type variables as well.
     * 
     * @param codeModel
     * @param name
     * @return JType named
     */
    // TODO provide a more direct way to create a type variable JType
    public static <T extends JType> T naiveType(JCodeModel codeModel, String name) {
        T result;
        try {
            @SuppressWarnings({ "unchecked", "unused" })
            T knownType = result = (T) codeModel.parseType(name);
        } catch (ClassNotFoundException e) {
            @SuppressWarnings({ "unused", "unchecked" })
            T naiveType = result = (T) codeModel.directClass(name);
        }
        return result;
    }

    /**
     * Add (potentially multiline) {@code commentary} to {@code commentPart}.
     * 
     * @param commentPart
     * @param commentary
     */
    public static void addTo(JCommentPart commentPart, String... commentary) {
        if (commentary != null) {
            for (String s : commentary) {
                commentPart.add(s);
                commentPart.add(SystemUtils.LINE_SEPARATOR);
            }
        }
    }
}
