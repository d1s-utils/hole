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

package dev.d1s.hole.service.storageObject;

import dev.d1s.hole.dto.common.EntityWithDto;
import dev.d1s.hole.dto.common.EntityWithDtoSet;
import dev.d1s.hole.dto.storageObject.StorageObjectDto;
import dev.d1s.hole.entity.storageObject.StorageObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

public interface StorageObjectService {

    @NotNull
    EntityWithDto<StorageObject, StorageObjectDto> getObject(
            @NotNull final String id,
            final boolean requireDto
    );

    void writeRawObjectToWeb(
            @NotNull final String id,
            @Nullable final String encryptionKey,
            @NotNull final HttpServletResponse response,
            @Nullable final String contentDisposition
    );

    @NotNull
    EntityWithDtoSet<StorageObject, StorageObjectDto> getAllObjects(final boolean requireDto);

    @NotNull
    EntityWithDto<StorageObject, StorageObjectDto> createObject(
            @NotNull final MultipartFile content,
            @NotNull final String group,
            @Nullable final String encryptionKey
    );

    @NotNull
    EntityWithDto<StorageObject, StorageObjectDto> updateObject(
            @NotNull final String id,
            @NotNull final StorageObject storageObject
    );

    void overwriteObject(
            @NotNull final String id,
            @NotNull final MultipartFile content,
            @Nullable final String encryptionKey
    );

    void deleteObject(@NotNull final String id);
}