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

/** Updates a catalog entry and compensates by writing the previous row back. Status stays unchanged. */
@Saga(name = "UpdateCatalogEntrySaga")
@Service
public class UpdateCatalogEntrySaga {

    static final String STEP_READ = "readCatalogEntryForUpdate";
    static final String STEP_UPDATE = "updateCatalogEntry";
    static final String COMPENSATE = "restoreCatalogEntry";
    static final String PREVIOUS = "previousCatalogEntry";

    private final ReferenceCatalogPort catalogs;

    public UpdateCatalogEntrySaga(ReferenceCatalogPort catalogs) {
        this.catalogs = catalogs;
    }

    @SagaStep(id = STEP_READ)
    @StepEvent(type = "reference.catalog.read")
    public Mono<CatalogEntry> readCatalogEntryForUpdate(UUID id, ExecutionContext ctx) {
        return catalogs.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Catalog entry was not found")))
                .doOnNext(current -> ctx.putVariable(PREVIOUS, current));
    }

    @SagaStep(id = STEP_UPDATE, dependsOn = STEP_READ, compensate = COMPENSATE)
    @StepEvent(type = "reference.catalog.updated")
    public Mono<CatalogEntry> updateCatalogEntry(CatalogEntry draft, ExecutionContext ctx) {
        CatalogEntry previous = ctx.getVariableAs(PREVIOUS, CatalogEntry.class);
        if (previous == null || previous.id() == null) {
            return Mono.error(new IllegalStateException("Catalog entry was not read before update"));
        }
        return catalogs.update(previous.id(), draft);
    }

    public Mono<Void> restoreCatalogEntry(CatalogEntry updated, ExecutionContext ctx) {
        CatalogEntry previous = ctx.getVariableAs(PREVIOUS, CatalogEntry.class);
        if (previous == null || previous.id() == null) {
            return Mono.empty();
        }
        return catalogs.update(previous.id(), previous).then();
    }
}
