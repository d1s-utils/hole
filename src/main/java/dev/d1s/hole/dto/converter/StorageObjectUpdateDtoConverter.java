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

package dev.d1s.hole.dto.converter;

import dev.d1s.hole.dto.metadata.MetadataPropertyDto;
import dev.d1s.hole.dto.storageObject.StorageObjectUpdateDto;
import dev.d1s.hole.entity.metadata.MetadataProperty;
import dev.d1s.hole.entity.storageObject.StorageObject;
import dev.d1s.teabag.dto.DtoConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class StorageObjectUpdateDtoConverter implements DtoConverter<StorageObjectUpdateDto, StorageObject> {

    private DtoConverter<MetadataPropertyDto, MetadataProperty> metadataPropertyDtoConverter;

    @NotNull
    @Override
    public StorageObjectUpdateDto convertToDto(@NotNull StorageObject storageObject) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public StorageObject convertToEntity(@NotNull StorageObjectUpdateDto storageObjectUpdateDto) {
        return new StorageObject(
                storageObjectUpdateDto.name(),
                storageObjectUpdateDto.group(),
                storageObjectUpdateDto.metadata()
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
