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

package dev.d1s.hole.dto.converter.storageObject;

import dev.d1s.hole.dto.metadata.MetadataPropertyDto;
import dev.d1s.hole.dto.storageObject.StorageObjectGroupAlterationDto;
import dev.d1s.hole.entity.metadata.MetadataProperty;
import dev.d1s.hole.entity.storageObject.StorageObjectGroup;
import dev.d1s.teabag.dto.DtoConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.stream.Collectors;

@Component
public class StorageObjectGroupAlterationDtoConverter implements DtoConverter<StorageObjectGroupAlterationDto, StorageObjectGroup> {

    private DtoConverter<MetadataPropertyDto, MetadataProperty> metadataPropertyDtoConverter;

    @NotNull
    @Override
    public StorageObjectGroupAlterationDto convertToDto(@NotNull StorageObjectGroup storageObjectGroup) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public StorageObjectGroup convertToEntity(@NotNull StorageObjectGroupAlterationDto storageObjectGroupAlterationDto) {
        return new StorageObjectGroup(
                storageObjectGroupAlterationDto.name(),
                new HashSet<>(),
                storageObjectGroupAlterationDto.metadata()
                        .stream()
                        .map(metadataPropertyDtoConverter::convertToEntity)
                        .collect(Collectors.toSet())
        );
    }

    @Autowired
    public void setMetadataPropertyDtoConverter(final DtoConverter<MetadataPropertyDto, MetadataProperty> metadataPropertyDtoConverter) {
        this.metadataPropertyDtoConverter = metadataPropertyDtoConverter;
    }
}
