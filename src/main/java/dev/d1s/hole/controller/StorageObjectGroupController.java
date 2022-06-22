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

import dev.d1s.hole.constant.mapping.storageObject.StorageObjectGroupRequestMappingConstants;
import dev.d1s.hole.dto.storageObject.StorageObjectGroupAlterationDto;
import dev.d1s.hole.dto.storageObject.StorageObjectGroupDto;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Set;

@Validated
public interface StorageObjectGroupController {

    @GetMapping(StorageObjectGroupRequestMappingConstants.GET_GROUP_MAPPING)
    ResponseEntity<StorageObjectGroupDto> getGroup(
            @NotNull
            @PathVariable
            @NotBlank final String id
    );

    @GetMapping(StorageObjectGroupRequestMappingConstants.GET_ALL_GROUPS_MAPPING)
    ResponseEntity<Set<StorageObjectGroupDto>> getAllGroups();

    @PostMapping(StorageObjectGroupRequestMappingConstants.POST_GROUP_MAPPING)
    ResponseEntity<StorageObjectGroupDto> postGroup(
            @NotNull
            @RequestBody
            @Valid final StorageObjectGroupAlterationDto alteration
    );

    @PutMapping(StorageObjectGroupRequestMappingConstants.PUT_GROUP_MAPPING)
    ResponseEntity<StorageObjectGroupDto> putGroup(
            @NotNull
            @PathVariable
            @NotBlank final String id,
            @NotNull
            @RequestBody
            @Valid final StorageObjectGroupAlterationDto alteration
    );

    @DeleteMapping(StorageObjectGroupRequestMappingConstants.DELETE_GROUP_MAPPING)
    ResponseEntity<?> deleteGroup(
            @NotNull
            @PathVariable
            @NotBlank final String id
    );
}
