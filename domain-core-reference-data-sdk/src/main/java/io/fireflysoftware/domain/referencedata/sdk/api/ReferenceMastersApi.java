/*
 * Copyright 2026 Firefly Software Foundation
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

package io.fireflysoftware.domain.referencedata.sdk.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fireflysoftware.domain.referencedata.sdk.invoker.ApiClient;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Reads currency and country reference rows from domain-core-reference-data.
 */
public class ReferenceMastersApi {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String basePath;

    public ReferenceMastersApi(ApiClient apiClient) {
        this.webClient = apiClient.getWebClient();
        this.basePath = trim(apiClient.getBasePath());
    }

    public Flux<MasterRow> list(String type) {
        String path = switch (type == null ? "" : type.toUpperCase()) {
            case "CURRENCY" -> "/api/v1/currencies/filter";
            case "COUNTRY" -> "/api/v1/countries/filter";
            default -> null;
        };
        if (path == null) {
            return Flux.error(new IllegalStateException("MASTER_TYPE_UNKNOWN"));
        }
        String masterType = type.toUpperCase();
        return webClient.post()
                .uri(basePath + path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .retrieve()
                .bodyToMono(String.class)
                .defaultIfEmpty("{}")
                .flatMapMany(raw -> Flux.fromIterable(items(tree(raw))))
                .map(item -> toRow(masterType, item));
    }

    /**
     * Reads one catalog row from {@code GET /api/v1/catalogs/{catalog}/{code}}.
     */
    public Mono<CatalogRow> findCatalog(String catalog, String code) {
        return webClient.get()
                .uri(basePath + "/api/v1/catalogs/{catalog}/{code}", catalog, code)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .map(raw -> toCatalog(code, tree(raw)));
    }

    public record MasterRow(String type, UUID id, String code, String name) {
    }

    public record CatalogRow(String code, Map<String, String> attributes) {
    }

    private static CatalogRow toCatalog(String requestedCode, JsonNode body) {
        Map<String, String> attributes = new java.util.LinkedHashMap<>();
        JsonNode source = body.path("attributes");
        if (source.isObject()) {
            source.fields().forEachRemaining(entry -> attributes.put(entry.getKey(), entry.getValue().asText("")));
        }
        String code = firstText(body, "code");
        return new CatalogRow(code.isBlank() ? requestedCode : code, Map.copyOf(attributes));
    }

    private static MasterRow toRow(String type, JsonNode item) {
        String code = firstText(item, "currencyCode", "countryCode", "isoCode", "code", "alpha2Code");
        String name = firstText(item, "name", "currencyName", "countryName");
        String idText = firstText(item, "id", "currencyId", "countryId");
        UUID id = idText.isBlank()
                ? UUID.nameUUIDFromBytes((type + ":" + code).getBytes(StandardCharsets.UTF_8))
                : UUID.fromString(idText);
        return new MasterRow(type, id, code, name);
    }

    private static Iterable<JsonNode> items(JsonNode body) {
        if (body == null || body.isMissingNode() || body.isNull()) {
            return java.util.List.of();
        }
        if (body.isArray()) {
            return body;
        }
        for (String field : new String[]{"content", "items", "data", "results"}) {
            JsonNode node = body.path(field);
            if (node.isArray()) {
                return node;
            }
        }
        return java.util.List.of();
    }

    private static String firstText(JsonNode item, String... names) {
        for (String name : names) {
            String value = item.path(name).asText("");
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static JsonNode tree(String raw) {
        if (raw == null || raw.isBlank()) {
            return MAPPER.createObjectNode();
        }
        try {
            return MAPPER.readTree(raw);
        } catch (Exception exception) {
            throw new IllegalStateException("DOMAIN_RESPONSE_UNREADABLE");
        }
    }

    private static String trim(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("PROPERTY_NOT_SET");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
