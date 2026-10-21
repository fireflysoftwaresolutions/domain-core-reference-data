/*
 * Copyright 2025 Firefly Software Foundation
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

package io.fireflysoftware.domain.referencedata.catalog;

/**
 * Locales the consulting kit defines for a document. Samples and the sandbox catalog use {@code en}.
 */
public enum DocumentLanguage {
    EN("en"),
    ES("es");

    private final String code;

    DocumentLanguage(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static DocumentLanguage fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String trimmed = code.trim();
        for (DocumentLanguage language : values()) {
            if (language.code.equals(trimmed) || language.name().equals(trimmed)) {
                return language;
            }
        }
        return null;
    }
}
