package com.tron.pay.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tron.pay.common.ApiResponse;
import com.tron.pay.common.TableResponse;
import com.tron.pay.dto.OrderDto;
import com.tron.pay.entity.CreateOrderEntity;
import com.tron.pay.entity.LoginEntity;
import com.tron.pay.entity.Merchant;
import com.tron.pay.entity.Order;
import com.tron.pay.service.MerchantService;
import com.tron.pay.service.OrderService;
import com.tron.pay.service.impl.AsyncOrderService;
import com.tron.pay.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Slf4j
public class PageApiController {

    @Resource
    private OrderService orderService;

    @Resource
    private MerchantService merchantService;

    @Resource
    private AsyncOrderService asyncOrderService;

    @Value("${tronPay.baseUrl}")
    private String baseUrl;

    @Value("${tronPay.mainNet}")
    private String mainNet;

    @Value("${tronPay.nileNet}")
    private String nileNet;


    @GetMapping("/loginStatus")
    public ApiResponse checkLoginStatus() {
        HashMap<String, Object> resp = new HashMap<>();
        resp.put("code", 200);
        resp.put("status", StpUtil.isLogin());
        return ApiResponse.success(resp);
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody LoginEntity loginEntity) {
        // 判断用户名或密码是否存在
        String merchantName = loginEntity.getMerchantName();
        String merchantPassword = loginEntity.getMerchantPassword();
        if (StrUtil.hasBlank(merchantName) || StrUtil.hasBlank(merchantPassword)) {
            return ApiResponse.fail(401,"用户名或密码错误");
        }
        Merchant merchant = new Merchant();
        merchant.setMerchantName(merchantName);
        merchant.setMerchantPassword(merchantPassword);
        Merchant result = merchantService.getMerchantByNameAndPassword(merchant);
        if (ObjUtil.isNotNull(result)) {
            StpUtil.login(result.getMerchantId());
            return ApiResponse.success("登录成功");
        }
        return ApiResponse.fail(401, "用户名或密码错误");
    }

    @GetMapping("logout")
    public ApiResponse logout() {
        StpUtil.logout();
        return ApiResponse.success("退出成功","/login");
    }

    @PostMapping("/register")
    public ApiResponse register(@RequestBody LoginEntity loginEntity) {
        // 判断用户名或密码是否存在
        String merchantName = loginEntity.getMerchantName();
        String p1 = loginEntity.getMerchantPassword();
        String p2 = loginEntity.getMerchantPassword2();
        if (StrUtil.hasBlank(merchantName) || StrUtil.hasBlank(p1) || StrUtil.hasBlank(p2)) {
            return ApiResponse.fail(401, "用户名或密码不能为空");
        }
        if (!p1.equals(p2)) {
            return ApiResponse.fail(401,"密码不一致");
        }
        int l1 = merchantName.length();
        int l2 = p1.length();
        int l3 = p2.length();
        if (l1 < 4 || l1 > 32 || l2 < 6 || l2 > 32) {
            String msg = "";
            if (l1 < 4) {
                msg = "用户名长度4~32位";
            }
            if (l2 < 6) {
                msg = "密码名长度6~32位";
            }
            return ApiResponse.fail(401,msg);
        }
        Merchant merchant = new Merchant();
        merchant.setMerchantName(merchantName);
        merchant.setMerchantPassword(p1);
        Merchant result = merchantService.getMerchantByName(merchantName);
        if (ObjUtil.isNotNull(result)) {
            return ApiResponse.fail(401,"用户名已存在");
        }
        // 初始化数据
        merchant.setCreateTime(new Date());
        merchant.setMerchantId(IdUtil.getSnowflakeNextIdStr());
        merchant.setMerchantKey(IdUtil.fastSimpleUUID());
        merchant.setRemainingTime(DateUtil.offsetWeek(new Date(), 1));
        merchant.setNetUrl(nileNet);
        // 写入数据库
        merchantService.save(merchant);
        return ApiResponse.success("注册成功");
    }

    @PostMapping("/updatePassword")
    public ApiResponse updatePassword(@RequestBody LoginEntity loginEntity) {
        String merchantId = (String) StpUtil.getLoginId();
        String oldP = loginEntity.getMerchantPassword();
        String p1 = loginEntity.getMerchantPassword1();
        String p2 = loginEntity.getMerchantPassword2();
        if (StrUtil.hasBlank(oldP) || StrUtil.hasBlank(p1) || StrUtil.hasBlank(p2)) {
            return ApiResponse.fail(401,"请填写所有字段");
        }
        Merchant merchant = merchantService.getById(merchantId);
        if (!oldP.equals(merchant.getMerchantPassword())) {
            return ApiResponse.fail(401,"旧密码错误");
        }
        if (!p1.equals(p2)) {
            return ApiResponse.fail(401,"密码不一致");
        }
        merchant.setMerchantPassword(p1);
        // 写入数据库
        merchantService.updateById(merchant);
        return ApiResponse.success("修改成功");
    }

