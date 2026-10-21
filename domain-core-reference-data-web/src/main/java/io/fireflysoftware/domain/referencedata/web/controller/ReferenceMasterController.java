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

import io.fireflysoftware.domain.referencedata.catalog.CatalogEntry;
import io.fireflysoftware.domain.referencedata.catalog.ReferenceCatalogQuery;
import io.fireflysoftware.domain.referencedata.catalog.ReferenceMaster;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reads a core master through the catalog port. Sandbox does not copy the core table.
 */
@RestController
@RequestMapping("/api/v1/masters")
public class ReferenceMasterController {

    private final ReferenceCatalogQuery query;

    public ReferenceMasterController(ReferenceCatalogQuery query) {
        this.query = query;
    }

    @GetMapping("/{master}")
    public Mono<List<CatalogEntry>> list(@PathVariable ReferenceMaster master) {
        return query.listMaster(master).collectList();
    }
}
