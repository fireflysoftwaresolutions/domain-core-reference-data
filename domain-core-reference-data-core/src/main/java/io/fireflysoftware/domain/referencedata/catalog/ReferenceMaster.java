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
 * Master published by core-common-reference-master-data.
 * The live adapter reads it through that core's SDK. The sandbox does not copy the table.
 */
public enum ReferenceMaster {
    ACTIVITY_CODE("/api/v1/activity-codes", null),
    ADMINISTRATIVE_DIVISION("/api/v1/divisions", null),
    ASSET_TYPE("/api/v1/asset-types", null),
    BANK_INSTITUTION_CODE("/api/v1/bank-institution-codes", null),
    CONSENT("/api/v1/consent-catalog", null),
    CONTRACT_DOCUMENT_TYPE("/api/v1/contract-document-types", null),
    CONTRACT_ROLE("/api/v1/contract-roles/filter", null),
    CONTRACT_ROLE_SCOPE("/api/v1/contract-role-scopes/filter", null),
    CONTRACT_TYPE("/api/v1/contract-types", null),
    COUNTRY("/api/v1/countries/filter", CatalogCode.COUNTRY),
    CURRENCY("/api/v1/currencies/filter", CatalogCode.CURRENCY),
    DOCUMENT_TEMPLATE("/api/v1/document-templates", null),
    DOCUMENT_TEMPLATE_LOCALIZATION("/api/v1/document-template-localizations", null),
    DOCUMENT_TEMPLATE_TYPE("/api/v1/document-template-types", null),
    IDENTITY_DOCUMENT("/api/v1/identity-documents", null),
    IDENTITY_DOCUMENT_CATEGORY("/api/v1/identity-document-categories", null),
    IDENTITY_DOCUMENT_LOCALIZATION("/api/v1/identity-document-localizations", null),
    LANGUAGE_LOCALE("/api/v1/language-locales", null),
    LEGAL_FORM("/api/v1/legal-forms/filter", null),
    LOOKUP_DOMAIN("/api/v1/lookup/domains", null),
    LOOKUP_ITEM("/api/v1/lookup/items", null),
    MESSAGE_TYPE("/api/v1/message-types", null),
    NOTIFICATION_MESSAGE("/api/v1/notification-messages", null),
    NOTIFICATION_MESSAGE_LOCALIZATION("/api/v1/notification-localizations", null),
    NOTIFICATION_TEMPLATE("/api/v1/notification-templates", null),
    RELATIONSHIP_TYPE("/api/v1/relationship-types", null),
    RULE_OPERATION_TYPE("/api/v1/rule-operation-types/filter", null),
    TITLE("/api/v1/titles", null),
    TRANSACTION_CATEGORY("/api/v1/transaction-categories", null),
    TRANSACTION_CATEGORY_LOCALIZATION("/api/v1/transaction-category-localizations", null);

    private final String listPath;
    private final CatalogCode catalog;

    ReferenceMaster(String listPath, CatalogCode catalog) {
        this.listPath = listPath;
        this.catalog = catalog;
    }

    public String listPath() {
        return listPath;
    }

    public CatalogCode catalog() {
        return catalog;
    }
}
