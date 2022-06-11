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

package dev.d1s.hole.accessor.impl;

import dev.d1s.hole.accessor.ObjectStorageAccessor;
import dev.d1s.hole.constant.error.StorageErrorConstants;
import dev.d1s.hole.entity.storageObject.StorageObject;
import dev.d1s.hole.entity.storageObject.StorageObjectPart;
import dev.d1s.hole.exception.storage.IllegalStorageRootException;
import dev.d1s.hole.exception.storage.StorageObjectAccessException;
import dev.d1s.hole.properties.StorageConfigurationProperties;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class ObjectStorageAccessorImpl implements ObjectStorageAccessor, InitializingBean {

    private StorageConfigurationProperties properties;

    private ObjectStorageAccessorImpl objectStorageAccessorImpl;

    @NotNull
    @Override
    public Set<StorageObjectPart> findAllAssociatingParts(@NotNull StorageObject object) {
        final var result = new HashSet<StorageObjectPart>();

        var currentPartId = 0;

        while (true) {
            final var partPath = this.getPath(object, currentPartId);

            if (Files.exists(partPath)) {
                result.add(new StorageObjectPart(currentPartId, Objects.requireNonNull(object.getId()), partPath));
                currentPartId++;
            } else {
                break;
            }
        }

        return result;
    }

    @Override
    public byte[] readPartBytes(@NotNull StorageObjectPart part) {
        try {
            return Files.readAllBytes(this.getPath(part.objectId(), part.partId()));
        } catch (IOException e) {
            throw new StorageObjectAccessException();
        }
    }

    @Override
    public @NotNull OutputStream createOutputStream(@NotNull StorageObject object, int partId) {
        try {
            return Files.newOutputStream(this.getPath(object, partId));
        } catch (IOException e) {
            throw new StorageObjectAccessException();
        }
    }

    @Override
    public void writeToOutputStream(@NotNull OutputStream out, byte[] bytes) {
        try {
            out.write(bytes);
        } catch (IOException e) {
            throw new StorageObjectAccessException();
        }
    }

    @Override
    public void closeOutputStream(@NotNull OutputStream out) {
        try {
            out.close();
        } catch (IOException e) {
            throw new StorageObjectAccessException();
        }
    }

    @Override
    public void deleteObject(@NotNull StorageObject object) {
        objectStorageAccessorImpl.findAllAssociatingParts(object).forEach(p -> {
            try {
                Files.delete(this.getPath(object, p.partId()));
            } catch (IOException e) {
                throw new StorageObjectAccessException();
            }
        });
    }

    @Override
    public void afterPropertiesSet() {
        final var path = Paths.get(properties.getRoot());

        if (!path.isAbsolute()) {
            throw new IllegalStorageRootException(StorageErrorConstants.STORAGE_PATH_NOT_ABSOLUTE_ERRROR);
        }

        if (!Files.exists(path)) {
            throw new IllegalStorageRootException(StorageErrorConstants.STORAGE_PATH_DOES_NOT_EXIST_ERROR);
        }

        if (!Files.isDirectory(path)) {
            throw new IllegalStorageRootException(StorageErrorConstants.STORAGE_PATH_NOT_A_DIRECTORY_ERROR);
        }

        if (!Files.isReadable(path) || !Files.isWritable(path)) {
            throw new IllegalStorageRootException(StorageErrorConstants.STORAGE_PATH_NOT_READABLE_OR_WRITABLE_ERROR);
        }
    }

    @Autowired
    public void setProperties(final StorageConfigurationProperties properties) {
        this.properties = properties;
    }

    private Path getPath(@NotNull final String id, final int partId) {
        return Paths.get(properties.getRoot(), partId + StorageObjectPart.DELIMITER + id);
    }

    private Path getPath(@NotNull final StorageObject object, final int partId) {
        return this.getPath(Objects.requireNonNull(object.getId()), partId);
    }

    @Lazy
    @Autowired
    public void setObjectStorageAccessorImpl(final ObjectStorageAccessorImpl objectStorageAccessorImpl) {
        this.objectStorageAccessorImpl = objectStorageAccessorImpl;
    }
}
