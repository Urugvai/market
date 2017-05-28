package org.morozov.market.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * Created by Morozov on 5/17/2017.
 */
@Entity(name = "market$ItemType")
@Table(name = "market_item_type")
public class ItemType extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "removed_from_selling")
    private Boolean removedFromSelling;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getRemovedFromSelling() {
        return removedFromSelling;
    }

    public void setRemovedFromSelling(Boolean removedFromSelling) {
        this.removedFromSelling = removedFromSelling;
    }
}
