package org.morozov.market.entity;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Morozov on 5/17/2017.
 */
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    @Id
    @Column(name = "id", nullable = false, unique = true)
    private String id;

    public BaseEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public UUID getId() {
        return UUID.fromString(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseEntity baseEntity = (BaseEntity) o;

        return id != null ? id.equals(baseEntity.id) : baseEntity.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
