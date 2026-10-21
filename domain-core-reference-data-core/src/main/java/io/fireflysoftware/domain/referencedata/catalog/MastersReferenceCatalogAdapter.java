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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter an installer selects in place of {@link SandboxReferenceCatalogAdapter}.
 * It calls core-common-reference-master-data. Currencies stay three-letter codes.
 * Lookup catalogs are transfer types, rail schemes, account space types, product families,
 * and document types.
 */
public final class MastersReferenceCatalogAdapter implements ReferenceCatalogPort {

    static final String CURRENCIES = "/api/v1/currencies";
    static final String COUNTRIES = "/api/v1/countries";
    static final String DOMAINS = "/api/v1/lookup/domains";
    static final String ITEMS = "/api/v1/lookup/items";

    private final WebClient webClient;
    private final ObjectMapper mapper;

    public MastersReferenceCatalogAdapter(WebClient webClient) {
        this(webClient, new ObjectMapper());
    }

    MastersReferenceCatalogAdapter(WebClient webClient, ObjectMapper mapper) {
        this.webClient = webClient;
        this.mapper = mapper;
    }

    @Override
    public Flux<CatalogEntry> list(CatalogCode catalog) {
        if (catalog == null) {
            return Flux.error(new IllegalArgumentException("Catalog is required"));
        }
        return switch (catalog) {
            case CURRENCY -> pages(CURRENCIES + "/filter", true).map(node -> toCurrency(node));
            case COUNTRY -> pages(COUNTRIES + "/filter", true).map(node -> toCountry(node));
            default -> domainId(catalog, false).flatMapMany(id -> id == null
                    ? Flux.empty()
                    : pages(ITEMS, false).filter(node -> id.equals(uuid(node, "domainId"))).map(node -> toLookup(catalog, node)));
        };
    }

    @Override
    public Mono<CatalogEntry> find(CatalogCode catalog, String code) {
        if (code == null || code.isBlank()) {
            return Mono.error(new IllegalArgumentException("Catalog code is required"));
        }
        return list(catalog).filter(entry -> entry.code().equals(code.trim())).next();
    }

    @Override
    public Mono<CatalogEntry> findById(UUID id) {
        return read(CURRENCIES, id).map(this::toCurrency)
                .switchIfEmpty(read(COUNTRIES, id).map(this::toCountry))
                .switchIfEmpty(read(ITEMS, id).flatMap(node -> domainCode(uuid(node, "domainId"))
                        .map(catalog -> toLookup(catalog, node))));
    }

    @Override
    public Mono<CatalogEntry> create(CatalogEntry draft) {
        CatalogRules.require(draft);
        return switch (draft.catalog()) {
            case CURRENCY -> post(CURRENCIES, currencyBody(draft)).map(this::toCurrency);
            case COUNTRY -> post(COUNTRIES, countryBody(draft)).map(this::toCountry);
            default -> domainId(draft.catalog(), true)
                    .flatMap(domain -> post(ITEMS, itemBody(domain, draft)).map(node -> toLookup(draft.catalog(), node)));
        };
    }

    @Override
    public Mono<CatalogEntry> update(UUID id, CatalogEntry draft) {
        CatalogRules.require(draft);
        return readRequired(draft.catalog(), id).flatMap(current -> {
            ObjectNode body = (ObjectNode) current.deepCopy();
            apply(draft, body);
            return put(path(draft.catalog()), id, body).map(node -> from(draft.catalog(), node));
        });
    }

    @Override
    public Mono<CatalogEntry> changeStatus(UUID id, CatalogStatus status) {
        if (status == null) {
            return Mono.error(new IllegalArgumentException("Catalog status is required"));
        }
        return locate(id).flatMap(located -> {
            ObjectNode body = (ObjectNode) located.node().deepCopy();
            body.put("status", status.name());
            return put(located.path(), id, body).map(node -> from(located.catalog(), node));
        });
    }

    @Override
    public Mono<Void> remove(UUID id) {
        return locate(id).flatMap(located -> webClient.delete()
                .uri(located.path() + "/{id}", id)
                .retrieve()
                .toBodilessEntity()
                .then());
    }

