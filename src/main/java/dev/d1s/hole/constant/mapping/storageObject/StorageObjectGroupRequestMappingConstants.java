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

package dev.d1s.hole.constant.mapping.storageObject;

import dev.d1s.hole.constant.mapping.ApiBaseRequestMappingConstants;
import dev.d1s.hole.constant.mapping.CommonRequestMappingConstants;

public final class StorageObjectGroupRequestMappingConstants {

    private static final String GROUP_BASE_MAPPING =
            ApiBaseRequestMappingConstants.API_BASE_MAPPING + "/groups";

    public static final String GET_GROUP_MAPPING =
            StorageObjectGroupRequestMappingConstants.GROUP_BASE_MAPPING
                    + CommonRequestMappingConstants.ID_MAPPING;

    public static final String GET_ALL_GROUPS_MAPPING =
            StorageObjectGroupRequestMappingConstants.GROUP_BASE_MAPPING;

    public static final String GET_ALL_GROUP_NAMES_MAPPING =
            StorageObjectGroupRequestMappingConstants.GROUP_BASE_MAPPING + "/names";

    public static final String POST_GROUP_MAPPING =
            StorageObjectGroupRequestMappingConstants.GROUP_BASE_MAPPING;

    public static final String PUT_GROUP_MAPPING =
            StorageObjectGroupRequestMappingConstants.GROUP_BASE_MAPPING
                    + CommonRequestMappingConstants.ID_MAPPING;

    public static final String DELETE_GROUP_MAPPING =
            StorageObjectGroupRequestMappingConstants.GROUP_BASE_MAPPING
                    + CommonRequestMappingConstants.ID_MAPPING;

    private StorageObjectGroupRequestMappingConstants() {
    }
}
