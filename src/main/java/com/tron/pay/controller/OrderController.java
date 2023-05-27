package com.tron.pay.controller;

import cn.hutool.core.util.IdUtil;
import com.tron.pay.config.TronConfig;
import com.tron.pay.entity.Merchant;
import com.tron.pay.entity.Order;
import com.tron.pay.pool.OrderNumberPoolManager;
import com.tron.pay.service.MerchantService;
import com.tron.pay.service.OrderService;
import com.tron.pay.utils.TronWalletUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    @Resource
    private TronConfig tronConfig;

    @Resource
    private OrderService orderService;

    @Resource
    private MerchantService merchantService;
    @Resource
    private OrderNumberPoolManager poolManager;


    @PostMapping("/createOrder")
    public Map<String, Object> createOrder(@RequestBody Order order) {
        HashMap<String, Object> resp = new HashMap<>();
        String merchantId = order.getMerchantId();
        Merchant merchant = merchantService.getById(merchantId);
        if (merchant == null) {
            resp.put("code",-1);
            resp.put("data",null);
            resp.put("message","商户不存在");
            return resp;
        }
        String orderId = IdUtil.getSnowflakeNextIdStr();
        // 设置订单号
        order.setOrderId(orderId);
        // 设置订单状态
        order.setOrderStatus("wait");
        // 设置通知次数
        order.setNotifyCount(0);
        // 设置通知状态
        order.setNotifyStatus("wait");
        // 设置支付方式
        order.setCoinType(order.getCoinType());
        // 设置订单所属商户
        order.setMerchantId(merchantId);
        // 设置订单随机金额
        String number = merchantService.getNextAvailableOrderNumber(merchant.getMerchantName());
        if (number == null) {
            resp.put("code",-1);
            resp.put("data",null);
            resp.put("message","请稍后再试！");
            return resp;
        }
        // 判断订单金额是否包含小数
        String amount = "";
        if (order.getAmount().contains(".")) {
            amount = order.getAmount() + number;
        }else {
            amount = order.getAmount() + "." + number;
        }
        order.setAmount(TronWalletUtils.toTronAmount(amount));
        order.setNumber(number);
        // 设置订单时间戳
        long currentTimeMillis = System.currentTimeMillis();
        order.setCreateTimeStamp(String.valueOf(currentTimeMillis));
        order.setFailureTimeStamp(String.valueOf(currentTimeMillis + 30 * 60 * 1000));
        // 设置监听地址
        order.setCollectionAddress(merchant.getAddress());

        orderService.save(order);
        orderService.createOrder(order, merchant);
        resp.put("code",200);
        resp.put("message","订单创建成功");
        resp.put("payUrl","http://localhost:8080/pay/wait/?" + "orderId=" + orderId);
        return resp;
    }

}