    @PostMapping("/updateUserInfo")
    public ApiResponse updateUserInfo(@RequestBody Merchant info) {
        String merchantId = (String) StpUtil.getLoginId();
        String address = info.getAddress();
        String notifyUrl = info.getNotifyUrl();
        if (StrUtil.hasBlank(address) || StrUtil.hasBlank(notifyUrl)) {
            return ApiResponse.fail("请填写所有字段");
        }
        // 查询商户信息
        Merchant merchant = merchantService.getById(merchantId);

        // 校验钱包地址是否正确
        boolean validateAddress = asyncOrderService.validateAddress(address, merchant.getNetUrl());
        if (!validateAddress) {
            return ApiResponse.fail("输入的钱包地址有误");
        }
        if ("true".equals(info.getIsMainNet())) {
            merchant.setNetUrl(mainNet);
        }else merchant.setNetUrl(nileNet);
        // 设置回调地址
        merchant.setNotifyUrl(notifyUrl);
        // 设置钱包地址
        merchant.setAddress(address);
        // 写入数据库
        merchantService.updateById(merchant);
        return ApiResponse.success("修改成功");
    }

    /**
     *商户续费
     * @param type 续费套餐类型
     * @return
     */
    @GetMapping("/renewal")
    public ApiResponse renewal(@RequestParam String type) {

        String merchantId = (String) StpUtil.getLoginId();
        Merchant merchant = merchantService.getById(merchantId);
        Merchant admin = merchantService.getMerchantByName("admin");
        if (StrUtil.isBlank(admin.getAddress())) return ApiResponse.fail("暂无收款钱包");
        Order order = new Order();
        // 判断套餐类型
        if ("month".equals(type)) {
            // 设置订单金额
            order.setAmount("10");
        }else if ("quarter".equals(type)) {
            order.setAmount("25");
        }else if ("year".equals(type)) {
            order.setAmount("80");
        }else return ApiResponse.fail("暂无该套餐");
        String token = UUID.fastUUID().toString();
        // 设置token
        order.setToken(token);
        // 设置订单号
//        order.setOrderId("");
        // 设置订单状态
        order.setOrderStatus("wait");
        // 设置订单类型 renewal 商户续费，用于回调时处理
        order.setOrderType("renewal");
        // 设置处理状态 待处理
        order.setState("no");
        // 设置续费商户id
        order.setRenewalId(merchantId);
        // 设置通知url
        order.setNotifyUrl(admin.getNotifyUrl());
        // 设置通知次数
        order.setNotifyCount(0);
        // 设置通知状态
        order.setNotifyStatus("wait");
        // 设置支币种类型
        order.setCoinType("USDT");
        // 设置订单所属商户
        order.setMerchantId(admin.getMerchantId());
        // 设置订单归属网络
        order.setNetwork(admin.getNetUrl());
        // 设置收款钱包地址
        order.setCollectionAddress(admin.getAddress());
        // 设置订单随机金额
        String number = merchantService.getNextAvailableOrderNumber(admin.getMerchantName());
        if (number == null) {
            return ApiResponse.fail("请稍后再试");
        }
        String payAmount = order.getAmount() + "." + number;
        // 设置支付金额
        order.setPayAmount(payAmount);
        // 订单随机金额
        order.setNumber(number);
        // 设置订单时间戳
        long currentTimeMillis = System.currentTimeMillis();
        order.setCreateTimeStamp(String.valueOf(currentTimeMillis));
        order.setFailureTimeStamp(String.valueOf(currentTimeMillis + 15 * 60 * 1000));
        // 生成订单签名
        MD5 md5 = MD5.create();
        // 签名：金额，币种，token，商户私钥
        String md5Str = order.getAmount() + order.getCoinType() + token + admin.getMerchantKey();
        String sign = md5.digestHex(md5Str);
        // 设置订单签名
        order.setSign(sign);
        orderService.save(order);
        orderService.createOrder(order, admin);
        // 收银台地址
        String officialPayUrl = baseUrl + "/pay/wait/?token=" + token;
        return ApiResponse.success("创建订单成功",officialPayUrl);
    }

