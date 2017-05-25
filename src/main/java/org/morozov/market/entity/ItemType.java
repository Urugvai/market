package org.morozov.market.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

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

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @OneToMany(mappedBy = "itemType", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Item> items;

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

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
