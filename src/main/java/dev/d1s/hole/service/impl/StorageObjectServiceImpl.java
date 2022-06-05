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

import dev.d1s.advice.exception.BadRequestException;
import dev.d1s.advice.exception.NotFoundException;
import dev.d1s.hole.accessor.ObjectStorageAccessor;
import dev.d1s.hole.constant.error.EncryptionErrorConstants;
import dev.d1s.hole.constant.error.StorageObjectErrorConstants;
import dev.d1s.hole.constant.longPolling.StorageObjectLongPollingConstants;
import dev.d1s.hole.dto.StorageObjectAccessDto;
import dev.d1s.hole.dto.StorageObjectDto;
import dev.d1s.hole.dto.common.EntityWithDto;
import dev.d1s.hole.dto.common.EntityWithDtoSet;
import dev.d1s.hole.entity.RawStorageObject;
import dev.d1s.hole.entity.StorageObject;
import dev.d1s.hole.entity.StorageObjectAccess;
import dev.d1s.hole.repository.StorageObjectAccessRepository;
import dev.d1s.hole.repository.StorageObjectRepository;
import dev.d1s.hole.service.StorageObjectService;
import dev.d1s.hole.util.FileNameUtils;
import dev.d1s.lp.server.publisher.AsyncLongPollingEventPublisher;
import dev.d1s.teabag.dto.DtoConverter;
import dev.d1s.teabag.dto.DtoSetConverterFacade;
import dev.d1s.teabag.dto.util.DtoConverterExtKt;
import dev.d1s.teabag.dto.util.DtoSetConverterFacadeExtKt;
import org.apache.tika.Tika;
import org.cryptonode.jncryptor.CryptorException;
import org.cryptonode.jncryptor.JNCryptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@Service
public class StorageObjectServiceImpl implements StorageObjectService, InitializingBean {

    private StorageObjectRepository storageObjectRepository;

    private StorageObjectAccessRepository storageObjectAccessRepository;

    private DtoConverter<StorageObjectDto, StorageObject> storageObjectDtoConverter;

    private DtoConverter<StorageObjectAccessDto, StorageObjectAccess> storageObjectAccessDtoConverter;

    private DtoSetConverterFacade<StorageObjectDto, StorageObject> storageObjectDtoSetConverter;

    private ObjectStorageAccessor objectStorageAccessor;

    private Tika tika;

    private AsyncLongPollingEventPublisher publisher;

    private JNCryptor jnCryptor;

    @Lazy
    private StorageObjectServiceImpl storageObjectServiceImpl;

    @NotNull
    @Override
    @Transactional(readOnly = true)
    public EntityWithDto<StorageObject, StorageObjectDto> getObject(@NotNull final String id, final boolean requireDto) throws NotFoundException {
        final var object = storageObjectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(StorageObjectErrorConstants.STORAGE_OBJECT_NOT_FOUND_ERROR));

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
    public RawStorageObject getRawObject(@NotNull final String id, @Nullable final String encryptionKey) throws NotFoundException, IOException, CryptorException {
        final var object = storageObjectServiceImpl.getObject(id, false).entity();

        var content = Files.readAllBytes(objectStorageAccessor.resolveObjectAsPath(object));

        if (object.isEncrypted()) {
            if (encryptionKey == null) {
                throw new BadRequestException(EncryptionErrorConstants.ENCRYPTION_KEY_NOT_PRESENT_ERROR);
            }

            content = jnCryptor.decryptData(content, encryptionKey.toCharArray());
        }

        final var objectAccess = storageObjectAccessRepository.save(new StorageObjectAccess(object));

        object.getStorageObjectAccesses().add(objectAccess);

        storageObjectRepository.save(object);

        publisher.publish(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_ACCESSED_GROUP,
                object.getId(),
                storageObjectAccessDtoConverter.convertToDto(objectAccess)
        );

        final var objectName = object.getName();

        return new RawStorageObject(
                objectName,
                tika.detect(content, objectName),
                content
        );
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
    public EntityWithDto<StorageObject, StorageObjectDto> createObject(@NotNull final MultipartFile content, @NotNull final String group, @Nullable final String encryptionKey) throws IOException, CryptorException {
        final var object = storageObjectRepository.save(
                new StorageObject(
                        FileNameUtils.sanitize(content.getOriginalFilename()),
                        group
                )
        );

        this.writeObject(object, content.getBytes(), encryptionKey, objectStorageAccessor.resolveObjectAsPath(object));

        final var objectDto = storageObjectDtoConverter.convertToDto(object);

        publisher.publish(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_CREATED_GROUP,
                object.getId(),
                objectDto
        );

        return new EntityWithDto<>(object, objectDto);
    }

    @NotNull
    @Override
    public EntityWithDto<StorageObject, StorageObjectDto> updateObject(@NotNull final String id, @NotNull final StorageObject storageObject) {
        final var foundObject = storageObjectServiceImpl.getObject(id, false).entity();

        foundObject.setName(storageObject.getName());
        foundObject.setObjectGroup(storageObject.getObjectGroup());

        final var savedObject = storageObjectRepository.save(foundObject);

        final var objectDto = storageObjectDtoConverter.convertToDto(savedObject);

        publisher.publish(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_UPDATED_GROUP,
                savedObject.getId(),
                objectDto
        );

        return new EntityWithDto<>(savedObject, objectDto);
    }

    @Override
    public void overwriteObject(@NotNull final String id, @NotNull final MultipartFile content, @Nullable final String encryptionKey) throws IOException, CryptorException {
        final var object = storageObjectServiceImpl.getObject(id, false).entity();
        final var path = objectStorageAccessor.resolveObjectAsPath(object);

        publisher.publish(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_OVERWRITTEN_GROUP,
                object.getId(),
                new Object() // {}
        );

        Files.delete(path);

        this.writeObject(object, content.getBytes(), encryptionKey, path);
    }

    @Override
    @Transactional
    public void deleteObject(@NotNull final String id) throws NotFoundException, IOException {
        final var object = storageObjectServiceImpl.getObject(id, false).entity();

        Files.delete(objectStorageAccessor.resolveObjectAsPath(object));

        storageObjectRepository.delete(object);

        publisher.publish(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_DELETED_GROUP,
                object.getId(),
                storageObjectDtoConverter.convertToDto(object)
        );
    }

    private void writeObject(final StorageObject object, byte[] bytes, final String encryptionKey, final Path path) throws CryptorException, IOException {
        if (encryptionKey != null) {
            bytes = jnCryptor.encryptData(bytes, encryptionKey.toCharArray());
            object.setEncrypted(true);
        }

        FileCopyUtils.copy(bytes, Files.newOutputStream(path));
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
    public void setJnCryptor(final JNCryptor jnCryptor) {
        this.jnCryptor = jnCryptor;
    }

    @Autowired
    public void setStorageObjectServiceImpl(final StorageObjectServiceImpl storageObjectServiceImpl) {
        this.storageObjectServiceImpl = storageObjectServiceImpl;
    }
}