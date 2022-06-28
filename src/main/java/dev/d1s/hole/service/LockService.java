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

import dev.d1s.hole.entity.storageObject.StorageObject;
import org.jetbrains.annotations.NotNull;

public interface LockService {

    void lockRead(@NotNull final String id);

    void lockRead(@NotNull final StorageObject object);

    void lockWrite(@NotNull final String id);

    void lockWrite(@NotNull final StorageObject object);

    void unlockRead(@NotNull final String id);

    void unlockRead(@NotNull final StorageObject object);

    void unlockWrite(@NotNull final String id);

    void unlockWrite(@NotNull final StorageObject object);

    void removeLock(@NotNull final StorageObject object);
}
