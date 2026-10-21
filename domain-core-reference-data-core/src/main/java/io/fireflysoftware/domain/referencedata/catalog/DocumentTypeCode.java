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
 * Communication types defined by the Firefly consulting kit.
 * {@code code} is the template code a journey passes when it issues the document.
 * The kit has no notice document, so notice is not a catalog code.
 */
public enum DocumentTypeCode {
    REPORT("report", "report", "Report"),
    PROPOSAL("proposal", "proposal", "Proposal"),
    STATEMENT_OF_WORK("sow", "sow", "Statement of work"),
    MEMO("memo", "memo", "Executive memo"),
    MINUTES("minutes", "minutes", "Meeting minutes"),
    ONE_PAGER("one-pager", "one-pager", "One-pager"),
    LETTER("letter", "letter", "Letter"),
    WELCOME("welcome", "welcome", "Welcome");

    private final String code;
    private final String kitKind;
    private final String label;

    DocumentTypeCode(String code, String kitKind, String label) {
        this.code = code;
        this.kitKind = kitKind;
        this.label = label;
    }

    /** Catalog code and FreeMarker template name. */
    public String code() {
        return code;
    }

    public String templateCode() {
        return code;
    }

    /** Consulting-kit kind whose structure the template follows. */
    public String kitKind() {
        return kitKind;
    }

    public String label() {
        return label;
    }

    public static DocumentTypeCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String trimmed = code.trim();
        for (DocumentTypeCode type : values()) {
            if (type.code.equals(trimmed)) {
                return type;
            }
        }
        return null;
    }
}
