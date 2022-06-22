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

package dev.d1s.hole.configuration;

import dev.d1s.hole.constant.longPolling.StorageObjectGroupLongPollingConstants;
import dev.d1s.hole.constant.longPolling.StorageObjectLongPollingConstants;
import dev.d1s.lp.server.configurer.LongPollingServerConfigurer;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class LongPollingServerConfiguration implements LongPollingServerConfigurer {

    @NotNull
    @Override
    public Set<String> getAvailableGroups() {
        return Set.of(
                StorageObjectLongPollingConstants.STORAGE_OBJECT_ACCESSED_GROUP,
                StorageObjectLongPollingConstants.STORAGE_OBJECT_CREATED_GROUP,
                StorageObjectLongPollingConstants.STORAGE_OBJECT_UPDATED_GROUP,
                StorageObjectLongPollingConstants.STORAGE_OBJECT_OVERWRITTEN_GROUP,
                StorageObjectLongPollingConstants.STORAGE_OBJECT_DELETED_GROUP,

                StorageObjectGroupLongPollingConstants.OBJECT_GROUP_CREATED,
                StorageObjectGroupLongPollingConstants.OBJECT_GROUP_UPDATED,
                StorageObjectGroupLongPollingConstants.OBJECT_GROUP_DELETED
        );
    }
}
