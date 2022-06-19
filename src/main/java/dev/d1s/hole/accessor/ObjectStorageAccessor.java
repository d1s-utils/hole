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

package dev.d1s.hole.accessor;

import dev.d1s.hole.entity.storageObject.StorageObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ObjectStorageAccessor {

    @NotNull
    InputStream createInputStream(@NotNull final StorageObject object);

    void closeInputStream(@NotNull final InputStream in);

    @NotNull
    OutputStream createOutputStream(@NotNull final StorageObject object);

    void closeOutputStream(@NotNull final OutputStream out);

    void deleteObject(@NotNull final StorageObject object);

    void processIoException(@NotNull final IOException e);
}
