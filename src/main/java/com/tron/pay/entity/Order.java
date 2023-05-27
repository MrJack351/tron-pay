package com.tron.pay.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tb_order")
public class Order {

    // 平台订单Id
    @TableId
    private String token;
    // 商户端订单号
    private String orderId;

    //订单所属商户id
    private String merchantId;

    // 支付方式 trc usdt
    private String coinType;

    // 交易hash
    private String transactionId;

    // 订单金额
    private String amount;

    // 支付金额
    private String payAmount;

    // 收款地址
    private String collectionAddress;

    // 付款地址
    private String paymentAddress;

    // 订单创建时间戳
    private String createTimeStamp;

    // 订单失效时间戳
    private String failureTimeStamp;

    // 订单状态 wait failure success
    private String orderStatus;

    // 回调状态 success / failure / wait
    private String notifyStatus;
    // 回调地址
    private String notifyUrl;
    // 回调次数
    private int notifyCount;
    // 签名
    private String sign;

    // 订单所属网络，主网，测试网
    private String network;
    // 随机数
    @TableField(exist = false)
    private String number;

    // 订单类型 renewal 商户续费 common 普通订单
    private String orderType;

    // 续费商户的id
    private String renewalId;

    // 订单回调处理状态 ok no
    private String state;

}
