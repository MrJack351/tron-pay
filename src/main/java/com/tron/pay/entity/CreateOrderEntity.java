package com.tron.pay.entity;

import lombok.Data;

@Data
public class CreateOrderEntity {
    // 自定义订单号
    private String orderId;
    // 商户私钥
    private String merchantKey;
    // 金额 两位小数
    private String amount;
    // 币种 TRX,USDT
    private String coinType;
    // 订单支付成功回调通知地址
    private String notifyUrl;
    // 支付成功后的跳转地址
    private String redirectUrl;
}
