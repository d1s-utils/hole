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

import dev.d1s.hole.exception.encryption.EncryptionException;
import dev.d1s.hole.service.EncryptionService;
import org.cryptonode.jncryptor.AES256JNCryptorInputStream;
import org.cryptonode.jncryptor.AES256JNCryptorOutputStream;
import org.cryptonode.jncryptor.CryptorException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;

@Service
public class EncryptionServiceImpl implements EncryptionService {

    @NotNull
    @Override
    public OutputStream createEncryptedOutputStream(@NotNull OutputStream out, @NotNull String encryptionKey) {
        try {
            return new AES256JNCryptorOutputStream(out, encryptionKey.toCharArray());
        } catch (final CryptorException e) {
            throw new EncryptionException(e);
        }
    }

    @NotNull
    @Override
    public InputStream createDecryptedInputStream(@NotNull InputStream in, @NotNull String encryptionKey) {
        try {
            return new AES256JNCryptorInputStream(in, encryptionKey.toCharArray());
        } catch (final RuntimeException e) {
            throw new EncryptionException(e);
        }
    }

    @NotNull
    @Override
    public EncryptionException createEncryptionException(@NotNull Throwable cause) {
        return new EncryptionException(cause);
    }
}
