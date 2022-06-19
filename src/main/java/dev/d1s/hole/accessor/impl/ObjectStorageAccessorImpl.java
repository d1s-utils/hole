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
import dev.d1s.hole.exception.storage.IllegalStorageRootException;
import dev.d1s.hole.exception.storage.StorageObjectAccessException;
import dev.d1s.hole.properties.StorageConfigurationProperties;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Component
public class ObjectStorageAccessorImpl implements ObjectStorageAccessor, InitializingBean {

    private StorageConfigurationProperties properties;

    @NotNull
    @Override
    public InputStream createInputStream(@NotNull StorageObject object) {
        try {
            return Files.newInputStream(this.getPath(object));
        } catch (final IOException e) {
            throw this.createException(e);
        }
    }

    @Override
    public void closeInputStream(@NotNull InputStream in) {
        try {
            in.close();
        } catch (final IOException e) {
            throw this.createException(e);
        }
    }

    @NotNull
    @Override
    public OutputStream createOutputStream(@NotNull StorageObject object) {
        try {
            return Files.newOutputStream(this.getPath(object));
        } catch (final IOException e) {
            throw this.createException(e);
        }
    }

    @Override
    public void closeOutputStream(@NotNull OutputStream out) {
        try {
            out.close();
        } catch (final IOException e) {
            throw this.createException(e);
        }
    }

    @Override
    public void deleteObject(@NotNull StorageObject object) {
        try {
            Files.delete(this.getPath(object));
        } catch (final IOException e) {
            throw this.createException(e);
        }
    }

    @Override
    public void processIoException(@NotNull IOException e) {
        throw this.createException(e);
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

    private Path getPath(final StorageObject object) {
        return Paths.get(properties.getRoot(), Objects.requireNonNull(object.getId()));
    }

    private StorageObjectAccessException createException(final IOException e) {
        e.printStackTrace();
        return new StorageObjectAccessException();
    }
}
