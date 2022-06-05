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

import dev.d1s.hole.constant.contentDisposition.ContentDispositionConstants;
import dev.d1s.hole.controller.StorageObjectController;
import dev.d1s.hole.dto.StorageObjectDto;
import dev.d1s.hole.dto.StorageObjectUpdateDto;
import dev.d1s.hole.entity.StorageObject;
import dev.d1s.hole.properties.SslConfigurationProperties;
import dev.d1s.hole.service.StorageObjectService;
import dev.d1s.teabag.dto.DtoConverter;
import dev.d1s.teabag.web.ServletUriComponentsBuilderKt;
import io.undertow.util.Headers;
import org.cryptonode.jncryptor.CryptorException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

@RestController
public class StorageObjectControllerImpl implements StorageObjectController {

    private StorageObjectService storageObjectService;

    private SslConfigurationProperties sslConfigurationProperties;

    private DtoConverter<StorageObjectUpdateDto, StorageObject> storageObjectUpdateDtoConverter;

    @NotNull
    @Override
    public ResponseEntity<StorageObjectDto> getObject(@NotNull final String id) {
        return ResponseEntity.ok(
                storageObjectService.getObject(id, true).dto()
        );
    }

    @NotNull
    @Override
    public ResponseEntity<byte[]> getRawObject(
            @NotNull final String id,
            @Nullable final String contentDisposition,
            @Nullable final String encryptionKey,
            @NotNull final HttpServletResponse response
    ) throws IOException, CryptorException {
        final var rawObject = storageObjectService.getRawObject(id, encryptionKey);

        response.setHeader(
                Headers.CONTENT_TYPE_STRING,
                rawObject.contentType()
        );

        response.setHeader(
                Headers.CONTENT_DISPOSITION_STRING,
                ContentDisposition.builder(
                                contentDisposition != null
                                        ? contentDisposition
                                        : ContentDispositionConstants.DEFAULT_CONTENT_DISPOSITION_TYPE
                        )
                        .filename(rawObject.name())
                        .build()
                        .toString()
        );

        return ResponseEntity.ok(rawObject.content());
    }

    @NotNull
    @Override
    public ResponseEntity<Set<StorageObjectDto>> getAllObjects() {
        return ResponseEntity.ok(
                storageObjectService.getAllObjects(true).dtos()
        );
    }

    @NotNull
    @Override
    public ResponseEntity<StorageObjectDto> postObject(@NotNull final MultipartFile content, @NotNull final String group, @Nullable final String encryptionKey) throws IOException, CryptorException {
        final var createdObject = storageObjectService.createObject(content, group, encryptionKey).dto();

        return ResponseEntity.created(
                ServletUriComponentsBuilderKt.buildFromCurrentRequest(b -> {
                            ServletUriComponentsBuilderKt.configureSsl(b, sslConfigurationProperties.isFallBackToHttps());
                            return b.path(Objects.requireNonNull(createdObject).id())
                                    .build()
                                    .toUri();
                        }
                )
        ).body(createdObject);
    }

    @NotNull
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
    @Override
    public ResponseEntity<?> putRawObject(@NotNull String id, @NotNull MultipartFile content, @Nullable final String encryptionKey) throws IOException, CryptorException {
        storageObjectService.overwriteObject(id, content, encryptionKey);

        return ResponseEntity.noContent().build();
    }

    @NotNull
    @Override
    public ResponseEntity<?> deleteObject(@NotNull final String id) throws IOException {
        storageObjectService.deleteObject(id);

        return ResponseEntity.noContent().build();
    }

    @Autowired
    public void setStorageObjectService(final StorageObjectService storageObjectService) {
        this.storageObjectService = storageObjectService;
    }

    @Autowired
    public void setSslConfigurationProperties(final SslConfigurationProperties sslConfigurationProperties) {
        this.sslConfigurationProperties = sslConfigurationProperties;
    }

    @Autowired
    public void setStorageObjectUpdateDtoConverter(final DtoConverter<StorageObjectUpdateDto, StorageObject> storageObjectUpdateDtoConverter) {
        this.storageObjectUpdateDtoConverter = storageObjectUpdateDtoConverter;
    }
}