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

package dev.d1s.hole.service;

import dev.d1s.hole.exception.encryption.EncryptionException;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.OutputStream;

public interface EncryptionService {

    @NotNull
    OutputStream createEncryptedOutputStream(@NotNull OutputStream out, @NotNull String encryptionKey);

    @NotNull
    InputStream createDecryptedInputStream(@NotNull InputStream in, @NotNull String encryptionKey);

    @NotNull
    EncryptionException createEncryptionException(@NotNull final Throwable cause);
}