    private void apply(CatalogEntry draft, ObjectNode body) {
        switch (draft.catalog()) {
            case CURRENCY -> {
                body.put("isoCode", draft.code().trim());
                body.put("currencyName", draft.label().trim());
                body.put("symbol", attribute(draft, "symbol"));
                body.put("decimalPrecision", Integer.parseInt(attribute(draft, "decimalPrecision")));
            }
            case COUNTRY -> {
                body.put("isoCode", draft.code().trim());
                body.put("countryName", draft.label().trim());
                body.put("region", attribute(draft, "region"));
            }
            default -> {
                body.put("itemCode", draft.code().trim());
                body.put("itemLabelDefault", draft.label().trim());
                body.put("extraJson", mapper.valueToTree(draft.attributes()).toString());
            }
        }
    }

    private String attribute(CatalogEntry draft, String name) {
        String value = draft.attributes().get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private Flux<JsonNode> pages(String path, boolean post) {
        return Flux.range(0, 20)
                .concatMap(page -> post ? postPage(path, page) : getPage(path, page))
                .takeUntil(rows -> rows.size() < 200)
                .flatMap(Flux::fromIterable);
    }

    private Mono<List<JsonNode>> postPage(String path, int page) {
        ObjectNode body = mapper.createObjectNode();
        body.putObject("filters");
        ObjectNode pagination = body.putObject("pagination");
        pagination.put("pageNumber", page);
        pagination.put("pageSize", 200);
        return webClient.post().uri(path).bodyValue(body).retrieve().bodyToMono(String.class).map(this::readTree).map(this::content);
    }

    private Mono<List<JsonNode>> getPage(String path, int page) {
        return webClient.get()
                .uri(uri -> uri.path(path).queryParam("pageNumber", page).queryParam("pageSize", 200).build())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::readTree)
                .map(this::content);
    }

    private List<JsonNode> content(JsonNode page) {
        JsonNode content = page.get("content");
        if (content == null || !content.isArray()) {
            if (page.isArray()) {
                List<JsonNode> rows = new ArrayList<>();
                page.forEach(rows::add);
                return rows;
            }
            return List.of();
        }
        List<JsonNode> rows = new ArrayList<>();
        content.forEach(rows::add);
        return rows;
    }

    private Mono<UUID> domainId(CatalogCode catalog, boolean create) {
        return pages(DOMAINS, false)
                .filter(node -> catalog.name().equals(node.path("domainCode").asText()))
                .map(node -> uuid(node, "domainId"))
                .next()
                .switchIfEmpty(create ? createDomain(catalog) : Mono.empty());
    }

    private Mono<UUID> createDomain(CatalogCode catalog) {
        ObjectNode body = mapper.createObjectNode();
        body.put("domainCode", catalog.name());
        body.put("domainName", catalog.name());
        body.put("status", "ACTIVE");
        return post(DOMAINS, body).map(node -> uuid(node, "domainId"));
    }

    private Mono<CatalogCode> domainCode(UUID domainId) {
        return pages(DOMAINS, false)
                .filter(node -> domainId.equals(uuid(node, "domainId")))
                .map(node -> CatalogCode.valueOf(node.path("domainCode").asText()))
                .next();
    }

    private Mono<JsonNode> read(String path, UUID id) {
        return webClient.get().uri(path + "/{id}", id).exchangeToMono(response -> {
            if (response.statusCode().value() == 404) {
                return response.releaseBody().then(Mono.empty());
            }
            if (response.statusCode().isError()) {
                return response.createException().flatMap(Mono::error);
            }
            return response.bodyToMono(String.class).map(this::readTree);
        });
    }

    private Mono<JsonNode> readRequired(CatalogCode catalog, UUID id) {
        return read(path(catalog), id).switchIfEmpty(Mono.error(new IllegalArgumentException("Catalog entry was not found")));
    }

    private Mono<Located> locate(UUID id) {
        return read(CURRENCIES, id).map(node -> new Located(CURRENCIES, CatalogCode.CURRENCY, node))
                .switchIfEmpty(read(COUNTRIES, id).map(node -> new Located(COUNTRIES, CatalogCode.COUNTRY, node)))
                .switchIfEmpty(read(ITEMS, id).flatMap(node -> domainCode(uuid(node, "domainId"))
                        .map(catalog -> new Located(ITEMS, catalog, node))))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Catalog entry was not found")));
    }

