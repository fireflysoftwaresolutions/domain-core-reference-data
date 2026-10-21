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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fireflysoftware.common.reference.master.data.sdk.api.ActivityCodesApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.AdministrativeDivisionsApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.AssetTypeApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.BankInstitutionCodesApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.ConsentCatalogApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.ContractDocumentTypeApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.ContractRoleApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.ContractRoleScopeApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.ContractTypeApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.CountriesApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.CurrenciesApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.DocumentTemplateLocalizationsApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.DocumentTemplateTypesApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.DocumentTemplatesApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.IdentityDocumentCategoriesApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.IdentityDocumentLocalizationsApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.IdentityDocumentsApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.LanguageLocaleApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.LegalFormsApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.LookupDomainsApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.LookupItemsApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.MessageTypeCatalogApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.NotificationMessageCatalogApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.NotificationMessageLocalizationsApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.NotificationMessageTemplatesApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.RelationshipTypeMasterApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.RuleOperationTypeApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.TitleMasterApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.TransactionCategoryCatalogApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.TransactionCategoryLocalizationApi;
import io.fireflysoftware.common.reference.master.data.sdk.invoker.ApiClient;
import io.fireflysoftware.common.reference.master.data.sdk.model.CountryDTO;
import io.fireflysoftware.common.reference.master.data.sdk.model.CurrencyDTO;
import io.fireflysoftware.common.reference.master.data.sdk.model.FilterRequestContractRoleDTO;
import io.fireflysoftware.common.reference.master.data.sdk.model.FilterRequestContractRoleScopeDTO;
import io.fireflysoftware.common.reference.master.data.sdk.model.FilterRequestCountryDTO;
import io.fireflysoftware.common.reference.master.data.sdk.model.FilterRequestCurrencyDTO;
import io.fireflysoftware.common.reference.master.data.sdk.model.FilterRequestLegalFormDTO;
import io.fireflysoftware.common.reference.master.data.sdk.model.FilterRequestRuleOperationTypeDTO;
import io.fireflysoftware.common.reference.master.data.sdk.model.LookupDomainDTO;
import io.fireflysoftware.common.reference.master.data.sdk.model.LookupItemDTO;
import io.fireflysoftware.common.reference.master.data.sdk.model.PaginationRequest;
import io.fireflysoftware.common.reference.master.data.sdk.model.PaginationResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Live catalog port. Every master is read through core-common-reference-master-data-sdk.
 * Selected with {@code firefly.reference-data.catalog.adapter=core}. Sandbox stays the default.
 * This class does not keep a second copy of the master tables.
 */
public final class MastersReferenceCatalogAdapter implements ReferenceCatalogPort {

    private static final int PAGE_SIZE = 200;

    private final CurrenciesApi currencies;
    private final CountriesApi countries;
    private final LookupDomainsApi domains;
    private final LookupItemsApi items;
    private final ActivityCodesApi activityCodes;
    private final AdministrativeDivisionsApi divisions;
    private final AssetTypeApi assetTypes;
    private final BankInstitutionCodesApi bankCodes;
    private final ConsentCatalogApi consents;
    private final ContractDocumentTypeApi contractDocumentTypes;
    private final ContractRoleApi contractRoles;
    private final ContractRoleScopeApi contractRoleScopes;
    private final ContractTypeApi contractTypes;
    private final DocumentTemplatesApi documentTemplates;
    private final DocumentTemplateTypesApi documentTemplateTypes;
    private final DocumentTemplateLocalizationsApi documentTemplateLocalizations;
    private final IdentityDocumentsApi identityDocuments;
    private final IdentityDocumentCategoriesApi identityDocumentCategories;
    private final IdentityDocumentLocalizationsApi identityDocumentLocalizations;
    private final LanguageLocaleApi languages;
    private final LegalFormsApi legalForms;
    private final MessageTypeCatalogApi messageTypes;
    private final NotificationMessageCatalogApi notificationMessages;
    private final NotificationMessageTemplatesApi notificationTemplates;
    private final NotificationMessageLocalizationsApi notificationLocalizations;
    private final RelationshipTypeMasterApi relationshipTypes;
    private final RuleOperationTypeApi ruleOperationTypes;
    private final TitleMasterApi titles;
    private final TransactionCategoryCatalogApi transactionCategories;
    private final TransactionCategoryLocalizationApi transactionCategoryLocalizations;
    private final ObjectMapper mapper = new ObjectMapper();

