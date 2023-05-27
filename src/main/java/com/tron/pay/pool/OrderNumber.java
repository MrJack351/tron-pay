package com.tron.pay.pool;

import lombok.Data;

@Data
public class OrderNumber {
    private final String number;
    private boolean isUsed;

    public OrderNumber(String number) {
        this.number = number;
        this.isUsed = false;
    }

}