    @PostMapping("/createOrder")
    public ApiResponse createOrder(@RequestBody CreateOrderEntity orderEntity) {
        String merchantKey = orderEntity.getMerchantKey();
        Merchant merchant = merchantService.getMerchantByMerchantKey(merchantKey);
        if (merchant == null) {
            return ApiResponse.fail("密钥错误");
        }
        // 获取商户的到期时间
        Date remainingTime = merchant.getRemainingTime();
        if (remainingTime == null || remainingTime.before(new Date())) {
            // 如果到期时间为空或者早于当前时间，则说明套餐已过期
            return ApiResponse.fail("套餐已过期请续费");
        }
        String coinType = orderEntity.getCoinType();
        if (!"TRX".equals(coinType) && !"USDT".equals(coinType)) {
            return ApiResponse.fail("暂不支持该币种");
        }
        String address = merchant.getAddress();
        if (StrUtil.hasBlank(address)) {
            return ApiResponse.fail("暂无收款地址");
        }
        Order order = new Order();
        String token = UUID.fastUUID().toString();
        // 设置token
        order.setToken(token);
        // 设置处理状态 待处理
        order.setState("no");
        // 设置订单金额
        order.setAmount(orderEntity.getAmount());
        // 设置订单号
        order.setOrderId(orderEntity.getOrderId());
        // 设置订单状态
        order.setOrderStatus("wait");
        // 设置订单类型
        order.setOrderType("common");
        // 设置通知url
        order.setNotifyUrl(merchant.getNotifyUrl());
        // 设置通知次数
        order.setNotifyCount(0);
        // 设置通知状态
        order.setNotifyStatus("wait");
        // 设置支币种类型
        order.setCoinType(orderEntity.getCoinType());
        // 设置订单所属商户
        order.setMerchantId(merchant.getMerchantId());
        // 设置订单归属网络
        order.setNetwork(merchant.getNetUrl());
        // 设置订单随机金额
        String number = merchantService.getNextAvailableOrderNumber(merchant.getMerchantName());
        if (number == null) {
            return ApiResponse.fail("请稍后再试");
        }
        // 判断订单金额是否包含小数
        String amount = "";
        if (orderEntity.getAmount().contains(".")) {
            amount = orderEntity.getAmount() + number;
        }else {
            amount = orderEntity.getAmount() + "." + number;
        }
        // 设置支付金额
        order.setPayAmount(amount);
        // 订单随机金额
        order.setNumber(number);
        // 设置订单时间戳
        long currentTimeMillis = System.currentTimeMillis();
        order.setCreateTimeStamp(String.valueOf(currentTimeMillis));
        order.setFailureTimeStamp(String.valueOf(currentTimeMillis + 15 * 60 * 1000));
        // 设置监听地址
        order.setCollectionAddress(merchant.getAddress());
        // 生成订单签名
        MD5 md5 = MD5.create();
        // 签名：金额，币种，token，商户私钥
        String md5Str = orderEntity.getAmount() + orderEntity.getCoinType() + token + merchantKey;
        String sign = md5.digestHex(md5Str);
        log.info("MD5前的字符串为：{}",md5Str);
        log.info("MD5后的签名字符串为：{}",sign);
        // 设置订单签名
        order.setSign(sign);
        orderService.save(order);
        orderService.createOrder(order, merchant);
        HashMap<String, String> data = new HashMap<>();
        // 平台订单token
        data.put("token", order.getToken());
        // 官方订单号
        data.put("orderId", order.getOrderId());
        // 订单金额
        data.put("amount", orderEntity.getAmount());
        // 订单支付金额
        data.put("payAmount", amount);
        // 币种
        data.put("coinType", orderEntity.getCoinType());
        // 收款地址
        data.put("collectionAddress", order.getCollectionAddress());
        // 官方收银台地址
        data.put("officialPayUrl", baseUrl + "/pay/wait/?token=" + token);
        // 订单过期时间（毫秒）
        data.put("timeout", order.getFailureTimeStamp());
        // 签名
        data.put("sign", sign);

        return ApiResponse.success("订单创建成功",data);
    }

