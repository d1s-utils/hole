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

package dev.d1s.hole.constant.longPolling;

public final class StorageObjectLongPollingConstants {

    private static final String GROUP_PREFIX = "storage-object-";

    public static final String STORAGE_OBJECT_ACCESSED_GROUP =
            StorageObjectLongPollingConstants.GROUP_PREFIX + "accessed";

    public static final String STORAGE_OBJECT_CREATED_GROUP =
            StorageObjectLongPollingConstants.GROUP_PREFIX + "created";

    public static final String STORAGE_OBJECT_UPDATED_GROUP =
            StorageObjectLongPollingConstants.GROUP_PREFIX + "updated";

    public static final String STORAGE_OBJECT_OVERWRITTEN_GROUP =
            StorageObjectLongPollingConstants.GROUP_PREFIX + "overwritten";

    public static final String STORAGE_OBJECT_DELETED_GROUP =
            StorageObjectLongPollingConstants.GROUP_PREFIX + "deleted";

    private StorageObjectLongPollingConstants() {
    }
}