    public MastersReferenceCatalogAdapter(String basePath) {
        this(configured(basePath));
    }

    /**
     * Test constructor. The SDK sends the core's published paths through the supplied client.
     */
    static MastersReferenceCatalogAdapter over(WebClient webClient) {
        return new MastersReferenceCatalogAdapter(new ApiClient(webClient));
    }

    private static ApiClient configured(String basePath) {
        return io.fireflysoftware.domain.referencedata.infra.ReferenceMasterClients.client(basePath);
    }

    private MastersReferenceCatalogAdapter(ApiClient client) {
        this.currencies = new CurrenciesApi(client);
        this.countries = new CountriesApi(client);
        this.domains = new LookupDomainsApi(client);
        this.items = new LookupItemsApi(client);
        this.activityCodes = new ActivityCodesApi(client);
        this.divisions = new AdministrativeDivisionsApi(client);
        this.assetTypes = new AssetTypeApi(client);
        this.bankCodes = new BankInstitutionCodesApi(client);
        this.consents = new ConsentCatalogApi(client);
        this.contractDocumentTypes = new ContractDocumentTypeApi(client);
        this.contractRoles = new ContractRoleApi(client);
        this.contractRoleScopes = new ContractRoleScopeApi(client);
        this.contractTypes = new ContractTypeApi(client);
        this.documentTemplates = new DocumentTemplatesApi(client);
        this.documentTemplateTypes = new DocumentTemplateTypesApi(client);
        this.documentTemplateLocalizations = new DocumentTemplateLocalizationsApi(client);
        this.identityDocuments = new IdentityDocumentsApi(client);
        this.identityDocumentCategories = new IdentityDocumentCategoriesApi(client);
        this.identityDocumentLocalizations = new IdentityDocumentLocalizationsApi(client);
        this.languages = new LanguageLocaleApi(client);
        this.legalForms = new LegalFormsApi(client);
        this.messageTypes = new MessageTypeCatalogApi(client);
        this.notificationMessages = new NotificationMessageCatalogApi(client);
        this.notificationTemplates = new NotificationMessageTemplatesApi(client);
        this.notificationLocalizations = new NotificationMessageLocalizationsApi(client);
        this.relationshipTypes = new RelationshipTypeMasterApi(client);
        this.ruleOperationTypes = new RuleOperationTypeApi(client);
        this.titles = new TitleMasterApi(client);
        this.transactionCategories = new TransactionCategoryCatalogApi(client);
        this.transactionCategoryLocalizations = new TransactionCategoryLocalizationApi(client);
    }

    @Override
    public Flux<CatalogEntry> list(CatalogCode catalog) {
        if (catalog == null) {
            return Flux.error(new IllegalArgumentException("Catalog is required"));
        }
        return switch (catalog) {
            case CURRENCY -> listCurrencies();
            case COUNTRY -> listCountries();
            default -> domainId(catalog, false).flatMapMany(id -> id == null
                    ? Flux.empty()
                    : listItems().filter(item -> id.equals(item.getDomainId())).map(item -> toLookup(catalog, item)));
        };
    }

