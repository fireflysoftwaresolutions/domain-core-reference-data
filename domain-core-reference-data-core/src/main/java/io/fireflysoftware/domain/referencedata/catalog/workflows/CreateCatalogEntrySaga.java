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

/** Creates a catalog entry and compensates by deleting it. The reference-data microservice allows delete. */
@Saga(name = "CreateCatalogEntrySaga")
@Service
public class CreateCatalogEntrySaga {

    static final String STEP = "createCatalogEntry";
    static final String COMPENSATE = "deleteCreatedCatalogEntry";

    private final ReferenceCatalogPort catalogs;

    public CreateCatalogEntrySaga(ReferenceCatalogPort catalogs) {
        this.catalogs = catalogs;
    }

    @SagaStep(id = STEP, compensate = COMPENSATE)
    @StepEvent(type = "reference.catalog.created")
    public Mono<CatalogEntry> createCatalogEntry(CatalogEntry draft, ExecutionContext ctx) {
        return catalogs.create(draft).doOnNext(created -> ctx.putVariable("createdCatalogEntry", created));
    }

    public Mono<Void> deleteCreatedCatalogEntry(CatalogEntry created) {
        if (created == null || created.id() == null) {
            return Mono.empty();
        }
        return catalogs.remove(created.id());
    }
}
