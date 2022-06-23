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

package dev.d1s.hole.service.impl;

import dev.d1s.advice.exception.BadRequestException;
import dev.d1s.hole.constant.error.MetadataErrorConstants;
import dev.d1s.hole.entity.common.MetadataAware;
import dev.d1s.hole.entity.metadata.MetadataProperty;
import dev.d1s.hole.repository.MetadataPropertyRepository;
import dev.d1s.hole.service.MetadataService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MetadataServiceImpl implements MetadataService {

    private static final Logger log = LogManager.getLogger();

    private MetadataPropertyRepository metadataPropertyRepository;

    private MetadataServiceImpl metadataServiceImpl;

    @NotNull
    @Override
    @Transactional(readOnly = true)
    public Optional<MetadataProperty> findMetadataPropertyByPropertyNameAndValue(@NotNull final String property, @NotNull final String value) {
        final var foundMetadata = metadataPropertyRepository.findByPropertyAndValue(property, value);

        log.debug("findMetadataPropertyByPropertyNameAndValue: {}", foundMetadata);

        return foundMetadata;
    }

    @NotNull
    @Override
    @Transactional
    public MetadataProperty saveMetadataProperty(@NotNull MetadataProperty metadataProperty) {
        final var savedMetadata = metadataPropertyRepository.save(metadataProperty);

        log.debug("saveMetadataProperty: {}", savedMetadata);

        return savedMetadata;
    }

    @Override
    public void checkMetadata(@NotNull MetadataAware metadataAware) {
        final var propertySet = new HashSet<String>();

        for (final var p : metadataAware.getMetadata()) {
            if (!propertySet.add(p.getProperty())) {
                throw new BadRequestException(MetadataErrorConstants.DUPLICATE_METADATA_PROPERTY_ERROR);
            }
        }
    }

    @Override
    public void transferMetadata(@NotNull MetadataAware from, @NotNull MetadataAware to) {
        to.setMetadata(
                from.getMetadata()
                        .stream()
                        .map(metadataProperty ->
                                metadataServiceImpl.findMetadataPropertyByPropertyNameAndValue(
                                        metadataProperty.getProperty(),
                                        metadataProperty.getValue()
                                ).orElse(metadataServiceImpl.saveMetadataProperty(metadataProperty))
                        )
                        .collect(Collectors.toSet())
        );
    }

    @Autowired
    public void setMetadataPropertyRepository(final MetadataPropertyRepository metadataPropertyRepository) {
        this.metadataPropertyRepository = metadataPropertyRepository;
    }

    @Lazy
    @Autowired
    public void setMetadataServiceImpl(final MetadataServiceImpl metadataServiceImpl) {
        this.metadataServiceImpl = metadataServiceImpl;
    }
}
