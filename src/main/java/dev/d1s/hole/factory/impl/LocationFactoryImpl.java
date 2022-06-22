/*
 * Copyright 2022 Hole project
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

package dev.d1s.hole.factory.impl;

import dev.d1s.hole.factory.LocationFactory;
import dev.d1s.hole.properties.SslConfigurationProperties;
import dev.d1s.teabag.web.ServletUriComponentsBuilderKt;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class LocationFactoryImpl implements LocationFactory {

    private SslConfigurationProperties sslConfigurationProperties;

    @NotNull
    @Override
    public URI createLocation(@NotNull final String identifier) {
        return ServletUriComponentsBuilderKt.buildFromCurrentRequest(b -> {
                    ServletUriComponentsBuilderKt.configureSsl(b, sslConfigurationProperties.isFallBackToHttps());
                    return b.path("/" + identifier)
                            .build()
                            .toUri();
                }
        );
    }

    @Autowired
    public void setSslConfigurationProperties(final SslConfigurationProperties sslConfigurationProperties) {
        this.sslConfigurationProperties = sslConfigurationProperties;
    }
}
