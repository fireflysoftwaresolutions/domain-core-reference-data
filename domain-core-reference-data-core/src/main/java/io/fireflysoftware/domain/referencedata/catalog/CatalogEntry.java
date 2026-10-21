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

import java.util.Map;
import java.util.UUID;

/**
 * One shared catalog row. Currency and country codes stay validated strings.
 * The row is not a product, a party, or a contract.
 */
public record CatalogEntry(
        UUID id,
        CatalogCode catalog,
        String code,
        String label,
        CatalogStatus status,
        Map<String, String> attributes) {
}
