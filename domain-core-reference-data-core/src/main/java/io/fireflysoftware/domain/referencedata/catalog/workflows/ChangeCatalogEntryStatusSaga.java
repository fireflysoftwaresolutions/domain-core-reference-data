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
import io.fireflysoftware.domain.referencedata.catalog.CatalogStatus;
import io.fireflysoftware.domain.referencedata.catalog.ReferenceCatalogPort;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Changes catalog status and compensates by restoring the previous status.
 * The reference-data catalogs used here have no history resource, so no history row is written.
 */
@Saga(name = "ChangeCatalogEntryStatusSaga")
@Service
public class ChangeCatalogEntryStatusSaga {

    static final String STEP_READ = "readCatalogEntryForStatus";
    static final String STEP_STATUS = "changeCatalogEntryStatus";
    static final String COMPENSATE = "restoreCatalogEntryStatus";
    static final String PREVIOUS = "previousCatalogStatus";

    private final ReferenceCatalogPort catalogs;

    public ChangeCatalogEntryStatusSaga(ReferenceCatalogPort catalogs) {
        this.catalogs = catalogs;
    }

    @SagaStep(id = STEP_READ)
    @StepEvent(type = "reference.catalog.status.read")
    public Mono<CatalogEntry> readCatalogEntryForStatus(UUID id, ExecutionContext ctx) {
        return catalogs.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Catalog entry was not found")))
                .doOnNext(current -> {
                    ctx.putVariable("catalogEntryId", current.id());
                    ctx.putVariable(PREVIOUS, current.status());
                });
    }

    @SagaStep(id = STEP_STATUS, dependsOn = STEP_READ, compensate = COMPENSATE)
    @StepEvent(type = "reference.catalog.status.changed")
    public Mono<CatalogEntry> changeCatalogEntryStatus(CatalogStatus status, ExecutionContext ctx) {
        UUID id = ctx.getVariableAs("catalogEntryId", UUID.class);
        if (id == null) {
            return Mono.error(new IllegalStateException("Catalog entry was not read before the status change"));
        }
        if (status == null) {
            return Mono.error(new IllegalArgumentException("Catalog status is required"));
        }
        return catalogs.changeStatus(id, status);
    }

    public Mono<Void> restoreCatalogEntryStatus(CatalogEntry updated, ExecutionContext ctx) {
        CatalogStatus previous = ctx.getVariableAs(PREVIOUS, CatalogStatus.class);
        UUID id = updated == null ? ctx.getVariableAs("catalogEntryId", UUID.class) : updated.id();
        if (id == null || previous == null) {
            return Mono.empty();
        }
        return catalogs.changeStatus(id, previous).then();
    }
}
