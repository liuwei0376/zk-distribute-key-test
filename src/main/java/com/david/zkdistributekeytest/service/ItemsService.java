package com.david.zkdistributekeytest.service;

import com.david.zkdistributekeytest.dao.ItemsDAO;
import com.david.zkdistributekeytest.domain.Items;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ItemsService {

    @Autowired
    private ItemsDAO itemsDAO;

    public Items getItem(String itemId){
        return itemsDAO.getOne(itemId);
    }

    public void saveItem(Items items){
        items.setId(UUID.randomUUID().toString());
        itemsDAO.save(items);
    }

    //根据itemId获取库存量（counts）
    public int getItemCounts(String itemId){
        return getItem(itemId).getCounts();
    }

    //调整库存
    public void reduceCounts(String itemId, int count){
        Items items = getItem(itemId);
        items.setCounts(items.getCounts() - count);
        itemsDAO.save(items);
    }

}
