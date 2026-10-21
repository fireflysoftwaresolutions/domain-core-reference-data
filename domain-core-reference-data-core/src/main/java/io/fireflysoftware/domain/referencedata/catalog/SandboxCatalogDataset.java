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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Deterministic catalog used when no reference-data microservice is running. */
public final class SandboxCatalogDataset {

    private SandboxCatalogDataset() {
    }

    public static List<CatalogEntry> entries() {
        List<CatalogEntry> rows = new ArrayList<>();
        currency(rows, "EUR", "Euro", "€", "2");
        currency(rows, "USD", "US Dollar", "$", "2");
        currency(rows, "GBP", "Pound Sterling", "£", "2");
        currency(rows, "CHF", "Swiss Franc", "CHF", "2");
        currency(rows, "JPY", "Yen", "¥", "0");
        country(rows, "ES", "Spain", "EUROPE");
        country(rows, "US", "United States", "AMERICAS");
        country(rows, "GB", "United Kingdom", "EUROPE");
        country(rows, "DE", "Germany", "EUROPE");
        country(rows, "FR", "France", "EUROPE");
        country(rows, "PT", "Portugal", "EUROPE");
        country(rows, "MX", "Mexico", "AMERICAS");
        country(rows, "BR", "Brazil", "AMERICAS");
        country(rows, "CH", "Switzerland", "EUROPE");
        country(rows, "JP", "Japan", "APAC");
        transfer(rows, TransferTypeCode.INTERNAL, ExecutionChannel.INTERNAL_BOOK, "INTERNAL", "INTERNAL",
                "Internal book transfer (traspaso)", true);
        transfer(rows, TransferTypeCode.DOMESTIC, ExecutionChannel.LIBRARY_RAILS, "OUTBOUND_RAIL", "INBOUND_RAIL",
                "Domestic transfer", false);
        transfer(rows, TransferTypeCode.INTERNATIONAL, ExecutionChannel.LIBRARY_RAILS, "OUTBOUND_RAIL", "INBOUND_RAIL",
                "International credit transfer", false);
        for (RailSchemeCode scheme : RailSchemeCode.values()) {
            rows.add(entry(CatalogCode.RAIL_SCHEME, scheme.name(), scheme.name().replace('_', ' '),
                    Map.of("executionChannel", ExecutionChannel.LIBRARY_RAILS.name())));
        }
        for (AccountSpaceTypeCode space : AccountSpaceTypeCode.values()) {
            rows.add(entry(CatalogCode.ACCOUNT_SPACE_TYPE, space.name(), space.name(), Map.of()));
        }
        family(rows, ProductFamilyCode.DEPOSITS, Regulation.REGULATED, "BANKING");
        family(rows, ProductFamilyCode.PAYMENTS, Regulation.REGULATED, "BANKING");
        family(rows, ProductFamilyCode.LENDING, Regulation.REGULATED, "BANKING");
        family(rows, ProductFamilyCode.CARDS, Regulation.REGULATED, "BANKING");
        family(rows, ProductFamilyCode.INSURANCE, Regulation.NON_REGULATED, "INSURANCE");
        for (DocumentTypeCode type : DocumentTypeCode.values()) {
            Map<String, String> attributes = new LinkedHashMap<>();
            attributes.put("language", DocumentLanguage.EN.code());
            attributes.put("productFamily", ProductFamilyCode.LENDING.name());
            attributes.put("regulated", "true");
            attributes.put("templateCode", type.templateCode());
            attributes.put("kitKind", type.kitKind());
            rows.add(entry(CatalogCode.DOCUMENT_TYPE, type.code(), type.label(), attributes));
        }
        rows.forEach(CatalogRules::require);
        return List.copyOf(rows);
    }

    private static void currency(List<CatalogEntry> rows, String code, String name, String symbol, String scale) {
        rows.add(entry(CatalogCode.CURRENCY, code, name, Map.of("symbol", symbol, "decimalPrecision", scale)));
    }

    private static void country(List<CatalogEntry> rows, String code, String name, String region) {
        rows.add(entry(CatalogCode.COUNTRY, code, name, Map.of("region", region)));
    }

    private static void transfer(List<CatalogEntry> rows, TransferTypeCode type, ExecutionChannel channel,
                                 String outbound, String inbound, String label, boolean spaces) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("executionChannel", channel.name());
        attributes.put("outboundRoute", outbound);
        attributes.put("inboundRoute", inbound);
        attributes.put("bookTransfer", Boolean.toString(channel == ExecutionChannel.INTERNAL_BOOK));
        attributes.put("accountSpaceMovement", Boolean.toString(spaces));
        rows.add(entry(CatalogCode.TRANSFER_TYPE, type.name(), label, attributes));
    }

    private static void family(List<CatalogEntry> rows, ProductFamilyCode code, Regulation regulation, String industry) {
        rows.add(entry(CatalogCode.PRODUCT_FAMILY, code.name(), code.name(),
                Map.of("regulation", regulation.name(), "industry", industry)));
    }

    private static CatalogEntry entry(CatalogCode catalog, String code, String label, Map<String, String> attributes) {
        return new CatalogEntry(id(catalog, code), catalog, code, label, CatalogStatus.ACTIVE, Map.copyOf(attributes));
    }

    static UUID id(CatalogCode catalog, String code) {
        return UUID.nameUUIDFromBytes((catalog.name() + ":" + code).getBytes(StandardCharsets.UTF_8));
    }
}
