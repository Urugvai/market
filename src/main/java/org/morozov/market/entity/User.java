package org.morozov.market.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Created by Morozov on 5/17/2017.
 */
@Entity(name = "market$User")
@Table(name = "market_user")
public class User extends BaseEntity {

    @Column(name = "login", nullable = false)
    private String login;

    @Column(name = "account")
    private BigDecimal account;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Item> items;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public BigDecimal getAccount() {
        return account;
    }

    public void setAccount(BigDecimal account) {
        this.account = account;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
