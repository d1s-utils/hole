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
import dev.d1s.hole.dto.storageObject.StorageObjectDto;
import dev.d1s.hole.dto.storageObject.StorageObjectGroupDto;
import dev.d1s.hole.entity.metadata.MetadataProperty;
import dev.d1s.hole.entity.storageObject.StorageObject;
import dev.d1s.hole.entity.storageObject.StorageObjectGroup;
import dev.d1s.teabag.dto.DtoConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class StorageObjectGroupDtoConverter implements DtoConverter<StorageObjectGroupDto, StorageObjectGroup> {

    private DtoConverter<StorageObjectDto, StorageObject> storageObjectDtoConverter;

    private DtoConverter<MetadataPropertyDto, MetadataProperty> metadataPropertyDtoConverter;

    @NotNull
    @Override
    public StorageObjectGroupDto convertToDto(@NotNull StorageObjectGroup storageObjectGroup) {
        return new StorageObjectGroupDto(
                Objects.requireNonNull(storageObjectGroup.getId()),
                Objects.requireNonNull(storageObjectGroup.getCreationTime()),
                storageObjectGroup.getName(),
                storageObjectGroup.getStorageObjects()
                        .stream()
                        .map(storageObjectDtoConverter::convertToDto)
                        .collect(Collectors.toSet()),
                storageObjectGroup.getMetadata()
                        .stream()
                        .map(metadataPropertyDtoConverter::convertToDto)
                        .collect(Collectors.toSet())
        );
    }

    @NotNull
    @Override
    public StorageObjectGroup convertToEntity(@NotNull StorageObjectGroupDto storageObjectGroupDto) {
        throw new UnsupportedOperationException();
    }

    @Autowired
    public void setStorageObjectDtoConverter(final DtoConverter<StorageObjectDto, StorageObject> storageObjectDtoConverter) {
        this.storageObjectDtoConverter = storageObjectDtoConverter;
    }

    @Autowired
    public void setMetadataPropertyDtoConverter(final DtoConverter<MetadataPropertyDto, MetadataProperty> metadataPropertyDtoConverter) {
        this.metadataPropertyDtoConverter = metadataPropertyDtoConverter;
    }
}
