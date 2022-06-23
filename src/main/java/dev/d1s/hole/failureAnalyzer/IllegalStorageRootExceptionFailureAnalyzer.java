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

package dev.d1s.hole.failureAnalyzer;

import dev.d1s.hole.exception.storage.IllegalStorageRootException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.stereotype.Component;

@Component
public class IllegalStorageRootExceptionFailureAnalyzer implements FailureAnalyzer {

    @Nullable
    @Override
    public FailureAnalysis analyze(@NotNull final Throwable failure) {
        if (failure instanceof IllegalStorageRootException) {
            return new FailureAnalysis(
                    failure.getMessage(),
                    "Update your configuration.",
                    failure
            );
        } else {
            return null;
        }
    }
}
