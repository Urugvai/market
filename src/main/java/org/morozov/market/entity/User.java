package org.morozov.market.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Created by Morozov on 5/17/2017.
 */
@Entity(name = "market$User")
@Table(name = "market_user")
public class User extends BaseEntity {

    @Column(name = "login", nullable = false)
    private String login;

    @Temporal(TemporalType.DATE)
    @Column(name = "login_date", nullable = false)
    private Date loginDate;

    @Column(name = "account")
    private BigDecimal account;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "market_user_item",
            joinColumns =
            @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns =
            @JoinColumn(name = "item_id", referencedColumnName = "id")
    )
    private List<Item> items;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Date getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(Date loginDate) {
        this.loginDate = loginDate;
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
