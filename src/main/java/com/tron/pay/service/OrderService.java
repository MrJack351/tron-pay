package com.tron.pay.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tron.pay.common.Pagination;
import com.tron.pay.entity.Merchant;
import com.tron.pay.entity.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface OrderService extends IService<Order> {

    void processOrder(Order order);

    void createOrder(Order order, Merchant merchant);

    IPage<Order> pageList(Pagination pagination);

    IPage<Order> getRecentlyOrderList();

    BigDecimal getTodayTurnover(String merchantId);

    BigDecimal getAllTurnover(String merchantId);

    int getDailyOrderCount(String merchantId);

    int getTotalOrderCount(String merchantId);

    List<Map<String,Object>> getWeekDataAmount(String merchantId);

    List<Map<String,Object>> getWeekDataCount(String merchantId);
}
