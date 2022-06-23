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

import dev.d1s.hole.controller.StorageObjectGroupController;
import dev.d1s.hole.dto.storageObject.StorageObjectGroupAlterationDto;
import dev.d1s.hole.dto.storageObject.StorageObjectGroupDto;
import dev.d1s.hole.entity.storageObject.StorageObjectGroup;
import dev.d1s.hole.factory.LocationFactory;
import dev.d1s.hole.service.storageObject.StorageObjectGroupService;
import dev.d1s.security.configuration.annotation.Secured;
import dev.d1s.teabag.dto.DtoConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;

@RestController
public class StorageObjectGroupControllerImpl implements StorageObjectGroupController {

    private StorageObjectGroupService storageObjectGroupService;

    private LocationFactory locationFactory;

    private DtoConverter<StorageObjectGroupAlterationDto, StorageObjectGroup> storageObjectGroupAlterationDtoConverter;

    @NotNull
    @Secured
    @Override

    public ResponseEntity<StorageObjectGroupDto> getGroup(@NotNull final String id) {
        return ResponseEntity.ok(storageObjectGroupService.getGroup(id, true).dto());
    }

    @NotNull
    @Secured
    @Override
    public ResponseEntity<Set<StorageObjectGroupDto>> getAllGroups() {
        return ResponseEntity.ok(storageObjectGroupService.getAllGroups(true).dtos());
    }

    @NotNull
    @Secured
    @Override
    public ResponseEntity<StorageObjectGroupDto> postGroup(@NotNull final StorageObjectGroupAlterationDto alteration) {
        final var createdGroup = storageObjectGroupService.createGroup(
                storageObjectGroupAlterationDtoConverter.convertToEntity(alteration)
        ).dto();

        return ResponseEntity.created(
                locationFactory.createLocation(
                        Objects.requireNonNull(createdGroup).id()
                )
        ).body(createdGroup);
    }

    @NotNull
    @Secured
    @Override
    public ResponseEntity<StorageObjectGroupDto> putGroup(@NotNull final String id, @NotNull final StorageObjectGroupAlterationDto alteration) {
        return ResponseEntity.ok(
                storageObjectGroupService.updateGroup(
                        id,
                        storageObjectGroupAlterationDtoConverter.convertToEntity(alteration)
                ).dto()
        );
    }

    @NotNull
    @Secured
    @Override
    public ResponseEntity<?> deleteGroup(@NotNull final String id) {
        storageObjectGroupService.deleteGroup(id);

        return ResponseEntity.noContent().build();
    }

    @Autowired
    public void setStorageObjectGroupService(final StorageObjectGroupService storageObjectGroupService) {
        this.storageObjectGroupService = storageObjectGroupService;
    }

    @Autowired
    public void setLocationFactory(final LocationFactory locationFactory) {
        this.locationFactory = locationFactory;
    }

    @Autowired
    public void setStorageObjectGroupAlterationDtoConverter(final DtoConverter<StorageObjectGroupAlterationDto, StorageObjectGroup> storageObjectGroupAlterationDtoConverter) {
        this.storageObjectGroupAlterationDtoConverter = storageObjectGroupAlterationDtoConverter;
    }
}
