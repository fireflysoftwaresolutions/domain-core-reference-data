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

package io.fireflysoftware.domain.referencedata.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "io.fireflysoftware.domain.referencedata")
@ConfigurationPropertiesScan
@OpenAPIDefinition(
        info = @Info(
                title = "Firefly OpenPlatform - Domain - Reference Data",
                version = "26.10.5",
                description = "Shared catalogs for currencies, countries, transfer types, rail schemes, account space types, and product families.",
                contact = @Contact(name = "Firefly Software Foundation", email = "dev@fireflysoftware.io")
        )
)
public class DomainReferenceDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(DomainReferenceDataApplication.class, args);
    }
}
