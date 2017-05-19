package org.morozov.market.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.morozov.market.entity.Item;
import org.morozov.market.util.PersistenceProvider;

import java.util.List;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);
    public static void main(String[] args) {
        logger.info("Hello!");
        List<Item> itemList =
                PersistenceProvider.getEntityManager().createQuery("select i from market$Item i", Item.class).getResultList();
        itemList.forEach(item -> System.out.println(item.getName()));
    }
}
