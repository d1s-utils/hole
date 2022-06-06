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

package dev.d1s.hole.dto.converter;

import dev.d1s.hole.dto.metadata.MetadataPropertyDto;
import dev.d1s.hole.entity.metadata.MetadataProperty;
import dev.d1s.teabag.dto.DtoConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class MetadataPropertyDtoConverter implements DtoConverter<MetadataPropertyDto, MetadataProperty> {

    @NotNull
    @Override
    public MetadataPropertyDto convertToDto(@NotNull final MetadataProperty metadataProperty) {
        return new MetadataPropertyDto(
                metadataProperty.getProperty(),
                metadataProperty.getValue()
        );
    }

    @NotNull
    @Override
    public MetadataProperty convertToEntity(@NotNull final MetadataPropertyDto metadataPropertyDto) {
        return new MetadataProperty(metadataPropertyDto.property(), metadataPropertyDto.value());
    }
}
