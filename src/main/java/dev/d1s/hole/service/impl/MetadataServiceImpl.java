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

import dev.d1s.hole.entity.metadata.MetadataProperty;
import dev.d1s.hole.repository.MetadataPropertyRepository;
import dev.d1s.hole.service.MetadataService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MetadataServiceImpl implements MetadataService {

    private MetadataPropertyRepository metadataPropertyRepository;

    @NotNull
    @Override
    @Transactional(readOnly = true)
    public Optional<MetadataProperty> findMetadataPropertyByPropertyNameAndValue(@NotNull final String property, @NotNull final String value) {
        return metadataPropertyRepository.findByPropertyAndValue(property, value);
    }

    @NotNull
    @Override
    @Transactional
    public MetadataProperty saveMetadataProperty(@NotNull MetadataProperty metadataProperty) {
        return metadataPropertyRepository.save(metadataProperty);
    }

    @Autowired
    public void setMetadataPropertyRepository(final MetadataPropertyRepository metadataPropertyRepository) {
        this.metadataPropertyRepository = metadataPropertyRepository;
    }
}
