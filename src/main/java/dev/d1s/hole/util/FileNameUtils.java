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

package dev.d1s.hole.util;

// liquibase actually has some utility class for sanitizing file names, so I'll use that instead of implementing my own.

import liquibase.util.FilenameUtil;

public final class FileNameUtils {

    public static String sanitize(final String fileName) {
        return FilenameUtil.normalize(
                FilenameUtil.sanitizeFileName(fileName)
        );
    }

    private FileNameUtils() {
    }
}
