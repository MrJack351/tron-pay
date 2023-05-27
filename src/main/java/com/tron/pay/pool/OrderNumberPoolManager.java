package com.tron.pay.pool;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrderNumberPoolManager {
    private final Map<String, OrderNumberPool> merchantOrderNumberPools = new HashMap<>();

    public synchronized OrderNumberPool getOrderNumberPoolForMerchant(String merchantName) {
        OrderNumberPool orderNumberPool = merchantOrderNumberPools.get(merchantName);
        if (orderNumberPool == null) {
            orderNumberPool = new OrderNumberPool();
            merchantOrderNumberPools.put(merchantName, orderNumberPool);
        }
        return orderNumberPool;
    }
}