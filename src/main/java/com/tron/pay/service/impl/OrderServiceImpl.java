package com.tron.pay.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tron.pay.common.Pagination;
import com.tron.pay.dto.OrderDto;
import com.tron.pay.entity.Merchant;
import com.tron.pay.entity.Order;
import com.tron.pay.mapper.MerchantMapper;
import com.tron.pay.mapper.OrderMapper;
import com.tron.pay.service.OrderService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Resource
    private AsyncOrderService asyncOrderService;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private MerchantMapper merchantMapper;

    public void processOrder(Order order) {
//        asyncOrderService.executeTask(order);
    }

    @Override
    public void createOrder(Order order, Merchant merchant) {
        asyncOrderService.executeTask(order, merchant);
    }

    @Override
    public IPage<Order> pageList(Pagination pagination) {
        OrderDto orderDto = (OrderDto) pagination;
        long pageNum = orderDto.getPageNum();
        long pageSize = orderDto.getPageSize();
        String merchantId = (String) StpUtil.getLoginId();
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> lqw = new LambdaQueryWrapper<>();

//        if ("1".equals(orderStatus)) {
//            lqw.eq(true, Order::getOrderStatus, "success");
//        }
//        if ("2".equals(orderStatus)) {
//            lqw.eq(true, Order::getOrderStatus, "wait");
//        }
//        if ("3".equals(orderStatus)) {
//            lqw.eq(true, Order::getOrderStatus, "failure");
//        }
        String orderStatus = orderDto.getOrderStatus();
        String token = orderDto.getToken();
        String orderId = orderDto.getOrderId();
        String transactionId = orderDto.getTransactionId();
        lqw.eq(StrUtil.isNotBlank(orderStatus), Order::getOrderStatus, orderStatus);
        lqw.eq(StrUtil.isNotBlank(token),Order::getToken,token);
        lqw.eq(StrUtil.isNotBlank(orderId),Order::getOrderId,orderId);
        lqw.eq(StrUtil.isNotBlank(transactionId),Order::getTransactionId,transactionId);
        lqw.eq(true,Order::getMerchantId, merchantId);
        lqw.orderBy(true, false, Order::getCreateTimeStamp);
        return orderMapper.selectPage(page, lqw);
    }

    public IPage<Order> getRecentlyOrderList() {
        Page<Order> page = new Page<>(1, 10);
        LambdaQueryWrapper<Order> lqw = new LambdaQueryWrapper<>();
        lqw.eq(true,Order::getMerchantId,String.valueOf(StpUtil.getLoginId()));
        lqw.orderBy(true, false, Order::getCreateTimeStamp);
        return orderMapper.selectPage(page, lqw);
    }

    @Override
    public BigDecimal getTodayTurnover(String merchantId) {
        // 获取今日零点时间戳（单位为毫秒）
        long startTime = DateUtil.beginOfDay(new Date()).getTime();
        // 获取明日零点时间戳（单位为毫秒），即今日结束时间戳
        long endTime = DateUtil.beginOfDay(DateUtil.tomorrow()).getTime();
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("merchant_id", merchantId)
                .ge("create_time_stamp", startTime)
                .le("create_time_stamp", endTime)
                .eq("order_status", "success");
        List<Order> orders = orderMapper.selectList(queryWrapper);
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Order order : orders) {
            if (StrUtil.isNotBlank(order.getPayAmount())) {
                BigDecimal payAmount = new BigDecimal(order.getPayAmount());
                totalAmount = totalAmount.add(payAmount);
            }
        }
        return totalAmount;
    }

    @Override
    public BigDecimal getAllTurnover(String merchantId) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("merchant_id", merchantId)
                .eq("order_status", "success");
        List<Order> orders = orderMapper.selectList(queryWrapper);
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Order order : orders) {
            if (StrUtil.isNotBlank(order.getPayAmount())) {
                BigDecimal payAmount = new BigDecimal(order.getPayAmount());
                totalAmount = totalAmount.add(payAmount);
            }
        }
        return totalAmount;
    }

    public int getDailyOrderCount(String merchantId) {
        // 获取今日零点时间戳（单位为毫秒）
        long startTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant(ZoneOffset.of("+8")).toEpochMilli();
        // 获取当前时间戳（单位为秒）
        long endTime = System.currentTimeMillis();
        return orderMapper.getDailyOrderCount(merchantId, startTime, endTime);
    }

    public int getTotalOrderCount(String merchantId) {
        return orderMapper.getTotalOrderCount(merchantId);
    }

    @Override
    public List<Map<String, Object>> getWeekDataAmount(String merchantId) {
        return orderMapper.selectWeekDataAmount(merchantId);
    }

    @Override
    public List<Map<String, Object>> getWeekDataCount(String merchantId) {
        return orderMapper.selectWeekDataCount(merchantId);
    }
}
