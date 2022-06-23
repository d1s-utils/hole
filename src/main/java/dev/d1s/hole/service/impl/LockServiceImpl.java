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

import dev.d1s.hole.entity.storageObject.StorageObject;
import dev.d1s.hole.exception.storage.StorageObjectLockedException;
import dev.d1s.hole.service.LockService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class LockServiceImpl implements LockService {

    private static final long LOCK_TIMEOUT = 10L;

    private static final Logger log = LogManager.getLogger();

    private final Map<String, Lock> lockMap = new ConcurrentHashMap<>();

    private LockServiceImpl lockServiceImpl;

    @Override
    public void lock(@NotNull String id) {
        var lock = lockMap.get(id);

        if (lock == null) {
            lock = new ReentrantLock();
            lockMap.put(id, lock);
        }

        try {
            if (!lock.tryLock(LOCK_TIMEOUT, TimeUnit.SECONDS)) {
                throw new StorageObjectLockedException(id);
            }

            log.debug("Locked object {}", id);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void lock(@NotNull StorageObject object) {
        lockServiceImpl.lock(Objects.requireNonNull(object.getId()));
    }

    @Override
    public void unlock(@NotNull String id) {
        var lock = lockMap.get(id);

        if (lock == null) {
            throw new IllegalArgumentException();
        }

        lock.unlock();

        log.debug("Unlocked object {}", id);
    }

    @Override
    public void unlock(@NotNull StorageObject object) {
        lockServiceImpl.unlock(Objects.requireNonNull(object.getId()));
    }

    @Override
    public void removeLock(@NotNull final StorageObject object) {
        final var objectId = Objects.requireNonNull(object.getId());

        lockMap.remove(objectId);

        log.debug("Removed lock for object {}", objectId);
    }

    @Lazy
    @Autowired
    public void setLockServiceImpl(final LockServiceImpl lockServiceImpl) {
        this.lockServiceImpl = lockServiceImpl;
    }
}
