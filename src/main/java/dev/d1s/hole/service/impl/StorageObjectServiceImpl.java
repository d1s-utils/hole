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
import dev.d1s.hole.service.MetadataService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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

    private MetadataService metadataService;

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
    public RawStorageObjectMetadata readRawObject(@NotNull final String id, @Nullable final String encryptionKey, @NotNull final OutputStream out) throws NotFoundException, CryptorException {
        final var object = storageObjectServiceImpl.getObject(id, false).entity();
        final var objectName = object.getName();

        final var parts = objectStorageAccessor.findAllAssociatingParts(object);

        final var contentType = new AtomicReference<String>(null);
        final var cryptorException = new AtomicReference<CryptorException>(null);

        parts.forEach(p -> {
            try {
                var bytes = Files.readAllBytes(p.path());

                final var encrypted = object.isEncrypted();

                if (encrypted && encryptionKey != null) {
                    bytes = jnCryptor.decryptData(bytes, encryptionKey.toCharArray());
                } else if (encrypted) {
                    throw new BadRequestException(EncryptionErrorConstants.ENCRYPTION_KEY_NOT_PRESENT_ERROR);
                }

                if (contentType.get() == null) {
                    contentType.set(tika.detect(bytes, objectName));
                }

                out.write(bytes);
            } catch (IOException | CryptorException e) {
                if (e instanceof CryptorException casted) {
                    cryptorException.set(casted);
                } else {
                    throw new RuntimeException(e);
                }
            }
        });

        if (cryptorException.get() != null) {
            throw cryptorException.get();
        }

        final var objectAccess = storageObjectAccessRepository.save(new StorageObjectAccess(object));

        object.getStorageObjectAccesses().add(objectAccess);

        storageObjectRepository.save(object);

        publisher.publish(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_ACCESSED_GROUP,
                object.getId(),
                storageObjectAccessDtoConverter.convertToDto(objectAccess)
        );

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
                        group,
                        encryptionKey != null,
                        new HashSet<>()
                )
        );

        this.writeObject(object, encryptionKey, content);

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

        return new EntityWithDto<>(savedObject, objectDto);
    }

    @Override
    public void overwriteObject(@NotNull final String id, @NotNull final MultipartFile content, @Nullable final String encryptionKey) throws IOException, CryptorException {
        final var object = storageObjectServiceImpl.getObject(id, false).entity();

        final var encryptionUsed = encryptionKey != null;

        if (object.isEncrypted() != encryptionUsed) {
            object.setEncrypted(encryptionUsed);
        }

        publisher.publish(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_OVERWRITTEN_GROUP,
                object.getId(),
                null
        );

        this.deleteObject(object);

        this.writeObject(object, encryptionKey, content);
    }

    @Override
    @Transactional
    public void deleteObject(@NotNull final String id) throws NotFoundException {
        final var object = storageObjectServiceImpl.getObject(id, false).entity();

        storageObjectRepository.delete(object);

        this.deleteObject(object);

        publisher.publish(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_DELETED_GROUP,
                object.getId(),
                storageObjectDtoConverter.convertToDto(object)
        );
    }

    private void writeObject(final StorageObject object, final String encryptionKey, final MultipartFile content) throws CryptorException, IOException {
        var currentPartId = 0;
        var out = this.newOutputStream(object, currentPartId);

        final var byteArrayBuilder = new ByteArrayBuilder();

        try (final var in = content.getInputStream()) {
            while (true) {
                final var b = in.read();

                if (b == -1) {
                    this.flushByteArrayBuilder(byteArrayBuilder, encryptionKey, out);
                    break;
                }

                byteArrayBuilder.append(b);

                if (byteArrayBuilder.size() == StorageObjectPart.SIZE) {
                    this.flushByteArrayBuilder(byteArrayBuilder, encryptionKey, out);
                    out = this.newOutputStream(object, ++currentPartId);
                }
            }
        } finally {
            out.close();
            byteArrayBuilder.close();
        }
    }

    private void flushByteArrayBuilder(final ByteArrayBuilder byteArrayBuilder, final String encryptionKey, final OutputStream out) throws CryptorException, IOException {
        var bytes = byteArrayBuilder.toByteArray();

        if (encryptionKey != null) {
            bytes = jnCryptor.encryptData(bytes, encryptionKey.toCharArray());
        }

        out.write(bytes);
        out.close();

        byteArrayBuilder.reset();
    }

    private OutputStream newOutputStream(final StorageObject object, final int partId) throws IOException {
        return Files.newOutputStream(objectStorageAccessor.resolveAsPart(object, partId).path());
    }

    private void deleteObject(final StorageObject object) {
        objectStorageAccessor.findAllAssociatingParts(object).forEach(p -> {
            try {
                Files.delete(p.path());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
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

    @Autowired
    public void setMetadataService(final MetadataService metadataService) {
        this.metadataService = metadataService;
    }
}