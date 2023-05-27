package com.tron.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tron.pay.entity.Merchant;
import com.tron.pay.mapper.MerchantMapper;
import com.tron.pay.pool.OrderNumberPool;
import com.tron.pay.pool.OrderNumberPoolManager;
import com.tron.pay.service.MerchantService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


@Service
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements MerchantService {

    @Resource
    private MerchantMapper merchantMapper;

    @Resource
    private OrderNumberPoolManager poolManager;

    public String getNextAvailableOrderNumber(String merchantName) {
        OrderNumberPool orderNumberPool = poolManager.getOrderNumberPoolForMerchant(merchantName);
        return orderNumberPool.getNextAvailableOrderNumber();
    }

    public void releaseOrderNumber(String merchantName, String number) {
        OrderNumberPool orderNumberPool = poolManager.getOrderNumberPoolForMerchant(merchantName);
        orderNumberPool.releaseOrderNumber(number);
    }

    @Override
    public Merchant getMerchantByNameAndPassword(Merchant merchant) {
        LambdaQueryWrapper<Merchant> lqw = new LambdaQueryWrapper<>();
        lqw.eq(true,Merchant::getMerchantName,merchant.getMerchantName());
        lqw.eq(true,Merchant::getMerchantPassword,merchant.getMerchantPassword());
        return merchantMapper.selectOne(lqw);
    }

    @Override
    public Merchant getMerchantByName(String merchantName) {
        LambdaQueryWrapper<Merchant> lqw = new LambdaQueryWrapper<>();
        lqw.eq(true,Merchant::getMerchantName,merchantName);
        return merchantMapper.selectOne(lqw);
    }

    @Override
    public Merchant getMerchantByMerchantKey(String merchantKey) {
        LambdaQueryWrapper<Merchant> lqw = new LambdaQueryWrapper<>();
        lqw.eq(true,Merchant::getMerchantKey,merchantKey);
        return merchantMapper.selectOne(lqw);
    }

}
