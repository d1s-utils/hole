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

import dev.d1s.hole.dto.StorageObjectAccessDto;
import dev.d1s.hole.dto.StorageObjectDto;
import dev.d1s.hole.entity.StorageObject;
import dev.d1s.hole.entity.StorageObjectAccess;
import dev.d1s.teabag.dto.DtoConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class StorageObjectDtoConverter implements DtoConverter<StorageObjectDto, StorageObject> {

    private DtoConverter<StorageObjectAccessDto, StorageObjectAccess> storageObjectAccessDtoConverter;

    @NotNull
    @Override
    public StorageObjectDto convertToDto(@NotNull final StorageObject storageObject) {
        return new StorageObjectDto(
                Objects.requireNonNull(storageObject.getId()),
                Objects.requireNonNull(storageObject.getCreationTime()),
                storageObject.getName(),
                storageObject.getObjectGroup(),
                storageObject.isEncrypted(),
                storageObject.getStorageObjectAccesses()
                        .stream()
                        .map(storageObjectAccessDtoConverter::convertToDto)
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
}
