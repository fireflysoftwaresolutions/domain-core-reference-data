<a href="https://fireflysoftware.io/openplatform/">
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-dark.svg">
  <img src="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-light.svg" alt="Firefly OpenPlatform - Domain - Reference Data" width="100%">
</picture>
</a>

# Firefly OpenPlatform - Domain - Reference Data

Developed by the Firefly Software Foundation. [dev@fireflysoftware.io](mailto:dev@fireflysoftware.io)

[Website](https://fireflysoftware.io/) · [OpenPlatform](https://fireflysoftware.io/openplatform/) · [Module catalogue](https://fireflysoftware.io/openplatform/modules/)

Shared catalogs for banking and insurance journeys. Product, party, and contract stay in their own masters. This service does not store products, parties, or contracts. Identity and rules stay in Jafly.

## Port

`ReferenceCatalogPort` (`io.fireflysoftware.domain.referencedata.catalog.ReferenceCatalogPort`).

Reads are direct (`ReferenceCatalogQuery`). Writes run sagas. Create compensates by delete. Update restores the previous row. Status change restores the previous status. The currency, country, and lookup masters used here do not expose a history resource, so a status change does not write a separate history row. Remove is allowed because those masters allow delete, and it compensates by recreating the previous row.

The default adapter is `SandboxReferenceCatalogAdapter`. It returns the dataset below and does not call a network. An installer replaces that class by setting `firefly.reference-data.catalog.adapter=core`. The selected class is `MastersReferenceCatalogAdapter`, which calls `core-common-reference-master-data`.

Currency values stay validated three-letter strings. The master lists them. Country codes stay two- or three-letter strings.

## Sandbox catalogs

| Catalog | Codes |
| --- | --- |
| `CURRENCY` | `EUR`, `USD`, `GBP`, `CHF`, `JPY` |
| `COUNTRY` | `ES`, `US`, `GB`, `DE`, `FR`, `PT`, `MX`, `BR`, `CH`, `JP` |
| `TRANSFER_TYPE` | `INTERNAL` uses `INTERNAL_BOOK` (traspaso, including movement between account spaces). `DOMESTIC` and `INTERNATIONAL` use `LIBRARY_RAILS` with routes `OUTBOUND_RAIL` and `INBOUND_RAIL`. International credit transfers are rails because library-rails exposes SWIFT. A PSP route is for an instrument library-psps supports, such as a card. |
| `RAIL_SCHEME` | `SEPA_CREDIT_TRANSFER`, `SEPA_INSTANT`, `ACH`, `FASTER_PAYMENTS`, each on `LIBRARY_RAILS` |
| `ACCOUNT_SPACE_TYPE` | `MAIN`, `SAVINGS`, `VACATION`, `EMERGENCY`, `GOALS`, `CUSTOM` (the accounts core enum). This catalog does not open accounts. |
| `PRODUCT_FAMILY` | `DEPOSITS`, `PAYMENTS`, `LENDING`, and `CARDS` are `REGULATED` banking families. `INSURANCE` is `NON_REGULATED`. These are family flags, not products. |
| `DOCUMENT_TYPE` | Consulting-kit documents `report`, `proposal`, `sow`, `memo`, `minutes`, `one-pager`, `letter`, and `welcome`. Each sandbox row is English (`en`), product family `LENDING`, and regulated `true`. Attributes are `language`, `productFamily`, `regulated`, `templateCode`, and `kitKind`. A journey issues a contract with `sow` and a letter with `letter`. The kit has no notice document. These rows choose a template. They are not stored documents, products, or contracts. |

Industry schemes are not seeded here. Identity-document and activity-code masters remain in `core-common-reference-master-data`.

Business parameters, including payments clearing account identifiers, nostro, transfer routes, and `journey.lending.min-credit-score`, are on `BusinessParameterPort` in `domain-core-configuration`. `core-banking-ledger` remains the book.
