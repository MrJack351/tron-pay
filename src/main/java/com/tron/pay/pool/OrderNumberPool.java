package com.tron.pay.pool;

import java.util.ArrayList;
import java.util.List;

public class OrderNumberPool {
    private final List<OrderNumber> orderNumbers = new ArrayList<>();

    public OrderNumberPool() {
        for (int i = 1; i < 100; i++) {
            orderNumbers.add(new OrderNumber(String.format("%02d", i)));
        }
    }

    public synchronized String getNextAvailableOrderNumber() {
        for (OrderNumber orderNumber : orderNumbers) {
            if (!orderNumber.isUsed()) {
                orderNumber.setUsed(true);
                return orderNumber.getNumber();
            }
        }
        return null; // 如果所有数字都已被使用，返回null
    }

    public synchronized void releaseOrderNumber(String number) {
        for (OrderNumber orderNumber : orderNumbers) {
            if (orderNumber.getNumber().equals(number)) {
                orderNumber.setUsed(false);
                break;
            }
        }
    }
}