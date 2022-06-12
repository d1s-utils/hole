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

package dev.d1s.hole.service.impl;

import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import dev.d1s.advice.exception.BadRequestException;
import dev.d1s.advice.exception.NotFoundException;
import dev.d1s.hole.accessor.ObjectStorageAccessor;
import dev.d1s.hole.constant.error.EncryptionErrorConstants;
import dev.d1s.hole.constant.error.MetadataErrorConstants;
import dev.d1s.hole.constant.error.StorageObjectErrorConstants;
import dev.d1s.hole.constant.longPolling.StorageObjectLongPollingConstants;
import dev.d1s.hole.dto.common.EntityWithDto;
import dev.d1s.hole.dto.common.EntityWithDtoSet;
import dev.d1s.hole.dto.storageObject.StorageObjectAccessDto;
import dev.d1s.hole.dto.storageObject.StorageObjectDto;
import dev.d1s.hole.entity.storageObject.RawStorageObjectMetadata;
import dev.d1s.hole.entity.storageObject.StorageObject;
import dev.d1s.hole.entity.storageObject.StorageObjectAccess;
import dev.d1s.hole.entity.storageObject.StorageObjectPart;
import dev.d1s.hole.repository.StorageObjectAccessRepository;
import dev.d1s.hole.repository.StorageObjectRepository;
import dev.d1s.hole.service.EncryptionService;
import dev.d1s.hole.service.LockService;
import dev.d1s.hole.service.MetadataService;
import dev.d1s.hole.service.StorageObjectService;
import dev.d1s.hole.util.FileNameUtils;
import dev.d1s.lp.server.publisher.AsyncLongPollingEventPublisher;
import dev.d1s.teabag.dto.DtoConverter;
import dev.d1s.teabag.dto.DtoSetConverterFacade;
import dev.d1s.teabag.dto.util.DtoConverterExtKt;
import dev.d1s.teabag.dto.util.DtoSetConverterFacadeExtKt;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.Tika;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class StorageObjectServiceImpl implements StorageObjectService, InitializingBean {

    private static final Logger log = LogManager.getLogger();

    private StorageObjectRepository storageObjectRepository;

    private StorageObjectAccessRepository storageObjectAccessRepository;

    private DtoConverter<StorageObjectDto, StorageObject> storageObjectDtoConverter;

    private DtoConverter<StorageObjectAccessDto, StorageObjectAccess> storageObjectAccessDtoConverter;

    private DtoSetConverterFacade<StorageObjectDto, StorageObject> storageObjectDtoSetConverter;

    private ObjectStorageAccessor objectStorageAccessor;

    private Tika tika;

    private AsyncLongPollingEventPublisher publisher;

    private EncryptionService encryptionService;

    private LockService lockService;

    private MetadataService metadataService;

    private StorageObjectServiceImpl storageObjectServiceImpl;

    @NotNull
    @Override
    @Transactional(readOnly = true)
    public EntityWithDto<StorageObject, StorageObjectDto> getObject(@NotNull final String id, final boolean requireDto) {
        final var object = storageObjectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(StorageObjectErrorConstants.STORAGE_OBJECT_NOT_FOUND_ERROR));

        log.debug("Found storage object: {}", object);

        return new EntityWithDto<>(
                object,
                DtoConverterExtKt.convertToDtoIf(
                        this.storageObjectDtoConverter,
                        object,
                        requireDto
                )
        );
    }

    @NotNull
    @Override
    @Transactional
    public RawStorageObjectMetadata readRawObject(@NotNull final String id, @Nullable final String encryptionKey, @NotNull final OutputStream out) {
        final var object = storageObjectServiceImpl.getObject(id, false).entity();
        final var objectName = object.getName();

        final AtomicReference<String> contentType = new AtomicReference<>(null);

        try {
            lockService.lock(id);

            final var parts = objectStorageAccessor.findAllAssociatingParts(object);

            parts.forEach(p -> {
                try {
                    var bytes = objectStorageAccessor.readPartBytes(p);

                    final var encrypted = object.isEncrypted();

                    if (encrypted && encryptionKey != null) {
                        bytes = encryptionService.decrypt(bytes, encryptionKey);
                    } else if (encrypted) {
                        throw new BadRequestException(EncryptionErrorConstants.ENCRYPTION_KEY_NOT_PRESENT_ERROR);
                    }

                    if (contentType.get() == null) {
                        contentType.set(tika.detect(bytes, objectName));
                    }

                    out.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            lockService.unlock(id);
        }

        final var objectAccess = storageObjectAccessRepository.save(new StorageObjectAccess(object));

        object.getStorageObjectAccesses().add(objectAccess);

        storageObjectRepository.save(object);

        publisher.publish(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_ACCESSED_GROUP,
                object.getId(),
                storageObjectAccessDtoConverter.convertToDto(objectAccess)
        );

        log.debug("Read raw storage object: {}", object);

        return new RawStorageObjectMetadata(objectName, contentType.get());
    }

    @NotNull
    @Override
    @Transactional(readOnly = true)
    public EntityWithDtoSet<StorageObject, StorageObjectDto> getAllObjects(@Nullable final String group, final boolean requireDto) {
        final Set<StorageObject> objects;

        if (group != null) {
            objects = storageObjectRepository.findByObjectGroup(group);
        } else {
            objects = new HashSet<>(storageObjectRepository.findAll());
        }

        log.debug("Found storage objects: {}", objects);

        return new EntityWithDtoSet<>(
                objects,
                DtoSetConverterFacadeExtKt.convertToDtoSetIf(
                        this.storageObjectDtoSetConverter,
                        objects,
                        requireDto
                )
        );
    }

    @NotNull
    @Override
    @Transactional
    public EntityWithDto<StorageObject, StorageObjectDto> createObject(@NotNull final MultipartFile content, @NotNull final String group, @Nullable final String encryptionKey) {
        final var object = storageObjectRepository.save(
                new StorageObject(
                        FileNameUtils.sanitize(content.getOriginalFilename()),
                        group,
                        encryptionKey != null,
                        this.createSha256Digest(content),
                        new HashSet<>()
                )
        );

        try {
            lockService.lock(object);

            this.writeObject(object, encryptionKey, content);
        } finally {
            lockService.unlock(object);
        }

        final var objectDto = storageObjectDtoConverter.convertToDto(object);

        publisher.publish(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_CREATED_GROUP,
                object.getId(),
                objectDto
        );

        log.debug("Created storage object: {}", object);

        return new EntityWithDto<>(object, objectDto);
    }

    @NotNull
    @Override
    public EntityWithDto<StorageObject, StorageObjectDto> updateObject(@NotNull final String id, @NotNull final StorageObject storageObject) {
        final var foundObject = storageObjectServiceImpl.getObject(id, false).entity();

        final var propertySet = new HashSet<String>();

        for (final var p : storageObject.getMetadata()) {
            if (!propertySet.add(p.getProperty())) {
                throw new BadRequestException(MetadataErrorConstants.DUPLICATE_METADATA_PROPERTY_ERROR);
            }
        }

        foundObject.setName(storageObject.getName());
        foundObject.setObjectGroup(storageObject.getObjectGroup());
        foundObject.setMetadata(
                storageObject.getMetadata()
                        .stream()
                        .map(metadataProperty ->
                                metadataService.findMetadataPropertyByPropertyNameAndValue(
                                        metadataProperty.getProperty(),
                                        metadataProperty.getValue()
                                ).orElse(metadataService.saveMetadataProperty(metadataProperty))
                        )
                        .collect(Collectors.toSet())
        );

        final var savedObject = storageObjectRepository.save(foundObject);

        final var objectDto = storageObjectDtoConverter.convertToDto(savedObject);

        publisher.publish(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_UPDATED_GROUP,
                savedObject.getId(),
                objectDto
        );

        log.debug("Updated storage object: {}", savedObject);

        return new EntityWithDto<>(savedObject, objectDto);
    }

    @Override
    @Transactional
    public void overwriteObject(@NotNull final String id, @NotNull final MultipartFile content, @Nullable final String encryptionKey) {
        final var object = storageObjectServiceImpl.getObject(id, false).entity();

        final var encryptionUsed = encryptionKey != null;

        final var digest = this.createSha256Digest(content);

        if (object.isEncrypted() != encryptionUsed || !digest.equals(object.getDigest())) {
            object.setEncrypted(encryptionUsed);
            object.setDigest(digest);

            storageObjectRepository.save(object);
        }

        try {
            lockService.lock(object);

            objectStorageAccessor.deleteObject(object);

            this.writeObject(object, encryptionKey, content);
        } finally {
            lockService.unlock(object);
        }

        publisher.publish(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_OVERWRITTEN_GROUP,
                object.getId(),
                null
        );

        log.debug("Overwrote storage object: {}", object);
    }

    @Override
    @Transactional
    public void deleteObject(@NotNull final String id) {
        final var object = storageObjectServiceImpl.getObject(id, false).entity();

        storageObjectRepository.delete(object);

        try {
            lockService.lock(object);

            objectStorageAccessor.deleteObject(object);
        } finally {
            lockService.unlock(object);
            lockService.removeLock(object);
        }

        publisher.publish(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_DELETED_GROUP,
                object.getId(),
                storageObjectDtoConverter.convertToDto(object)
        );

        log.debug("Deleted storage object: {}", object);
    }

    private void writeObject(final StorageObject object, final String encryptionKey, final MultipartFile content) {
        var currentPartId = 0;
        var out = objectStorageAccessor.createOutputStream(object, currentPartId);

        try (final var byteArrayBuilder = new ByteArrayBuilder(); final var in = content.getInputStream()) {
            while (true) {
                final var b = in.read();

                if (b == -1) {
                    this.flushByteArrayBuilder(byteArrayBuilder, encryptionKey, out);
                    break;
                }

                byteArrayBuilder.append(b);

                if (byteArrayBuilder.size() == StorageObjectPart.SIZE) {
                    this.flushByteArrayBuilder(byteArrayBuilder, encryptionKey, out);
                    out = objectStorageAccessor.createOutputStream(object, ++currentPartId);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            objectStorageAccessor.closeOutputStream(out);
        }
    }

    private void flushByteArrayBuilder(final ByteArrayBuilder byteArrayBuilder, final String encryptionKey, final OutputStream out) {
        var bytes = byteArrayBuilder.toByteArray();

        if (encryptionKey != null) {
            bytes = encryptionService.encrypt(bytes, encryptionKey);
        }

        objectStorageAccessor.writeToOutputStream(out, bytes);
        objectStorageAccessor.closeOutputStream(out);

        byteArrayBuilder.reset();
    }

    private String createSha256Digest(final MultipartFile content) {
        try (final var in = content.getInputStream()) {
            return DigestUtils.sha256Hex(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterPropertiesSet() {
        this.storageObjectDtoSetConverter = DtoConverterExtKt.converterForSet(this.storageObjectDtoConverter);
    }

    @Autowired
    public void setStorageObjectRepository(final StorageObjectRepository storageObjectRepository) {
        this.storageObjectRepository = storageObjectRepository;
    }

    @Autowired
    public void setStorageObjectAccessRepository(final StorageObjectAccessRepository storageObjectAccessRepository) {
        this.storageObjectAccessRepository = storageObjectAccessRepository;
    }

    @Autowired
    public void setStorageObjectDtoConverter(final DtoConverter<StorageObjectDto, StorageObject> storageObjectDtoConverter) {
        this.storageObjectDtoConverter = storageObjectDtoConverter;
    }

    @Autowired
    public void setStorageObjectAccessDtoConverter(final DtoConverter<StorageObjectAccessDto, StorageObjectAccess> storageObjectAccessDtoConverter) {
        this.storageObjectAccessDtoConverter = storageObjectAccessDtoConverter;
    }

    @Autowired
    public void setObjectStorageAccessor(final ObjectStorageAccessor objectStorageAccessor) {
        this.objectStorageAccessor = objectStorageAccessor;
    }

    @Autowired
    public void setTika(final Tika tika) {
        this.tika = tika;
    }

    @Autowired
    public void setPublisher(final AsyncLongPollingEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Autowired
    public void setEncryptionService(final EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Autowired
    public void setLockService(final LockService lockService) {
        this.lockService = lockService;
    }

    @Autowired
    public void setMetadataService(final MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @Lazy
    @Autowired
    public void setStorageObjectServiceImpl(final StorageObjectServiceImpl storageObjectServiceImpl) {
        this.storageObjectServiceImpl = storageObjectServiceImpl;
    }
}