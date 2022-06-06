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

package dev.d1s.hole.entity.storageObject;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "storage_object_access")
public final class StorageObjectAccess {

    @Id
    @Column
    @Nullable
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Nullable
    @Column(nullable = false)
    private Instant time;

    @NotNull
    @ManyToOne(cascade = CascadeType.MERGE)
    private StorageObject storageObject;

    public StorageObjectAccess(@NotNull final StorageObject storageObject) {
        this.storageObject = storageObject;
    }

    @PrePersist
    private void setCreationTime() {
        if (time == null) {
            time = Instant.now();
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final var that = (StorageObjectAccess) o;
        return Objects.equals(id, that.id) && Objects.equals(time, that.time) && storageObject.equals(that.storageObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, time, storageObject);
    }
}
