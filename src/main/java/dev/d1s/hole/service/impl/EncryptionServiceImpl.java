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
import org.cryptonode.jncryptor.CryptorException;
import org.cryptonode.jncryptor.JNCryptor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EncryptionServiceImpl implements EncryptionService {

    private JNCryptor jnCryptor;

    @Override
    public byte[] encrypt(byte[] bytes, @NotNull String encryptionKey) {
        try {
            return jnCryptor.encryptData(bytes, encryptionKey.toCharArray());
        } catch (CryptorException e) {
            throw new EncryptionException(e);
        }
    }

    @Override
    public byte[] decrypt(byte[] bytes, @NotNull String encryptionKey) {
        try {
            return jnCryptor.decryptData(bytes, encryptionKey.toCharArray());
        } catch (CryptorException e) {
            throw new EncryptionException(e);
        }
    }

    @Autowired
    public void setJnCryptor(final JNCryptor jnCryptor) {
        this.jnCryptor = jnCryptor;
    }
}
