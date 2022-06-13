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

package dev.d1s.hole.constant.error;

public final class StorageObjectErrorConstants {

    public static final String STORAGE_OBJECT_NOT_FOUND_ERROR =
            "Storage object was not found.";

    public static final String STORAGE_OBJECT_ACCESS_ERROR =
            "Could not perform the I/O operation on the internal storage.";

    public static final String STORAGE_OBJECT_LOCKED_ERROR =
            "Storage object is locked. Please try again later.";

    public static final String FILE_NAME_NOT_RPESENT_ERROR =
            "File name must be present within the request.";

    private StorageObjectErrorConstants() {
    }
}
