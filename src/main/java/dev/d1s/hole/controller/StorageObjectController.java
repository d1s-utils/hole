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

package dev.d1s.hole.controller;

import dev.d1s.hole.constant.mapping.storageObject.StorageObjectRequestMappingConstants;
import dev.d1s.hole.dto.storageObject.StorageObjectDto;
import dev.d1s.hole.dto.storageObject.StorageObjectUpdateDto;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Set;

@Validated
public interface StorageObjectController {

    @NotNull
    @GetMapping(StorageObjectRequestMappingConstants.GET_OBJECT_MAPPING)
    ResponseEntity<StorageObjectDto> getObject(
            @NotNull
            @PathVariable
            @NotBlank final String id
    );

    @GetMapping(StorageObjectRequestMappingConstants.GET_RAW_OBJECT_MAPPING)
    void readRawObject(
            @NotNull
            @PathVariable
            @NotBlank final String id,
            @Nullable
            @RequestParam(required = false) final String contentDisposition,
            @Nullable
            @RequestParam(required = false) final String encryptionKey,
            @NotNull final HttpServletResponse response
    );

    @NotNull
    @GetMapping(StorageObjectRequestMappingConstants.GET_ALL_OBJECTS_MAPPING)
    ResponseEntity<Set<StorageObjectDto>> getAllObjects();

    @NotNull
    @PostMapping(StorageObjectRequestMappingConstants.POST_OBJECT_MAPPING)
    ResponseEntity<StorageObjectDto> postObject(
            @NotNull
            @RequestParam final MultipartFile content,
            @NotNull
            @RequestParam
            @NotBlank final String group,
            @Nullable
            @RequestParam(required = false) final String encryptionKey
    );

    @NotNull
    @PutMapping(StorageObjectRequestMappingConstants.PUT_OBJECT_MAPPING)
    ResponseEntity<StorageObjectDto> putObject(
            @NotNull
            @PathVariable
            @NotBlank final String id,
            @NotNull
            @RequestBody
            @Valid final StorageObjectUpdateDto storageObjectUpdateDto
    );

    @NotNull
    @PutMapping(StorageObjectRequestMappingConstants.PUT_RAW_OBJECT_MAPPING)
    ResponseEntity<?> putRawObject(
            @NotNull
            @PathVariable
            @NotBlank final String id,
            @NotNull
            @RequestParam final MultipartFile content,
            @Nullable
            @RequestParam(required = false) final String encryptionKey
    );

    @NotNull
    @DeleteMapping(StorageObjectRequestMappingConstants.DELETE_OBJECT_MAPPING)
    ResponseEntity<?> deleteObject(
            @NotNull
            @PathVariable
            @NotBlank final String id
    );
}