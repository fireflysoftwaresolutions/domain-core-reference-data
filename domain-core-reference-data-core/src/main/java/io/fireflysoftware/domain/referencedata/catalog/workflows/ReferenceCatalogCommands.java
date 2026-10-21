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
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Runs each catalog saga and applies its compensation when a later step fails. */
@Service
public class ReferenceCatalogCommands {

    private final CreateCatalogEntrySaga createSaga;
    private final UpdateCatalogEntrySaga updateSaga;
    private final ChangeCatalogEntryStatusSaga statusSaga;
    private final RemoveCatalogEntrySaga removeSaga;

    public ReferenceCatalogCommands(
            CreateCatalogEntrySaga createSaga,
            UpdateCatalogEntrySaga updateSaga,
            ChangeCatalogEntryStatusSaga statusSaga,
            RemoveCatalogEntrySaga removeSaga) {
        this.createSaga = createSaga;
        this.updateSaga = updateSaga;
        this.statusSaga = statusSaga;
        this.removeSaga = removeSaga;
    }

    public Mono<CatalogEntry> create(CatalogEntry draft) {
        ExecutionContext ctx = ExecutionContext.forSaga("CreateCatalogEntrySaga", "api");
        return createSaga.createCatalogEntry(draft, ctx)
                .onErrorResume(error -> compensate(createSaga.deleteCreatedCatalogEntry(
                        ctx.getVariableAs("createdCatalogEntry", CatalogEntry.class)), error));
    }

    public Mono<CatalogEntry> update(UUID id, CatalogEntry draft) {
        ExecutionContext ctx = ExecutionContext.forSaga("UpdateCatalogEntrySaga", "api");
        return updateSaga.readCatalogEntryForUpdate(id, ctx)
                .then(updateSaga.updateCatalogEntry(draft, ctx))
                .onErrorResume(error -> compensate(updateSaga.restoreCatalogEntry(null, ctx), error));
    }

    public Mono<CatalogEntry> changeStatus(UUID id, CatalogStatus status) {
        ExecutionContext ctx = ExecutionContext.forSaga("ChangeCatalogEntryStatusSaga", "api");
        return statusSaga.readCatalogEntryForStatus(id, ctx)
                .then(statusSaga.changeCatalogEntryStatus(status, ctx))
                .onErrorResume(error -> compensate(statusSaga.restoreCatalogEntryStatus(null, ctx), error));
    }

    public Mono<Void> remove(UUID id) {
        ExecutionContext ctx = ExecutionContext.forSaga("RemoveCatalogEntrySaga", "api");
        return removeSaga.readCatalogEntryForRemove(id, ctx)
                .then(removeSaga.removeCatalogEntry(id, ctx))
                .then()
                .onErrorResume(error -> compensate(removeSaga.recreateCatalogEntry(null, ctx), error));
    }

    private <T> Mono<T> compensate(Mono<Void> compensation, Throwable error) {
        return compensation.onErrorResume(ignored -> Mono.empty()).then(Mono.error(error));
    }
}