    @Override
    public Flux<CatalogEntry> listMaster(ReferenceMaster master) {
        if (master == null) {
            return Flux.error(new IllegalArgumentException("Master is required"));
        }
        return switch (master) {
            case CURRENCY -> list(CatalogCode.CURRENCY);
            case COUNTRY -> list(CatalogCode.COUNTRY);
            case LOOKUP_DOMAIN -> pages(page -> domains.listDomains(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case LOOKUP_ITEM -> pages(page -> items.listItems(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case ACTIVITY_CODE -> pages(page -> activityCodes.listActivityCodes(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case ADMINISTRATIVE_DIVISION -> pages(page -> divisions.listDivisions(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case ASSET_TYPE -> pages(page -> assetTypes.listAssetTypes(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case BANK_INSTITUTION_CODE -> pages(page -> bankCodes.listBankInstitutionCodes(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case CONSENT -> pages(page -> consents.listConsentCatalog(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case CONTRACT_DOCUMENT_TYPE -> pages(page -> contractDocumentTypes.listContractDocumentTypes(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case CONTRACT_ROLE -> filterPages(page -> contractRoles.filterContractRoles(roleFilter(page), null))
                    .map(row -> toGeneric(master, row));
            case CONTRACT_ROLE_SCOPE -> filterPages(page -> contractRoleScopes.filterContractRoleScopes(scopeFilter(page), null))
                    .map(row -> toGeneric(master, row));
            case CONTRACT_TYPE -> pages(page -> contractTypes.listContractTypes(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case DOCUMENT_TEMPLATE -> pages(page -> documentTemplates.listDocumentTemplates(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case DOCUMENT_TEMPLATE_TYPE -> pages(page -> documentTemplateTypes.listDocumentTemplateTypes(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case IDENTITY_DOCUMENT -> pages(page -> identityDocuments.listIdentityDocuments(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case IDENTITY_DOCUMENT_CATEGORY -> pages(page -> identityDocumentCategories.listIdentityDocumentCategories(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case IDENTITY_DOCUMENT_LOCALIZATION -> pages(page -> identityDocumentLocalizations.listIdentityDocumentLocalizations(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case LANGUAGE_LOCALE -> pages(page -> languages.listLanguageLocales(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case LEGAL_FORM -> filterPages(page -> legalForms.listLegalForms(legalFormFilter(page), null))
                    .map(row -> toGeneric(master, row));
            case MESSAGE_TYPE -> pages(page -> messageTypes.listMessageTypes(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case NOTIFICATION_MESSAGE -> pages(page -> notificationMessages.listNotificationMessages(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case NOTIFICATION_TEMPLATE -> pages(page -> notificationTemplates.listNotificationMessageTemplates(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case RELATIONSHIP_TYPE -> pages(page -> relationshipTypes.listRelationshipTypes(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case RULE_OPERATION_TYPE -> filterPages(page -> ruleOperationTypes.filterRuleOperationTypes(ruleFilter(page), null))
                    .map(row -> toGeneric(master, row));
            case TITLE -> pages(page -> titles.listTitles(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case TRANSACTION_CATEGORY -> pages(page -> transactionCategories.listTransactionCategories(page, PAGE_SIZE, null, null, null))
                    .map(row -> toGeneric(master, row));
            case DOCUMENT_TEMPLATE_LOCALIZATION, NOTIFICATION_MESSAGE_LOCALIZATION, TRANSACTION_CATEGORY_LOCALIZATION ->
                    Flux.error(new IllegalArgumentException(master.name() + " is read by id"));
        };
    }

    /**
     * Reads a localization master by id. Those cores do not publish an unscoped list.
     */
    public Mono<CatalogEntry> readLocalization(ReferenceMaster master, UUID id) {
        Mono<?> call = switch (master) {
            case DOCUMENT_TEMPLATE_LOCALIZATION -> documentTemplateLocalizations.getDocumentTemplateLocalization(id, null);
            case NOTIFICATION_MESSAGE_LOCALIZATION -> notificationLocalizations.getNotificationMessageLocalization(id, null);
            case TRANSACTION_CATEGORY_LOCALIZATION -> transactionCategoryLocalizations.getTransactionCategoryLocalization(id, null);
            default -> Mono.error(new IllegalArgumentException(master.name() + " is listed, not read by this method"));
        };
        return absent(call).map(row -> toGeneric(master, row));
    }

    @Override
    public Mono<CatalogEntry> find(CatalogCode catalog, String code) {
        if (code == null || code.isBlank()) {
            return Mono.error(new IllegalArgumentException("Catalog code is required"));
        }
        return list(catalog).filter(entry -> entry.code().equals(code.trim())).next();
    }

    @Override
    public Mono<CatalogEntry> findById(UUID id) {
        return absent(currencies.getCurrency(id, null)).map(this::toCurrency)
                .switchIfEmpty(absent(countries.getCountry(id, null)).map(this::toCountry))
                .switchIfEmpty(absent(items.getItem(id, null)).flatMap(item -> domainCode(item.getDomainId())
                        .map(catalog -> toLookup(catalog, item))));
    }

    @Override
    public Mono<CatalogEntry> create(CatalogEntry draft) {
        CatalogRules.require(draft);
        return switch (draft.catalog()) {
            case CURRENCY -> currencies.createCurrency(currencyBody(draft), null).map(this::toCurrency);
            case COUNTRY -> countries.createCountry(countryBody(draft), null).map(this::toCountry);
            default -> domainId(draft.catalog(), true)
                    .flatMap(domain -> items.createItem(itemBody(domain, draft), null).map(item -> toLookup(draft.catalog(), item)));
        };
    }

    @Override
    public Mono<CatalogEntry> update(UUID id, CatalogEntry draft) {
        CatalogRules.require(draft);
        return switch (draft.catalog()) {
            case CURRENCY -> absent(currencies.getCurrency(id, null))
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Catalog entry was not found")))
                    .flatMap(current -> currencies.updateCurrency(id, applyCurrency(current, draft), null).map(this::toCurrency));
            case COUNTRY -> absent(countries.getCountry(id, null))
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Catalog entry was not found")))
                    .flatMap(current -> countries.updateCountry(id, applyCountry(current, draft), null).map(this::toCountry));
            default -> absent(items.getItem(id, null))
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Catalog entry was not found")))
                    .flatMap(current -> items.updateItem(id, applyItem(current, draft), null).map(item -> toLookup(draft.catalog(), item)));
        };
    }

    @Override
    public Mono<CatalogEntry> changeStatus(UUID id, CatalogStatus status) {
        if (status == null) {
            return Mono.error(new IllegalArgumentException("Catalog status is required"));
        }
        return absent(currencies.getCurrency(id, null))
                .flatMap(current -> {
                    current.setStatus(CurrencyDTO.StatusEnum.fromValue(status.name()));
                    return currencies.updateCurrency(id, current, null).map(this::toCurrency);
                })
                .switchIfEmpty(absent(countries.getCountry(id, null)).flatMap(current -> {
                    current.setStatus(CountryDTO.StatusEnum.fromValue(status.name()));
                    return countries.updateCountry(id, current, null).map(this::toCountry);
                }))
                .switchIfEmpty(absent(items.getItem(id, null)).flatMap(current -> domainCode(current.getDomainId())
                        .flatMap(catalog -> {
                            current.setStatus(LookupItemDTO.StatusEnum.fromValue(status.name()));
                            return items.updateItem(id, current, null).map(item -> toLookup(catalog, item));
                        })))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Catalog entry was not found")));
    }

    @Override
    public Mono<Void> remove(UUID id) {
        return absent(currencies.getCurrency(id, null))
                .flatMap(found -> currencies.deleteCurrency(id, null))
                .switchIfEmpty(absent(countries.getCountry(id, null)).flatMap(found -> countries.deleteCountry(id, null)))
                .switchIfEmpty(absent(items.getItem(id, null)).flatMap(found -> items.deleteItem(id, null)))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Catalog entry was not found")))
                .then();
    }

    private Flux<CatalogEntry> listCurrencies() {
        return Flux.range(0, 20)
                .concatMap(page -> currencies.filterCurrencies(currencyFilter(page), null)
                        .map(pageResult -> pageResult.getContent() == null ? List.<CurrencyDTO>of() : pageResult.getContent()))
                .takeUntil(rows -> rows.size() < PAGE_SIZE)
                .flatMap(Flux::fromIterable)
                .map(this::toCurrency);
    }

    private Flux<CatalogEntry> listCountries() {
        return Flux.range(0, 20)
                .concatMap(page -> countries.filterCountries(countryFilter(page), null)
                        .map(pageResult -> pageResult.getContent() == null ? List.<CountryDTO>of() : pageResult.getContent()))
                .takeUntil(rows -> rows.size() < PAGE_SIZE)
                .flatMap(Flux::fromIterable)
                .map(this::toCountry);
    }

    private Flux<LookupItemDTO> listItems() {
        return pages(page -> items.listItems(page, PAGE_SIZE, null, null, null))
                .map(row -> mapper.convertValue(row, LookupItemDTO.class));
    }

    private Flux<Object> pages(java.util.function.Function<Integer, Mono<PaginationResponse>> pageCall) {
        return Flux.range(0, 20)
                .concatMap(page -> pageCall.apply(page).map(this::content))
                .takeUntil(rows -> rows.size() < PAGE_SIZE)
                .flatMap(Flux::fromIterable);
    }

    private Flux<Object> filterPages(java.util.function.Function<Integer, Mono<?>> pageCall) {
        return Flux.range(0, 20)
                .concatMap(page -> pageCall.apply(page).map(this::contentOf))
                .takeUntil(rows -> rows.size() < PAGE_SIZE)
                .flatMap(Flux::fromIterable);
    }

    private List<Object> content(PaginationResponse response) {
        return response == null || response.getContent() == null ? List.of() : response.getContent();
    }

    private List<Object> contentOf(Object page) {
        if (page instanceof PaginationResponse response) {
            return content(response);
        }
        Object content = mapper.convertValue(page, Map.class).get("content");
        if (content instanceof List<?> rows) {
            return rows.stream().map(row -> (Object) row).toList();
        }
        return List.of();
    }

    private Mono<UUID> domainId(CatalogCode catalog, boolean create) {
        return pages(page -> domains.listDomains(page, PAGE_SIZE, null, null, null))
                .map(row -> mapper.convertValue(row, LookupDomainDTO.class))
                .filter(domain -> catalog.name().equals(domain.getDomainCode()))
                .map(LookupDomainDTO::getDomainId)
                .next()
                .switchIfEmpty(create ? createDomain(catalog) : Mono.empty());
    }

    private Mono<UUID> createDomain(CatalogCode catalog) {
        LookupDomainDTO body = new LookupDomainDTO()
                .domainCode(catalog.name())
                .domainName(catalog.name())
                .status(LookupDomainDTO.StatusEnum.ACTIVE);
        return domains.createDomain(body, null).map(LookupDomainDTO::getDomainId);
    }

    private Mono<CatalogCode> domainCode(UUID domainId) {
        return pages(page -> domains.listDomains(page, PAGE_SIZE, null, null, null))
                .map(row -> mapper.convertValue(row, LookupDomainDTO.class))
                .filter(domain -> domainId.equals(domain.getDomainId()))
                .map(domain -> CatalogCode.valueOf(domain.getDomainCode()))
                .next();
    }

    private CurrencyDTO currencyBody(CatalogEntry draft) {
        return new CurrencyDTO()
                .isoCode(draft.code().trim())
                .currencyName(draft.label().trim())
                .symbol(attribute(draft, "symbol"))
                .decimalPrecision(Integer.parseInt(attribute(draft, "decimalPrecision")))
                .status(CurrencyDTO.StatusEnum.fromValue(statusName(draft.status())));
    }

    private CurrencyDTO applyCurrency(CurrencyDTO current, CatalogEntry draft) {
        return current.isoCode(draft.code().trim())
                .currencyName(draft.label().trim())
                .symbol(attribute(draft, "symbol"))
                .decimalPrecision(Integer.parseInt(attribute(draft, "decimalPrecision")));
    }

    private CountryDTO countryBody(CatalogEntry draft) {
        return new CountryDTO()
                .isoCode(draft.code().trim())
                .countryName(draft.label().trim())
                .region(CountryDTO.RegionEnum.fromValue(attribute(draft, "region")))
                .status(CountryDTO.StatusEnum.fromValue(statusName(draft.status())));
    }

    private CountryDTO applyCountry(CountryDTO current, CatalogEntry draft) {
        return current.isoCode(draft.code().trim())
                .countryName(draft.label().trim())
                .region(CountryDTO.RegionEnum.fromValue(attribute(draft, "region")));
    }

    private LookupItemDTO itemBody(UUID domainId, CatalogEntry draft) {
        return new LookupItemDTO()
                .domainId(domainId)
                .itemCode(draft.code().trim())
                .itemLabelDefault(draft.label().trim())
                .sortOrder(0)
                .isCurrent(true)
                .extraJson(writeAttributes(draft.attributes()))
                .status(LookupItemDTO.StatusEnum.fromValue(statusName(draft.status())));
    }

    private LookupItemDTO applyItem(LookupItemDTO current, CatalogEntry draft) {
        return current.itemCode(draft.code().trim())
                .itemLabelDefault(draft.label().trim())
                .extraJson(writeAttributes(draft.attributes()));
    }

    private FilterRequestCurrencyDTO currencyFilter(int page) {
        FilterRequestCurrencyDTO filter = new FilterRequestCurrencyDTO();
        filter.setPagination(page(page));
        return filter;
    }

    private FilterRequestCountryDTO countryFilter(int page) {
        FilterRequestCountryDTO filter = new FilterRequestCountryDTO();
        filter.setPagination(page(page));
        return filter;
    }

    private FilterRequestLegalFormDTO legalFormFilter(int page) {
        FilterRequestLegalFormDTO filter = new FilterRequestLegalFormDTO();
        filter.setPagination(page(page));
        return filter;
    }

    private FilterRequestContractRoleDTO roleFilter(int page) {
        FilterRequestContractRoleDTO filter = new FilterRequestContractRoleDTO();
        filter.setPagination(page(page));
        return filter;
    }

    private FilterRequestContractRoleScopeDTO scopeFilter(int page) {
        FilterRequestContractRoleScopeDTO filter = new FilterRequestContractRoleScopeDTO();
        filter.setPagination(page(page));
        return filter;
    }

    private FilterRequestRuleOperationTypeDTO ruleFilter(int page) {
        FilterRequestRuleOperationTypeDTO filter = new FilterRequestRuleOperationTypeDTO();
        filter.setPagination(page(page));
        return filter;
    }

    private PaginationRequest page(int page) {
        return new PaginationRequest().pageNumber(page).pageSize(PAGE_SIZE);
    }

    private CatalogEntry toCurrency(CurrencyDTO dto) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("symbol", dto.getSymbol() == null ? "" : dto.getSymbol());
        attributes.put("decimalPrecision", dto.getDecimalPrecision() == null ? "2" : dto.getDecimalPrecision().toString());
        return new CatalogEntry(dto.getCurrencyId(), CatalogCode.CURRENCY, dto.getIsoCode(), dto.getCurrencyName(),
                status(dto.getStatus() == null ? null : dto.getStatus().getValue()), Map.copyOf(attributes));
    }

    private CatalogEntry toCountry(CountryDTO dto) {
        return new CatalogEntry(dto.getCountryId(), CatalogCode.COUNTRY, dto.getIsoCode(), dto.getCountryName(),
                status(dto.getStatus() == null ? null : dto.getStatus().getValue()),
                Map.of("region", dto.getRegion() == null ? "" : dto.getRegion().getValue()));
    }

    private CatalogEntry toLookup(CatalogCode catalog, LookupItemDTO dto) {
        return new CatalogEntry(dto.getItemId(), catalog, dto.getItemCode(), dto.getItemLabelDefault(),
                status(dto.getStatus() == null ? null : dto.getStatus().getValue()), readAttributes(dto.getExtraJson()));
    }

    private CatalogEntry toGeneric(ReferenceMaster master, Object row) {
        @SuppressWarnings("unchecked")
        Map<String, Object> values = mapper.convertValue(row, Map.class);
        UUID id = firstUuid(values);
        String code = firstText(values, "isoCode", "code", "itemCode", "domainCode", "contractCode", "roleCode",
                "scopeCode", "templateCode", "messageCode", "typeCode", "activityCode", "categoryCode");
        String label = firstText(values, "currencyName", "countryName", "name", "itemLabelDefault", "domainName",
                "title", "label", "description");
        CatalogCode catalog = master.catalog() == null ? CatalogCode.CURRENCY : master.catalog();
        if (master.catalog() == null) {
            return new CatalogEntry(id, CatalogCode.CURRENCY, code == null ? String.valueOf(id) : code,
                    label == null ? master.name() : label, CatalogStatus.ACTIVE, Map.of("master", master.name()));
        }
        return new CatalogEntry(id, catalog, code == null ? "" : code, label == null ? "" : label, CatalogStatus.ACTIVE, Map.of());
    }

    private UUID firstUuid(Map<String, Object> values) {
        for (Map.Entry<String, Object> field : values.entrySet()) {
            if (field.getKey().endsWith("Id") && field.getValue() != null) {
                return UUID.fromString(field.getValue().toString());
            }
        }
        throw new IllegalArgumentException("Master response did not include an id");
    }

    private String firstText(Map<String, Object> values, String... names) {
        for (String name : names) {
            Object value = values.get(name);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }

    private Map<String, String> readAttributes(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = mapper.readValue(json, Map.class);
            Map<String, String> attributes = new LinkedHashMap<>();
            raw.forEach((key, value) -> attributes.put(key, value == null ? "" : value.toString()));
            return Map.copyOf(attributes);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return Map.of();
        }
    }

    private String writeAttributes(Map<String, String> attributes) {
        try {
            return mapper.writeValueAsString(attributes == null ? Map.of() : attributes);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("Catalog attributes could not be written");
        }
    }

    private String attribute(CatalogEntry draft, String name) {
        String value = draft.attributes() == null ? null : draft.attributes().get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private CatalogStatus status(String value) {
        return "INACTIVE".equals(value) ? CatalogStatus.INACTIVE : CatalogStatus.ACTIVE;
    }

    private String statusName(CatalogStatus status) {
        return status == null ? CatalogStatus.ACTIVE.name() : status.name();
    }

    private <T> Mono<T> absent(Mono<T> call) {
        return call.onErrorResume(WebClientResponseException.class, error ->
                error.getStatusCode().value() == 404 ? Mono.empty() : Mono.error(error));
    }
}
