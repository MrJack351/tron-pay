package com.tron.pay.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tron.pay.entity.Merchant;

public interface MerchantService extends IService<Merchant> {

    String getNextAvailableOrderNumber(String merchantName);

    void releaseOrderNumber(String merchantName, String number);

    Merchant getMerchantByNameAndPassword(Merchant merchant);

    Merchant getMerchantByName(String merchantName);

    Merchant getMerchantByMerchantKey(String merchantKey);
}
