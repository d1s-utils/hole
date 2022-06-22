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

import dev.d1s.advice.exception.BadRequestException;
import dev.d1s.advice.exception.NotFoundException;
import dev.d1s.hole.accessor.ObjectStorageAccessor;
import dev.d1s.hole.constant.contentDisposition.ContentDispositionConstants;
import dev.d1s.hole.constant.error.EncryptionErrorConstants;
import dev.d1s.hole.constant.error.storageObject.StorageObjectErrorConstants;
import dev.d1s.hole.constant.longPolling.StorageObjectLongPollingConstants;
import dev.d1s.hole.dto.common.EntityWithDto;
import dev.d1s.hole.dto.common.EntityWithDtoSet;
import dev.d1s.hole.dto.storageObject.StorageObjectAccessDto;
import dev.d1s.hole.dto.storageObject.StorageObjectDto;
import dev.d1s.hole.entity.storageObject.StorageObject;
import dev.d1s.hole.entity.storageObject.StorageObjectAccess;
import dev.d1s.hole.repository.StorageObjectAccessRepository;
import dev.d1s.hole.repository.StorageObjectRepository;
import dev.d1s.hole.service.EncryptionService;
import dev.d1s.hole.service.LockService;
import dev.d1s.hole.service.MetadataService;
import dev.d1s.hole.service.storageObject.StorageObjectGroupService;
import dev.d1s.hole.service.storageObject.StorageObjectService;
import dev.d1s.hole.util.FileNameUtils;
import dev.d1s.lp.server.publisher.AsyncLongPollingEventPublisher;
import dev.d1s.teabag.dto.DtoConverter;
import dev.d1s.teabag.dto.DtoSetConverterFacade;
import dev.d1s.teabag.dto.util.DtoConverterExtKt;
import dev.d1s.teabag.dto.util.DtoSetConverterFacadeExtKt;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.Tika;
import org.apache.tika.utils.StringUtils;
import org.cryptonode.jncryptor.StreamIntegrityException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.BadPaddingException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

    private StorageObjectGroupService storageObjectGroupService;

    private StorageObjectServiceImpl storageObjectServiceImpl;

    @NotNull
    @Override
    @Transactional(readOnly = true)
    public EntityWithDto<StorageObject, StorageObjectDto> getObject(
            @NotNull final String id,
            final boolean requireDto
    ) {
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

    @Override
    @Transactional
    public void writeRawObjectToWeb(
            @NotNull final String id,
            @Nullable final String encryptionKey,
            @NotNull final HttpServletResponse response,
            @Nullable final String contentDisposition
    ) {
        final var object = storageObjectServiceImpl.getObject(id, false).entity();
        final var encrypted = object.isEncrypted();
        final var contentLength = object.getContentLength();

        if (encrypted && StringUtils.isBlank(encryptionKey)) {
            throw new BadRequestException(EncryptionErrorConstants.ENCRYPTION_KEY_NOT_PRESENT_ERROR);
        }

        if (contentLength == 0) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }

        final ServletOutputStream out;

        try {
            out = response.getOutputStream();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.builder(
                                contentDisposition != null
                                        ? contentDisposition
                                        : ContentDispositionConstants.DEFAULT_CONTENT_DISPOSITION_TYPE
                        )
                        .filename(object.getName())
                        .build()
                        .toString()
        );

        InputStream in = null;

        try {
            lockService.lock(id);

            in = objectStorageAccessor.createInputStream(object);

            if (encrypted) {
                in = encryptionService.createDecryptedInputStream(in, encryptionKey);
            }

            var contentTypeAndLengthSet = false;

            final var buffer = new byte[IOUtils.DEFAULT_BUFFER_SIZE];

            while (true) {
                final var read = in.read(buffer, 0, IOUtils.DEFAULT_BUFFER_SIZE);

                if (!contentTypeAndLengthSet) {
                    response.setContentType(object.getContentType());
                    response.setContentLengthLong(contentLength);

                    contentTypeAndLengthSet = true;
                }

                if (read != -1) {
                    out.write(buffer, 0, read);
                } else {
                    break;
                }
            }
        } catch (final IOException e) {
            if (e instanceof StreamIntegrityException) {
                throw encryptionService.createEncryptionException(e);
            } else if (e.getCause() instanceof BadPaddingException) {
                throw encryptionService.createEncryptionException(e.getCause());
            }

            log.warn("Failed to write to response: {}. " +
                    "Perhaps the client disconnected without waiting for the completion.", e.getMessage());
        } finally {
            objectStorageAccessor.closeInputStream(Objects.requireNonNull(in));
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
    }

    @NotNull
    @Override
    @Transactional(readOnly = true)
    public EntityWithDtoSet<StorageObject, StorageObjectDto> getAllObjects(final boolean requireDto) {
        final Set<StorageObject> objects = new HashSet<>(storageObjectRepository.findAll());

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
    public EntityWithDto<StorageObject, StorageObjectDto> createObject(
            @NotNull final MultipartFile content,
            @NotNull final String group,
            @Nullable final String encryptionKey
    ) {
        this.checkContent(content, encryptionKey);

        final var filename = FileNameUtils.sanitizeAndCheck(content.getOriginalFilename());
        final var contentSize = content.getSize();

        final StorageObject object = storageObjectRepository.save(
                new StorageObject(
                        filename,
                        storageObjectGroupService.getGroup(group, false).entity(),
                        !StringUtils.isBlank(encryptionKey),
                        this.createSha256Digest(content),
                        this.detectContentType(content, filename),
                        contentSize,
                        new HashSet<>(),
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
    public EntityWithDto<StorageObject, StorageObjectDto> updateObject(
            @NotNull final String id,
            @NotNull final StorageObject storageObject
    ) {
        final var foundObject = storageObjectServiceImpl.getObject(id, false).entity();

        metadataService.checkMetadata(storageObject);

        foundObject.setName(storageObject.getName());
        foundObject.setGroup(storageObject.getGroup());

        metadataService.transferMetadata(storageObject, foundObject);

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
    public void overwriteObject(
            @NotNull final String id,
            @NotNull final MultipartFile content,
            @Nullable final String encryptionKey
    ) {
        this.checkContent(content, encryptionKey);

        final var object = storageObjectServiceImpl.getObject(id, false).entity();

        final var encryptionUsed = !StringUtils.isBlank(encryptionKey);

        final var digest = this.createSha256Digest(content);

        final var sanitizedFileName = FileNameUtils.sanitizeAndCheck(content.getOriginalFilename());

        final var contentType = this.detectContentType(content, sanitizedFileName);

        final var contentLength = content.getSize();

        try {
            lockService.lock(object);

            var needsUpdate = false;

            if (encryptionUsed != object.isEncrypted()) {
                object.setEncrypted(encryptionUsed);
                needsUpdate = true;
            }

            if (!digest.equals(object.getDigest())) {
                object.setDigest(digest);
                needsUpdate = true;
            }

            if (!sanitizedFileName.equals(object.getName())) {
                object.setName(sanitizedFileName);
                needsUpdate = true;
            }

            if (!contentType.equals(object.getContentType())) {
                object.setContentType(contentType);
                needsUpdate = true;
            }

            if (contentLength != object.getContentLength()) {
                object.setContentLength(contentLength);
                needsUpdate = true;
            }

            if (needsUpdate) {
                storageObjectRepository.save(object);
            }

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
        final var object = storageObjectServiceImpl.getObject(id, true);
        final var entity = object.entity();

        storageObjectRepository.delete(entity);

        try {
            lockService.lock(entity);

            objectStorageAccessor.deleteObject(entity);
        } finally {
            lockService.unlock(entity);
            lockService.removeLock(entity);
        }

        publisher.publish(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_DELETED_GROUP,
                entity.getId(),
                object.dto()
        );

        log.debug("Deleted storage object: {}", object);
    }

    private void writeObject(
            final StorageObject object,
            final String encryptionKey,
            final MultipartFile content
    ) {
        var out = objectStorageAccessor.createOutputStream(object);

        try (final var in = content.getInputStream()) {
            if (!StringUtils.isBlank(encryptionKey)) {
                out = encryptionService.createEncryptedOutputStream(out, encryptionKey);
            }

            IOUtils.copyLarge(in, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            objectStorageAccessor.closeOutputStream(out);
        }
    }

    private String createSha256Digest(final MultipartFile content) {
        try (final var in = content.getInputStream()) {
            return DigestUtils.sha256Hex(in);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String detectContentType(final MultipartFile content, final String filename) {
        try {
            return tika.detect(content.getInputStream(), filename);
        } catch (IOException e) {
            objectStorageAccessor.processIoException(e);

            // impossible to reach this point
            throw new RuntimeException(e);
        }
    }

    private void checkContent(final MultipartFile content, final String encryptionKey) {
        if (content.getSize() == 0 && encryptionKey != null) {
            throw new BadRequestException(EncryptionErrorConstants.NOTHING_TO_ENCRYPT_ERROR);
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
    public void setStorageObjectDtoConverter(
            final DtoConverter<StorageObjectDto, StorageObject> storageObjectDtoConverter) {
        this.storageObjectDtoConverter = storageObjectDtoConverter;
    }

    @Autowired
    public void setStorageObjectAccessDtoConverter(
            final DtoConverter<StorageObjectAccessDto, StorageObjectAccess> storageObjectAccessDtoConverter) {
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

    @Autowired
    public void setStorageObjectGroupService(final StorageObjectGroupService storageObjectGroupService) {
        this.storageObjectGroupService = storageObjectGroupService;
    }

    @Lazy
    @Autowired
    public void setStorageObjectServiceImpl(final StorageObjectServiceImpl storageObjectServiceImpl) {
        this.storageObjectServiceImpl = storageObjectServiceImpl;
    }
}