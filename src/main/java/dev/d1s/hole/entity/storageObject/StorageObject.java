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

import dev.d1s.hole.entity.common.Identifiable;
import dev.d1s.hole.entity.metadata.MetadataProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "storage_object")
public final class StorageObject extends Identifiable {

    @NotNull
    @Column(nullable = false)
    private String name;

    @NotNull
    @Column(nullable = false)
    private String objectGroup;

    @Column(nullable = false)
    private boolean encrypted;

    @NotNull
    @Transient
    private String digest;

    @NotNull
    @OneToMany(mappedBy = "storageObject", cascade = CascadeType.ALL)
    private Set<StorageObjectAccess> storageObjectAccesses;

    @NotNull
    @ManyToMany
    @JoinTable(
            name = "storage_object_metadata",
            joinColumns = @JoinColumn(name = "storage_object_id"),
            inverseJoinColumns = @JoinColumn(name = "metadata_property_id")
    )
    private Set<MetadataProperty> metadata;

    public StorageObject(@NotNull final String name, @NotNull final String objectGroup, final boolean encrypted, @NotNull final Set<MetadataProperty> metadata) {
        this.name = name;
        this.objectGroup = objectGroup;
        this.encrypted = encrypted;
        this.metadata = metadata;
        this.storageObjectAccesses = new HashSet<>();
    }

    public StorageObject(@NotNull final String name, @NotNull final String objectGroup, @NotNull final Set<MetadataProperty> metadata) {
        this(name, objectGroup, false, metadata);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final var that = (StorageObject) o;
        return Objects.equals(this.getId(), that.getId())
                && Objects.equals(this.getCreationTime(), that.getCreationTime())
                && this.name.equals(that.name)
                && this.objectGroup.equals(that.objectGroup)
                && this.encrypted == that.encrypted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getCreationTime(), this.name, this.objectGroup, this.encrypted);
    }
}
