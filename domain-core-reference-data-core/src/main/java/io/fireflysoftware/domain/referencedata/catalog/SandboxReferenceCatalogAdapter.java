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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default adapter. It returns the sandbox dataset and never opens a network connection.
 * An installer replaces this class with {@link MastersReferenceCatalogAdapter}.
 */
public final class SandboxReferenceCatalogAdapter implements ReferenceCatalogPort {

    private final Map<UUID, CatalogEntry> byId = new ConcurrentHashMap<>();

    public SandboxReferenceCatalogAdapter() {
        SandboxCatalogDataset.entries().forEach(entry -> byId.put(entry.id(), entry));
    }

    @Override
    public Flux<CatalogEntry> listMaster(ReferenceMaster master) {
        if (master == null) {
            return Flux.error(new IllegalArgumentException("Master is required"));
        }
        if (master.catalog() == null) {
            return Flux.empty();
        }
        return list(master.catalog());
    }

    @Override
    public Flux<CatalogEntry> list(CatalogCode catalog) {
        if (catalog == null) {
            return Flux.error(new IllegalArgumentException("Catalog is required"));
        }
        return Flux.fromIterable(byId.values().stream()
                .filter(entry -> entry.catalog() == catalog)
                .sorted(Comparator.comparing(CatalogEntry::code))
                .toList());
    }

    @Override
    public Mono<CatalogEntry> find(CatalogCode catalog, String code) {
        if (catalog == null || code == null || code.isBlank()) {
            return Mono.error(new IllegalArgumentException("Catalog and code are required"));
        }
        return Mono.justOrEmpty(byId.values().stream()
                .filter(entry -> entry.catalog() == catalog && entry.code().equals(code.trim()))
                .findFirst());
    }

    @Override
    public Mono<CatalogEntry> findById(UUID id) {
        return Mono.justOrEmpty(byId.get(id));
    }

    @Override
    public Mono<CatalogEntry> create(CatalogEntry draft) {
        return Mono.fromCallable(() -> {
            synchronized (this) {
                CatalogRules.require(draft);
                String code = draft.code().trim();
                if (findStored(draft.catalog(), code) != null) {
                    throw new IllegalArgumentException("Catalog code already exists");
                }
                UUID id = draft.id() == null ? SandboxCatalogDataset.id(draft.catalog(), code) : draft.id();
                if (byId.containsKey(id)) {
                    throw new IllegalArgumentException("Catalog id already exists");
                }
                CatalogEntry created = copy(id, draft, draft.status() == null ? CatalogStatus.ACTIVE : draft.status());
                byId.put(id, created);
                return created;
            }
        });
    }

    @Override
    public Mono<CatalogEntry> update(UUID id, CatalogEntry draft) {
        return Mono.fromCallable(() -> {
            synchronized (this) {
                CatalogEntry current = required(id);
                CatalogRules.require(draft);
                if (draft.catalog() != current.catalog()) {
                    throw new IllegalArgumentException("Catalog cannot change");
                }
                CatalogEntry other = findStored(draft.catalog(), draft.code().trim());
                if (other != null && !other.id().equals(id)) {
                    throw new IllegalArgumentException("Catalog code already exists");
                }
                CatalogEntry updated = copy(id, draft, current.status());
                byId.put(id, updated);
                return updated;
            }
        });
    }

    @Override
    public Mono<CatalogEntry> changeStatus(UUID id, CatalogStatus status) {
        return Mono.fromCallable(() -> {
            synchronized (this) {
                if (status == null) {
                    throw new IllegalArgumentException("Catalog status is required");
                }
                CatalogEntry current = required(id);
                CatalogEntry updated = new CatalogEntry(
                        current.id(), current.catalog(), current.code(), current.label(), status, current.attributes());
                byId.put(id, updated);
                return updated;
            }
        });
    }

    @Override
    public Mono<Void> remove(UUID id) {
        return Mono.fromRunnable(() -> {
            synchronized (this) {
                required(id);
                byId.remove(id);
            }
        });
    }

    private CatalogEntry findStored(CatalogCode catalog, String code) {
        return byId.values().stream()
                .filter(entry -> entry.catalog() == catalog && entry.code().equals(code))
                .findFirst()
                .orElse(null);
    }

    private CatalogEntry required(UUID id) {
        CatalogEntry current = byId.get(id);
        if (current == null) {
            throw new IllegalArgumentException("Catalog entry was not found");
        }
        return current;
    }

    private CatalogEntry copy(UUID id, CatalogEntry draft, CatalogStatus status) {
        return new CatalogEntry(
                id,
                draft.catalog(),
                draft.code().trim(),
                draft.label().trim(),
                status,
                draft.attributes() == null ? Map.of() : Map.copyOf(draft.attributes()));
    }
}
