package org.morozov.market.run;

import org.morozov.market.entity.Item;
import org.morozov.market.util.PersistenceProvider;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Item> itemList =
                PersistenceProvider.getEntityManager().createQuery("select i from market$Item i", Item.class).getResultList();
        itemList.forEach(item -> System.out.println(item.getName()));
    }
}
