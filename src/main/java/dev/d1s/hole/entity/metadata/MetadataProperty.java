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

package dev.d1s.hole.entity.metadata;

import dev.d1s.hole.entity.common.Identifiable;
import dev.d1s.hole.entity.storageObject.StorageObject;
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
@Table(name = "metadata_property")
public final class MetadataProperty extends Identifiable {

    @NotNull
    @Column(nullable = false)
    private String property;

    @NotNull
    @Column(nullable = false)
    private String value;

    @NotNull
    @ManyToMany(mappedBy = "metadata", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<StorageObject> storageObjects;

    public MetadataProperty(@NotNull final String property, @NotNull final String value) {
        this.property = property;
        this.value = value;
        storageObjects = new HashSet<>();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final var that = (MetadataProperty) o;
        return Objects.equals(this.getId(), that.getId())
                && Objects.equals(this.getCreationTime(), that.getCreationTime())
                && this.property.equals(that.property)
                && this.value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getCreationTime(), this.property, this.value);
    }
}
