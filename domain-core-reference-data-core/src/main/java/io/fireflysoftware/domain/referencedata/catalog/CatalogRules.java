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

import java.util.Map;
import java.util.regex.Pattern;

/** Closed codes are enums. Currency and country codes stay validated strings. */
public final class CatalogRules {

    private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern COUNTRY = Pattern.compile("^[A-Z]{2,3}$");

    private CatalogRules() {
    }

    public static void require(CatalogEntry draft) {
        if (draft == null || draft.catalog() == null) {
            throw new IllegalArgumentException("Catalog is required");
        }
        if (draft.code() == null || draft.code().isBlank()) {
            throw new IllegalArgumentException("Catalog code is required");
        }
        if (draft.label() == null || draft.label().isBlank()) {
            throw new IllegalArgumentException("Catalog label is required");
        }
        String code = draft.code().trim();
        Map<String, String> attributes = draft.attributes() == null ? Map.of() : draft.attributes();
        switch (draft.catalog()) {
            case CURRENCY -> requireMatch(CURRENCY, code, "Currency code must be three letters");
            case COUNTRY -> {
                requireMatch(COUNTRY, code, "Country code must be two or three letters");
                requireEnum(RegionName.class, attributes.get("region"), "Country region is required");
            }
            case TRANSFER_TYPE -> {
                TransferTypeCode type = requireEnum(TransferTypeCode.class, code, "Transfer type is not in the closed set");
                ExecutionChannel channel = requireEnum(ExecutionChannel.class, attributes.get("executionChannel"),
                        "Transfer execution channel is required");
                requireTransfer(type, channel, attributes);
            }
            case RAIL_SCHEME -> {
                requireEnum(RailSchemeCode.class, code, "Rail scheme is not in the closed set");
                ExecutionChannel channel = requireEnum(ExecutionChannel.class, attributes.get("executionChannel"),
                        "Rail execution channel is required");
                if (channel != ExecutionChannel.LIBRARY_RAILS) {
                    throw new IllegalArgumentException("A rail scheme uses library-rails");
                }
            }
            case ACCOUNT_SPACE_TYPE ->
                    requireEnum(AccountSpaceTypeCode.class, code, "Account space type is not in the closed set");
            case PRODUCT_FAMILY -> {
                requireEnum(ProductFamilyCode.class, code, "Product family is not in the closed set");
                requireEnum(Regulation.class, attributes.get("regulation"), "Product family regulation is required");
            }
            case DOCUMENT_TYPE -> requireDocumentType(code, attributes);
            default -> throw new IllegalArgumentException("Catalog is required");
        }
    }

    private static void requireDocumentType(String code, Map<String, String> attributes) {
        DocumentTypeCode type = DocumentTypeCode.fromCode(code);
        if (type == null) {
            throw new IllegalArgumentException("Document type is not in the closed set");
        }
        DocumentLanguage language = DocumentLanguage.fromCode(attributes.get("language"));
        if (language == null) {
            throw new IllegalArgumentException("Document language is required");
        }
        requireEnum(ProductFamilyCode.class, attributes.get("productFamily"), "Document product family is required");
        String regulated = attributes.get("regulated");
        if (!"true".equals(regulated) && !"false".equals(regulated)) {
            throw new IllegalArgumentException("Document regulated flag must be true or false");
        }
        if (!type.templateCode().equals(attributes.get("templateCode"))) {
            throw new IllegalArgumentException("Document template code must match the catalog code");
        }
        if (!type.kitKind().equals(attributes.get("kitKind"))) {
            throw new IllegalArgumentException("Document kit kind must match the consulting kit");
        }
    }

    private static void requireTransfer(TransferTypeCode type, ExecutionChannel channel, Map<String, String> attributes) {
        ExecutionChannel expected = switch (type) {
            case INTERNAL -> ExecutionChannel.INTERNAL_BOOK;
            case DOMESTIC -> ExecutionChannel.LIBRARY_RAILS;
            case INTERNATIONAL -> ExecutionChannel.LIBRARY_RAILS;
        };
        if (channel != expected) {
            throw new IllegalArgumentException("Transfer type " + type + " uses " + expected);
        }
        if (type == TransferTypeCode.INTERNAL && !"true".equals(attributes.get("accountSpaceMovement"))) {
            throw new IllegalArgumentException("An internal transfer includes movement between account spaces");
        }
    }

    private static void requireMatch(Pattern pattern, String code, String message) {
        if (!pattern.matcher(code).matches()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static <E extends Enum<E>> E requireEnum(Class<E> type, String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    /** Regions accepted by the country master. */
    public enum RegionName {
        EUROPE,
        AMERICAS,
        APAC,
        OCEANIA,
        ASIA
    }
}
