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

import java.util.UUID;

/**
 * Port for shared catalogs. Banking and insurance cores read currencies, countries,
 * transfer types, rail schemes, account space types, product families, and document types here.
 *
 * <p>The default adapter is {@link SandboxReferenceCatalogAdapter}. An installer replaces
 * that adapter with {@link MastersReferenceCatalogAdapter} by setting
 * {@code firefly.reference-data.catalog.adapter=core}.
 */
public interface ReferenceCatalogPort {

    Flux<CatalogEntry> list(CatalogCode catalog);

    Mono<CatalogEntry> find(CatalogCode catalog, String code);

    Mono<CatalogEntry> findById(UUID id);

    Mono<CatalogEntry> create(CatalogEntry draft);

    Mono<CatalogEntry> update(UUID id, CatalogEntry draft);

    Mono<CatalogEntry> changeStatus(UUID id, CatalogStatus status);

    Mono<Void> remove(UUID id);
}
