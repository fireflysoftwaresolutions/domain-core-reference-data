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

/**
 * Shared catalog published by the reference-data microservice.
 * This is not a product, party, or contract master.
 * Document types are communication templates, not stored documents.
 */
public enum CatalogCode {
    CURRENCY,
    COUNTRY,
    TRANSFER_TYPE,
    RAIL_SCHEME,
    ACCOUNT_SPACE_TYPE,
    PRODUCT_FAMILY,
    DOCUMENT_TYPE
}
