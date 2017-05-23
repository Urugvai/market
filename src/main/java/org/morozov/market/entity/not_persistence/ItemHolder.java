package org.morozov.market.entity.not_persistence;

import org.morozov.market.entity.Item;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Morozov on 5/23/2017.
 */
public class ItemHolder implements Serializable {
    public List<Item> itemList = new ArrayList<>();
}
