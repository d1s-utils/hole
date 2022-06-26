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

package dev.d1s.hole.service.impl.storageObject;

import dev.d1s.advice.exception.NotFoundException;
import dev.d1s.advice.exception.UnprocessableEntityException;
import dev.d1s.hole.constant.error.storageObject.StorageObjectGroupErrorConstants;
import dev.d1s.hole.constant.longPolling.StorageObjectGroupLongPollingConstants;
import dev.d1s.hole.dto.common.EntityWithDto;
import dev.d1s.hole.dto.common.EntityWithDtoSet;
import dev.d1s.hole.dto.storageObject.StorageObjectGroupDto;
import dev.d1s.hole.entity.storageObject.StorageObjectGroup;
import dev.d1s.hole.repository.StorageObjectGroupRepository;
import dev.d1s.hole.service.MetadataService;
import dev.d1s.hole.service.storageObject.StorageObjectGroupService;
import dev.d1s.lp.server.publisher.AsyncLongPollingEventPublisher;
import dev.d1s.teabag.dto.DtoConverter;
import dev.d1s.teabag.dto.DtoSetConverterFacade;
import dev.d1s.teabag.dto.util.DtoConverterExtKt;
import dev.d1s.teabag.dto.util.DtoSetConverterFacadeExtKt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
public class StorageObjectGroupServiceImpl implements StorageObjectGroupService, InitializingBean {

    private static final Logger log = LogManager.getLogger();

    private StorageObjectGroupRepository storageObjectGroupRepository;

    private DtoConverter<StorageObjectGroupDto, StorageObjectGroup> storageObjectGroupDtoConverter;

    private DtoSetConverterFacade<StorageObjectGroupDto, StorageObjectGroup> storageObjectGroupDtoSetConverter;

    private AsyncLongPollingEventPublisher publisher;

    private MetadataService metadataService;

    private StorageObjectGroupServiceImpl storageObjectGroupServiceImpl;

    @NotNull
    @Override
    @Transactional(readOnly = true)
    public EntityWithDto<StorageObjectGroup, StorageObjectGroupDto> getGroup(@NotNull final String id, final boolean requireDto) {
        final var group = storageObjectGroupRepository.findById(id)
                .orElseGet(() -> storageObjectGroupRepository.findByName(id)
                        .orElseThrow(() -> new NotFoundException(StorageObjectGroupErrorConstants.STORAGE_OBJECT_GROUP_NOT_FOUND_ERROR.formatted(id)))
                );

        log.debug("Found object group: {}", group);

        return new EntityWithDto<>(
                group,
                DtoConverterExtKt.convertToDtoIf(storageObjectGroupDtoConverter, group, requireDto)
        );
    }

    @NotNull
    @Override
    @Transactional(readOnly = true)
    public EntityWithDtoSet<StorageObjectGroup, StorageObjectGroupDto> getAllGroups(final boolean requireDto) {
        final var groups = new HashSet<>(storageObjectGroupRepository.findAll());

        log.debug("Found object groups: {}", groups);

        return new EntityWithDtoSet<>(
                groups,
                DtoSetConverterFacadeExtKt.convertToDtoSetIf(storageObjectGroupDtoSetConverter, groups, requireDto)
        );
    }

    @NotNull
    @Override
    @Transactional
    public EntityWithDto<StorageObjectGroup, StorageObjectGroupDto> createGroup(@NotNull final StorageObjectGroup group) {
        metadataService.checkMetadata(group);

        this.checkGroupName(group, false);

        final var savedGroup = storageObjectGroupRepository.save(group);

        final var groupDto = storageObjectGroupDtoConverter.convertToDto(savedGroup);

        publisher.publish(
                StorageObjectGroupLongPollingConstants.OBJECT_GROUP_CREATED,
                savedGroup.getId(),
                groupDto
        );

        log.debug("Created object group: {}", savedGroup);

        return new EntityWithDto<>(savedGroup, groupDto);
    }

    @NotNull
    @Override
    @Transactional
    public EntityWithDto<StorageObjectGroup, StorageObjectGroupDto> updateGroup(@NotNull final String id, @NotNull final StorageObjectGroup group) {
        final var foundGroup = storageObjectGroupServiceImpl.getGroup(id, false).entity();

        metadataService.checkMetadata(group);

        this.checkGroupName(group, true);

        foundGroup.setName(group.getName());

        metadataService.transferMetadata(group, foundGroup);

        final var savedGroup = storageObjectGroupRepository.save(foundGroup);

        final var groupDto = storageObjectGroupDtoConverter.convertToDto(savedGroup);

        publisher.publish(
                StorageObjectGroupLongPollingConstants.OBJECT_GROUP_UPDATED,
                savedGroup.getId(),
                groupDto
        );

        log.debug("Updated object group: {}", savedGroup);

        return new EntityWithDto<>(savedGroup, groupDto);
    }

    @Override
    @Transactional
    public void deleteGroup(@NotNull final String id) {
        final var group = storageObjectGroupServiceImpl.getGroup(id, true);
        final var entity = group.entity();

        storageObjectGroupRepository.delete(entity);

        publisher.publish(
                StorageObjectGroupLongPollingConstants.OBJECT_GROUP_DELETED,
                entity.getId(),
                group.dto()
        );

        log.debug("Deleted storage object: {}", group);
    }

    @Override
    public void afterPropertiesSet() {
        this.storageObjectGroupDtoSetConverter = DtoConverterExtKt.converterForSet(
                this.storageObjectGroupDtoConverter
        );
    }

    @Autowired
    public void setStorageObjectGroupRepository(final StorageObjectGroupRepository storageObjectGroupRepository) {
        this.storageObjectGroupRepository = storageObjectGroupRepository;
    }

    @Autowired
    public void setStorageObjectGroupDtoConverter(final DtoConverter<StorageObjectGroupDto, StorageObjectGroup> storageObjectGroupDtoConverter) {
        this.storageObjectGroupDtoConverter = storageObjectGroupDtoConverter;
    }

    @Autowired
    public void setPublisher(final AsyncLongPollingEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Autowired
    public void setMetadataService(final MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @Lazy
    @Autowired
    public void setStorageObjectGroupServiceImpl(final StorageObjectGroupServiceImpl storageObjectGroupServiceImpl) {
        this.storageObjectGroupServiceImpl = storageObjectGroupServiceImpl;
    }

    private void checkGroupName(final StorageObjectGroup group, final boolean updateOperation) {
        final var foundGroup = storageObjectGroupRepository.findByName(group.getName());

        if (foundGroup.isPresent() && !(updateOperation && foundGroup.get().getName().equals(group.getName()))) {
            throw new UnprocessableEntityException(StorageObjectGroupErrorConstants.STORAGE_OBJECT_GROUP_NAME_ALREADY_EXISTS_ERROR);
        }
    }
}
