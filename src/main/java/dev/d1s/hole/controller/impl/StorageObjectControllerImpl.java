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

package dev.d1s.hole.controller.impl;

import dev.d1s.hole.controller.StorageObjectController;
import dev.d1s.hole.dto.storageObject.StorageObjectDto;
import dev.d1s.hole.dto.storageObject.StorageObjectUpdateDto;
import dev.d1s.hole.entity.storageObject.StorageObject;
import dev.d1s.hole.factory.LocationFactory;
import dev.d1s.hole.service.storageObject.StorageObjectService;
import dev.d1s.security.configuration.annotation.Secured;
import dev.d1s.teabag.dto.DtoConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.Set;

@RestController
public class StorageObjectControllerImpl implements StorageObjectController {

    private StorageObjectService storageObjectService;

    private LocationFactory locationFactory;

    private DtoConverter<StorageObjectUpdateDto, StorageObject> storageObjectUpdateDtoConverter;

    @NotNull
    @Secured
    @Override
    public ResponseEntity<StorageObjectDto> getObject(@NotNull final String id) {
        return ResponseEntity.ok(
                storageObjectService.getObject(id, true).dto()
        );
    }

    @Secured
    @Override
    public void readRawObject(
            @NotNull final String id,
            @Nullable final String contentDisposition,
            @Nullable final String encryptionKey,
            @NotNull final HttpServletResponse response
    ) {
        storageObjectService.writeRawObjectToWeb(id, encryptionKey, response, contentDisposition);
    }

    @NotNull
    @Secured
    @Override
    public ResponseEntity<Set<StorageObjectDto>> getAllObjects() {
        return ResponseEntity.ok(
                storageObjectService.getAllObjects(true).dtos()
        );
    }

    @NotNull
    @Secured
    @Override
    public ResponseEntity<StorageObjectDto> postObject(
            @NotNull final MultipartFile content,
            @NotNull final String group,
            @Nullable final String encryptionKey,
            @Nullable final String name
    ) {
        final var createdObject = storageObjectService.createObject(content, group, encryptionKey, name).dto();

        return ResponseEntity.created(
                locationFactory.createLocation(
                        Objects.requireNonNull(createdObject).id()
                )
        ).body(createdObject);
    }

    @NotNull
    @Secured
    @Override
    public ResponseEntity<StorageObjectDto> putObject(@NotNull String id, @NotNull StorageObjectUpdateDto storageObjectUpdateDto) {
        return ResponseEntity.ok(
                storageObjectService.updateObject(
                        id,
                        storageObjectUpdateDtoConverter.convertToEntity(storageObjectUpdateDto)
                ).dto()
        );
    }

    @NotNull
    @Secured
    @Override
    public ResponseEntity<?> putRawObject(
            @NotNull String id,
            @NotNull MultipartFile content,
            @Nullable final String encryptionKey,
            @Nullable final String name
    ) {
        storageObjectService.overwriteObject(id, content, encryptionKey, name);

        return ResponseEntity.noContent().build();
    }

    @NotNull
    @Secured
    @Override
    public ResponseEntity<?> deleteObject(@NotNull final String id) {
        storageObjectService.deleteObject(id);

        return ResponseEntity.noContent().build();
    }

    @Autowired
    public void setStorageObjectService(final StorageObjectService storageObjectService) {
        this.storageObjectService = storageObjectService;
    }

    @Autowired
    public void setLocationFactory(final LocationFactory locationFactory) {
        this.locationFactory = locationFactory;
    }

    @Autowired
    public void setStorageObjectUpdateDtoConverter(final DtoConverter<StorageObjectUpdateDto, StorageObject> storageObjectUpdateDtoConverter) {
        this.storageObjectUpdateDtoConverter = storageObjectUpdateDtoConverter;
    }
}