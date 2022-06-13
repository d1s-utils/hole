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

// liquibase actually has an utility class for sanitizing file names, so I'll use that instead of implementing my own.

import dev.d1s.advice.exception.BadRequestException;
import dev.d1s.hole.constant.error.StorageObjectErrorConstants;
import liquibase.util.FilenameUtil;
import org.jetbrains.annotations.NotNull;

public final class FileNameUtils {

    @NotNull
    public static String sanitizeAndCheck(final String fileName) {
        if (fileName == null) {
            throw new BadRequestException(StorageObjectErrorConstants.FILE_NAME_NOT_RPESENT_ERROR);
        }

        return FilenameUtil.normalize(
                FilenameUtil.sanitizeFileName(fileName)
        );
    }

    private FileNameUtils() {
    }
}