    @PostMapping("/notifyUrl")
    public ApiResponse notifyUrl(@RequestBody Map<String, Object> map) {
        log.info("收到异步回调通知：{}", map);
        // 从请求参数中获取各个字段值
        String amount = Objects.toString(map.getOrDefault("amount", ""));
        String orderType = Objects.toString(map.getOrDefault("orderType", ""));
        String renewalId = Objects.toString(map.getOrDefault("renewalId", ""));
        String coinType = Objects.toString(map.getOrDefault("coinType", ""));
        String token = Objects.toString(map.getOrDefault("token", ""));
        String sign = Objects.toString(map.getOrDefault("sign", ""));
        String merchantKey = "36661995b9eb47eb92c178beb35e7278";   // 商户私钥
        String verifySign = CommonUtils.verifySign(amount, coinType, token, merchantKey);
        Order order = orderService.getById(token);
        // 验证签名是否正确
        if (verifySign.equals(sign)) {
            log.info("签名校验成功:原sign{}:计算sign{}", sign, verifySign);
            if (StrUtil.isNotBlank(orderType) && StrUtil.isNotBlank(renewalId)) {
                Merchant merchant = merchantService.getById(renewalId);
                if (merchant != null && order != null && "no".equals(order.getState())) {
                    // 获取商户的到期时间
                    Date expiredTime = merchant.getRemainingTime();
                    if (expiredTime == null) {
                        expiredTime = new Date();
                    }
                    // 根据传入的金额计算续费时长，并设置新日期
                    int renewalMonths = 0;
                    if (amount.equals("10")) {
                        renewalMonths = 1;
                    }
                    if (amount.equals("25")) {
                        renewalMonths = 3;
                    }
                    if (amount.equals("80")) {
                        renewalMonths = 12;
                    }
                    // 如果已过期，则将商户的到期时间设置为当前时间加上续费时长
                    long currentTimeMillis = System.currentTimeMillis();
                    if (currentTimeMillis >= expiredTime.getTime()) {
                        expiredTime = new Date();
                    }
                    Date newExpiredTime = DateUtil.offsetMonth(expiredTime, renewalMonths);

                    merchant.setRemainingTime(newExpiredTime);
                    merchantService.updateById(merchant);
                }
            }
            return ApiResponse.success("操作成功", "ok");
        } else {
            log.error("签名校验失败:原sign{}:计算sign{}", sign, verifySign);
            return ApiResponse.fail("验签失败");
        }
    }


    @GetMapping("/orderList")
    public TableResponse<List<Order>> orderList(@ModelAttribute OrderDto orderDto) {
        IPage<Order> orderIPage = orderService.pageList(orderDto);
        return TableResponse.success(orderIPage.getTotal(),orderIPage.getRecords());
    }

    @GetMapping("/delOrder")
    public ApiResponse delete(@RequestParam String... tokens) {
        boolean flag = orderService.removeBatchByIds(Arrays.asList(tokens));
        if (flag) {
            return ApiResponse.success();
        }
        return ApiResponse.fail("删除失败");
    }

    /**
     * 回调订单
     * @param tokens 订单唯一标识
     * @return
     */
    @GetMapping("/callback")
    public ApiResponse callback(@RequestParam String... tokens) {
        String merchantId = (String) StpUtil.getLoginId();
        Merchant merchant = merchantService.getById(merchantId);
        String notifyUrl = merchant.getNotifyUrl();
        if (StrUtil.isBlank(notifyUrl)) return ApiResponse.fail("请配置回调地址");
        for (String token : tokens) {
            Order order = orderService.getById(token);
            if (order != null) {
                order.setNotifyUrl(notifyUrl);
                asyncOrderService.sendNotify(order);
            }
        }
        return ApiResponse.success();
    }

    @GetMapping("/orderStatus")
    public ApiResponse orderStatus() {
        String merchantId = (String) StpUtil.getLoginId();
        HashMap<String, Object> resp = new HashMap<>();
        List<Map<String, Object>> weekDataAmount = orderService.getWeekDataAmount(merchantId);
        List<Map<String, Object>> weekDataCount = orderService.getWeekDataCount(merchantId);
        List<String> labels1 = new ArrayList<>();
        List<BigDecimal> data1 = new ArrayList<>();

        List<String> labels2 = new ArrayList<>();
        List<Integer> data2 = new ArrayList<>();
        for (Map<String, Object> map : weekDataAmount) {
            String date =  DateUtil.formatDate((Date) map.get("date"));
            BigDecimal totalAmount = (BigDecimal) map.get("total_amount");
            labels1.add(date);
            data1.add(totalAmount);
        }

        for (Map<String, Object> map : weekDataCount) {
            String date =  DateUtil.formatDate((Date) map.get("date"));
            Long totalCount = (Long) map.get("order_count");
            labels2.add(date);
            data2.add(Math.toIntExact(totalCount));
        }
        resp.put("labels1",labels1);
        resp.put("data1",data1);
        resp.put("labels2",labels2);
        resp.put("data2",data2);
        return ApiResponse.success(resp);
    }

    @GetMapping("/queryOrder")
    public Map<String, Object> queryOrder(@RequestParam String token) {
        HashMap<String, Object> resp = new HashMap<>();
        Order order = orderService.getById(token);
        if (order == null) {
            resp.put("status", false);
        }else {
            resp.put("status", order.getOrderStatus().equals("success"));
        }
        resp.put("code", 200);
        resp.put("success", true);
        return resp;
    }

    @PostMapping("/configuration")
    public ApiResponse configuration(@RequestBody Merchant params) {
        String merchantId = (String) StpUtil.getLoginId();
        Merchant merchant = merchantService.getById(merchantId);
        merchant.setMerchantKey(params.getMerchantKey());
        merchant.setAddress(params.getAddress());
        merchant.setNotifyUrl(params.getNotifyUrl());
        merchantService.updateById(merchant);
        return ApiResponse.success();
    }
}
