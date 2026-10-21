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

package io.fireflysoftware.domain.referencedata.catalog.workflows;

import io.fireflysoftware.domain.referencedata.catalog.CatalogEntry;
import io.fireflysoftware.domain.referencedata.catalog.ReferenceCatalogPort;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Removes a catalog entry. Compensation recreates it. The reference-data microservice allows delete. */
@Saga(name = "RemoveCatalogEntrySaga")
@Service
public class RemoveCatalogEntrySaga {

    static final String STEP_READ = "readCatalogEntryForRemove";
    static final String STEP_REMOVE = "removeCatalogEntry";
    static final String COMPENSATE = "recreateCatalogEntry";
    static final String PREVIOUS = "removedCatalogEntry";

    private final ReferenceCatalogPort catalogs;

    public RemoveCatalogEntrySaga(ReferenceCatalogPort catalogs) {
        this.catalogs = catalogs;
    }

    @SagaStep(id = STEP_READ)
    @StepEvent(type = "reference.catalog.remove.read")
    public Mono<CatalogEntry> readCatalogEntryForRemove(UUID id, ExecutionContext ctx) {
        return catalogs.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Catalog entry was not found")))
                .doOnNext(current -> ctx.putVariable(PREVIOUS, current));
    }

    @SagaStep(id = STEP_REMOVE, dependsOn = STEP_READ, compensate = COMPENSATE)
    @StepEvent(type = "reference.catalog.removed")
    public Mono<CatalogEntry> removeCatalogEntry(UUID id, ExecutionContext ctx) {
        CatalogEntry previous = ctx.getVariableAs(PREVIOUS, CatalogEntry.class);
        if (previous == null || previous.id() == null) {
            return Mono.error(new IllegalStateException("Catalog entry was not read before remove"));
        }
        return catalogs.remove(previous.id()).thenReturn(previous);
    }

    public Mono<Void> recreateCatalogEntry(CatalogEntry removed, ExecutionContext ctx) {
        CatalogEntry previous = ctx.getVariableAs(PREVIOUS, CatalogEntry.class);
        if (previous == null) {
            previous = removed;
        }
        if (previous == null || previous.id() == null) {
            return Mono.empty();
        }
        return catalogs.create(previous).then();
    }
}
