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

package dev.d1s.hole.dto.storageObject;

import dev.d1s.hole.dto.metadata.MetadataPropertyDto;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Set;

public record StorageObjectDto(

        @NotNull
        String id,

        @NotNull
        Instant creationTime,

        @NotNull
        String name,

        @NotNull
        String group,

        boolean encrypted,

        @NotNull
        String digest,
        @NotNull
        Set<MetadataPropertyDto> metadata
) {
}