    private Mono<JsonNode> post(String path, ObjectNode body) {
        return webClient.post().uri(path).bodyValue(body).retrieve().bodyToMono(String.class).map(this::readTree);
    }

    private Mono<JsonNode> put(String path, UUID id, ObjectNode body) {
        return webClient.put().uri(path + "/{id}", id).bodyValue(body).retrieve().bodyToMono(String.class).map(this::readTree);
    }

    private JsonNode readTree(String text) {
        try {
            return mapper.readTree(text);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Reference-data response was not JSON");
        }
    }

    private ObjectNode currencyBody(CatalogEntry draft) {
        ObjectNode body = mapper.createObjectNode();
        body.put("isoCode", draft.code().trim());
        body.put("currencyName", draft.label().trim());
        body.put("symbol", attribute(draft, "symbol"));
        body.put("decimalPrecision", Integer.parseInt(attribute(draft, "decimalPrecision")));
        body.put("status", statusName(draft.status()));
        return body;
    }

    private ObjectNode countryBody(CatalogEntry draft) {
        ObjectNode body = mapper.createObjectNode();
        body.put("isoCode", draft.code().trim());
        body.put("countryName", draft.label().trim());
        body.put("region", attribute(draft, "region"));
        body.put("status", statusName(draft.status()));
        return body;
    }

    private ObjectNode itemBody(UUID domainId, CatalogEntry draft) {
        ObjectNode body = mapper.createObjectNode();
        body.put("domainId", domainId.toString());
        body.put("itemCode", draft.code().trim());
        body.put("itemLabelDefault", draft.label().trim());
        body.put("sortOrder", 0);
        body.put("isCurrent", true);
        body.put("extraJson", writeAttributes(draft.attributes()));
        body.put("status", statusName(draft.status()));
        return body;
    }

    private String writeAttributes(Map<String, String> attributes) {
        try {
            return mapper.writeValueAsString(attributes == null ? Map.of() : attributes);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("Catalog attributes could not be written");
        }
    }

    private CatalogEntry from(CatalogCode catalog, JsonNode node) {
        return switch (catalog) {
            case CURRENCY -> toCurrency(node);
            case COUNTRY -> toCountry(node);
            default -> toLookup(catalog, node);
        };
    }

    private CatalogEntry toCurrency(JsonNode node) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("symbol", node.path("symbol").asText(""));
        attributes.put("decimalPrecision", node.path("decimalPrecision").asText("2"));
        return new CatalogEntry(uuid(node, "currencyId", "id"), CatalogCode.CURRENCY, node.path("isoCode").asText(),
                node.path("currencyName").asText(), status(node), Map.copyOf(attributes));
    }

    private CatalogEntry toCountry(JsonNode node) {
        return new CatalogEntry(uuid(node, "countryId", "id"), CatalogCode.COUNTRY, node.path("isoCode").asText(),
                node.path("countryName").asText(), status(node), Map.of("region", node.path("region").asText()));
    }

    private CatalogEntry toLookup(CatalogCode catalog, JsonNode node) {
        return new CatalogEntry(uuid(node, "itemId", "id"), catalog, node.path("itemCode").asText(),
                node.path("itemLabelDefault").asText(), status(node), readAttributes(node.path("extraJson").asText("")));
    }

    private Map<String, String> readAttributes(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode node = mapper.readTree(json);
            Map<String, String> attributes = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                attributes.put(field.getKey(), field.getValue().asText());
            }
            return Map.copyOf(attributes);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return Map.of();
        }
    }

    private CatalogStatus status(JsonNode node) {
        return "INACTIVE".equals(node.path("status").asText()) ? CatalogStatus.INACTIVE : CatalogStatus.ACTIVE;
    }

    private String statusName(CatalogStatus status) {
        return status == null ? CatalogStatus.ACTIVE.name() : status.name();
    }

    private String path(CatalogCode catalog) {
        return switch (catalog) {
            case CURRENCY -> CURRENCIES;
            case COUNTRY -> COUNTRIES;
            default -> ITEMS;
        };
    }

    private UUID uuid(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText("");
            if (!value.isBlank()) {
                return UUID.fromString(value);
            }
        }
        throw new IllegalArgumentException("Catalog response did not include an id");
    }

    private record Located(String path, CatalogCode catalog, JsonNode node) {
    }
}
