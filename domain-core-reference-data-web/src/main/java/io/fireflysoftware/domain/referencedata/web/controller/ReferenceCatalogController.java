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

package io.fireflysoftware.domain.referencedata.web.controller;

import io.fireflysoftware.domain.referencedata.catalog.CatalogCode;
import io.fireflysoftware.domain.referencedata.catalog.CatalogEntry;
import io.fireflysoftware.domain.referencedata.catalog.CatalogStatus;
import io.fireflysoftware.domain.referencedata.catalog.ReferenceCatalogQuery;
import io.fireflysoftware.domain.referencedata.catalog.workflows.ReferenceCatalogCommands;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared catalogs. Reads call the port directly. Writes run the sagas.
 */
@RestController
@RequestMapping("/api/v1/catalogs")
public class ReferenceCatalogController {

    private final ReferenceCatalogQuery query;
    private final ReferenceCatalogCommands commands;

    public ReferenceCatalogController(ReferenceCatalogQuery query, ReferenceCatalogCommands commands) {
        this.query = query;
        this.commands = commands;
    }

    @GetMapping("/{catalog}")
    public Mono<ResponseEntity<List<CatalogEntry>>> list(@PathVariable CatalogCode catalog) {
        return query.list(catalog).collectList().map(ResponseEntity::ok);
    }

    @GetMapping("/{catalog}/{code}")
    public Mono<ResponseEntity<CatalogEntry>> find(@PathVariable CatalogCode catalog, @PathVariable String code) {
        return query.find(catalog, code)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<CatalogEntry>> create(@RequestBody CatalogWrite request) {
        return commands.create(request.toEntry(null))
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .onErrorResume(IllegalArgumentException.class, error -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<CatalogEntry>> update(@PathVariable UUID id, @RequestBody CatalogWrite request) {
        return commands.update(id, request.toEntry(id))
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, error -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @PostMapping("/{id}/status")
    public Mono<ResponseEntity<CatalogEntry>> changeStatus(@PathVariable UUID id, @RequestBody StatusWrite request) {
        return commands.changeStatus(id, request.status())
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, error -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> remove(@PathVariable UUID id) {
        return commands.remove(id)
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .onErrorResume(IllegalArgumentException.class, error -> Mono.just(ResponseEntity.badRequest().build()));
    }

    public record CatalogWrite(
            CatalogCode catalog,
            String code,
            String label,
            Map<String, String> attributes) {

        CatalogEntry toEntry(UUID id) {
            return new CatalogEntry(id, catalog, code, label, CatalogStatus.ACTIVE,
                    attributes == null ? Map.of() : attributes);
        }
    }

    public record StatusWrite(CatalogStatus status) {
    }
}
