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

public final class StorageErrorConstants {

    public static final String STORAGE_PATH_NOT_ABSOLUTE_ERRROR =
            "The provided storage path is not absolute.";

    public static final String STORAGE_PATH_DOES_NOT_EXIST_ERROR =
            "The provided storage path does not exist.";

    public static final String STORAGE_PATH_NOT_A_DIRECTORY_ERROR =
            "The provided storage path is not a directory.";

    private StorageErrorConstants() {
    }
}
