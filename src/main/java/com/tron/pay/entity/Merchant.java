package com.tron.pay.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_merchant")
public class Merchant {
    // 商户id
    @TableId
    private String merchantId;
    // 商户名称
    private String merchantName;
    // 商户密码
    private String merchantPassword;
    // 商户密钥
    private String merchantKey;
    // 收款地址
    private String address;
    // 钱包base64二维码地址
    private String imgUrl;
    private String netUrl;
    // 回调地址
    private String notifyUrl;
    //创建时间
    private Date createTime;
    // 商户到期时间
    private Date remainingTime;
    @TableField(exist = false)
    private String isMainNet;


}