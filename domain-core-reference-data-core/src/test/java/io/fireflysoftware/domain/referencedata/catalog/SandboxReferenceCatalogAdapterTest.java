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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fireflysoftware.domain.referencedata.catalog.workflows.ChangeCatalogEntryStatusSaga;
import io.fireflysoftware.domain.referencedata.catalog.workflows.CreateCatalogEntrySaga;
import io.fireflysoftware.domain.referencedata.catalog.workflows.RemoveCatalogEntrySaga;
import io.fireflysoftware.domain.referencedata.catalog.workflows.UpdateCatalogEntrySaga;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SandboxReferenceCatalogAdapterTest {

    @Test
    void sandboxCoversTransfersCurrenciesFamiliesAndAccountSpaces() {
        SandboxReferenceCatalogAdapter adapter = new SandboxReferenceCatalogAdapter();

        CatalogEntry internal = adapter.find(CatalogCode.TRANSFER_TYPE, "INTERNAL").block();
        assertThat(internal.attributes()).containsEntry("executionChannel", ExecutionChannel.INTERNAL_BOOK.name());
        assertThat(internal.attributes()).containsEntry("accountSpaceMovement", "true");
        assertThat(internal.attributes()).containsEntry("bookTransfer", "true");
        assertThat(internal.label()).contains("traspaso");

        CatalogEntry domestic = adapter.find(CatalogCode.TRANSFER_TYPE, "DOMESTIC").block();
        assertThat(domestic.attributes()).containsEntry("executionChannel", ExecutionChannel.LIBRARY_RAILS.name());
        assertThat(domestic.attributes()).containsEntry("outboundRoute", "OUTBOUND_RAIL");

        CatalogEntry international = adapter.find(CatalogCode.TRANSFER_TYPE, "INTERNATIONAL").block();
        assertThat(international.attributes()).containsEntry("executionChannel", ExecutionChannel.LIBRARY_PSPS.name());
        assertThat(international.attributes()).containsEntry("outboundRoute", "OUTBOUND_PSP");

        assertThat(adapter.list(CatalogCode.CURRENCY).map(CatalogEntry::code).collectList().block())
                .contains("EUR", "USD", "GBP")
                .allMatch(code -> code.length() == 3);

        assertThat(adapter.find(CatalogCode.PRODUCT_FAMILY, "INSURANCE").block().attributes())
                .containsEntry("regulation", Regulation.NON_REGULATED.name());
        assertThat(adapter.find(CatalogCode.PRODUCT_FAMILY, "LENDING").block().attributes())
                .containsEntry("regulation", Regulation.REGULATED.name());

        assertThat(adapter.list(CatalogCode.ACCOUNT_SPACE_TYPE).map(CatalogEntry::code).collectList().block())
                .containsExactlyInAnyOrder("MAIN", "SAVINGS", "VACATION", "EMERGENCY", "GOALS", "CUSTOM");
        assertThat(adapter.find(CatalogCode.RAIL_SCHEME, "SEPA_CREDIT_TRANSFER").block().attributes())
                .containsEntry("executionChannel", ExecutionChannel.LIBRARY_RAILS.name());
    }

    @Test
    void createCompensatesByDeleteAndStatusRestores() {
        SandboxReferenceCatalogAdapter adapter = new SandboxReferenceCatalogAdapter();
        CreateCatalogEntrySaga create = new CreateCatalogEntrySaga(adapter);
        ExecutionContext ctx = ExecutionContext.forSaga("CreateCatalogEntrySaga", "test");
        CatalogEntry draft = new CatalogEntry(null, CatalogCode.CURRENCY, "CAD", "Canadian Dollar",
                CatalogStatus.ACTIVE, Map.of("symbol", "$", "decimalPrecision", "2"));
        CatalogEntry created = create.createCatalogEntry(draft, ctx).block();
        assertThat(adapter.find(CatalogCode.CURRENCY, "CAD").block()).isNotNull();
        create.deleteCreatedCatalogEntry(created).block();
        assertThat(adapter.find(CatalogCode.CURRENCY, "CAD").block()).isNull();

        ChangeCatalogEntryStatusSaga status = new ChangeCatalogEntryStatusSaga(adapter);
        ExecutionContext statusCtx = ExecutionContext.forSaga("ChangeCatalogEntryStatusSaga", "test");
        UUID eur = adapter.find(CatalogCode.CURRENCY, "EUR").block().id();
        status.readCatalogEntryForStatus(eur, statusCtx).block();
        status.changeCatalogEntryStatus(CatalogStatus.INACTIVE, statusCtx).block();
        assertThat(adapter.findById(eur).block().status()).isEqualTo(CatalogStatus.INACTIVE);
        status.restoreCatalogEntryStatus(adapter.findById(eur).block(), statusCtx).block();
        assertThat(adapter.findById(eur).block().status()).isEqualTo(CatalogStatus.ACTIVE);
    }

    @Test
    void updateAndRemoveCompensate() {
        SandboxReferenceCatalogAdapter adapter = new SandboxReferenceCatalogAdapter();
        UUID id = adapter.find(CatalogCode.COUNTRY, "ES").block().id();
        UpdateCatalogEntrySaga update = new UpdateCatalogEntrySaga(adapter);
        ExecutionContext ctx = ExecutionContext.forSaga("UpdateCatalogEntrySaga", "test");
        update.readCatalogEntryForUpdate(id, ctx).block();
        CatalogEntry changed = update.updateCatalogEntry(new CatalogEntry(
                id, CatalogCode.COUNTRY, "ES", "Kingdom of Spain", CatalogStatus.ACTIVE, Map.of("region", "EUROPE")), ctx).block();
        assertThat(changed.label()).isEqualTo("Kingdom of Spain");
        update.restoreCatalogEntry(changed, ctx).block();
        assertThat(adapter.findById(id).block().label()).isEqualTo("Spain");

        RemoveCatalogEntrySaga remove = new RemoveCatalogEntrySaga(adapter);
        ExecutionContext removeCtx = ExecutionContext.forSaga("RemoveCatalogEntrySaga", "test");
        remove.readCatalogEntryForRemove(id, removeCtx).block();
        remove.removeCatalogEntry(id, removeCtx).block();
        assertThat(adapter.findById(id).block()).isNull();
        remove.recreateCatalogEntry(null, removeCtx).block();
        assertThat(adapter.find(CatalogCode.COUNTRY, "ES").block()).isNotNull();
    }

    @Test
    void queryIsADirectReadAndSandboxIsTheDefaultAdapter() {
        SandboxReferenceCatalogAdapter adapter = new SandboxReferenceCatalogAdapter();
        assertThat(new ReferenceCatalogQuery(adapter).find(CatalogCode.CURRENCY, "JPY").block().code()).isEqualTo("JPY");

        new ApplicationContextRunner()
                .withUserConfiguration(ReferenceCatalogAdapterConfiguration.class)
                .run(context -> assertThat(context.getBean(ReferenceCatalogPort.class))
                        .isInstanceOf(SandboxReferenceCatalogAdapter.class));
        new ApplicationContextRunner()
                .withUserConfiguration(ReferenceCatalogAdapterConfiguration.class)
                .withPropertyValues("firefly.reference-data.catalog.adapter=core")
                .run(context -> assertThat(context.getBean(ReferenceCatalogPort.class))
                        .isInstanceOf(MastersReferenceCatalogAdapter.class));
    }

    @Test
    void coreAdapterCreatesACurrencyOnTheReferenceDataMicroservice() {
        ObjectMapper mapper = new ObjectMapper();
        UUID id = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
        WebClient client = WebClient.builder().exchangeFunction(request -> {
            assertThat(request.url().getPath()).isEqualTo("/api/v1/currencies");
            ObjectNode body = mapper.createObjectNode();
            body.put("currencyId", id.toString());
            body.put("isoCode", "CAD");
            body.put("currencyName", "Canadian Dollar");
            body.put("symbol", "$");
            body.put("decimalPrecision", 2);
            body.put("status", "ACTIVE");
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body.toString())
                    .build());
        }).build();
        MastersReferenceCatalogAdapter adapter = new MastersReferenceCatalogAdapter(client, mapper);
        CatalogEntry created = adapter.create(new CatalogEntry(
                null, CatalogCode.CURRENCY, "CAD", "Canadian Dollar", CatalogStatus.ACTIVE,
                Map.of("symbol", "$", "decimalPrecision", "2"))).block();
        assertThat(created.id()).isEqualTo(id);
        assertThat(created.code()).isEqualTo("CAD");
    }
}
