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
import dev.d1s.hole.dto.storageObject.StorageObjectAccessDto;
import dev.d1s.hole.dto.storageObject.StorageObjectDto;
import dev.d1s.hole.entity.metadata.MetadataProperty;
import dev.d1s.hole.entity.storageObject.StorageObject;
import dev.d1s.hole.entity.storageObject.StorageObjectAccess;
import dev.d1s.teabag.dto.DtoConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class StorageObjectDtoConverter implements DtoConverter<StorageObjectDto, StorageObject> {

    private DtoConverter<StorageObjectAccessDto, StorageObjectAccess> storageObjectAccessDtoConverter;

    private DtoConverter<MetadataPropertyDto, MetadataProperty> metadataPropertyDtoConverter;

    @NotNull
    @Override
    public StorageObjectDto convertToDto(@NotNull final StorageObject storageObject) {
        return new StorageObjectDto(
                Objects.requireNonNull(storageObject.getId()),
                Objects.requireNonNull(storageObject.getCreationTime()),
                storageObject.getName(),
                Objects.requireNonNull(storageObject.getGroup().getId()),
                storageObject.isEncrypted(),
                Objects.requireNonNull(storageObject.getDigest()),
                storageObject.getStorageObjectAccesses()
                        .stream()
                        .map(storageObjectAccessDtoConverter::convertToDto)
                        .collect(Collectors.toSet()),
                storageObject.getMetadata()
                        .stream()
                        .map(metadataPropertyDtoConverter::convertToDto)
                        .collect(Collectors.toSet())
        );
    }

    @NotNull
    @Override
    public StorageObject convertToEntity(@NotNull final StorageObjectDto storageObjectDto) {
        throw new UnsupportedOperationException();
    }

    @Autowired
    public void setStorageObjectAccessDtoConverter(final DtoConverter<StorageObjectAccessDto, StorageObjectAccess> storageObjectAccessDtoConverter) {
        this.storageObjectAccessDtoConverter = storageObjectAccessDtoConverter;
    }

    @Autowired
    public void setMetadataPropertyDtoConverter(final DtoConverter<MetadataPropertyDto, MetadataProperty> metadataPropertyDtoConverter) {
        this.metadataPropertyDtoConverter = metadataPropertyDtoConverter;
    }
}
