package com.tron.pay.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tron.pay.entity.Merchant;
import com.tron.pay.entity.Order;
import com.tron.pay.service.MerchantService;
import com.tron.pay.service.OrderService;
import com.tron.pay.utils.CommonUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.Date;


@Controller
public class PageController {

    @Resource
    private OrderService orderService;

    @Resource
    private MerchantService merchantService;

    @GetMapping("/index")
    public String index(Model model, HttpServletResponse response) {
        // 判断是否登录
        if (checkIsLogin(response)) return null;
        model.addAttribute("logoutUrl", "views/logout");
        String merchantId = (String) StpUtil.getLoginId();
        Merchant merchant = merchantService.getById(merchantId);
        model.addAttribute("merchantName",merchant.getMerchantName());
        model.addAttribute("home","/home");
        return "views/index";
    }

    @GetMapping("/main")
    public String main(Model model, HttpServletResponse response) {
        if (checkIsLogin(response))return null;
        String merchantId = (String) StpUtil.getLoginId();
        Merchant merchant = merchantService.getById(merchantId);
        // 今日成交额
        BigDecimal todayTurnover = orderService.getTodayTurnover(merchantId);
        // 总成交额
        BigDecimal allTurnover = orderService.getAllTurnover(merchantId);
        // 今日订单数
        int dailyOrderCount = orderService.getDailyOrderCount(merchantId);
        // 总订单数
        int totalOrderCount = orderService.getTotalOrderCount(merchantId);
        // 近期订单
        IPage<Order> orders = orderService.getRecentlyOrderList();
        model.addAttribute("todayTurnover",todayTurnover);
        model.addAttribute("allTurnover",allTurnover);
        model.addAttribute("dailyOrderCount",dailyOrderCount);
        model.addAttribute("totalOrderCount",totalOrderCount);
        model.addAttribute("orders",orders.getRecords());
        model.addAttribute("remainingTime", DateUtil.format(merchant.getRemainingTime(),"yyyy-MM-dd HH:mm:ss"));
        return "views/main";
    }

    @GetMapping("/")
    public String localhost(Model model, HttpServletResponse response) {
        return "views/tronpay/user/login";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("registerUrl", "/register");
        return "views/tronpay/user/login";
    }

    @GetMapping("/logout")
    public String logout(Model model) {
        StpUtil.logout();
        return "views/tronpay/user/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("loginUrl", "/login");
        return "views/tronpay/user/reg";
    }

    @GetMapping("/user/info")
    public String userInfo(Model model, HttpServletResponse response) {
        if (checkIsLogin(response))return null;
        String merchantId = (String) StpUtil.getLoginId();
        Merchant merchant = merchantService.getById(merchantId);
        model.addAttribute("merchantName",merchant.getMerchantName());
        model.addAttribute("merchantKey",merchant.getMerchantKey());
        model.addAttribute("address",merchant.getAddress());
        model.addAttribute("notifyUrl",merchant.getNotifyUrl());
        model.addAttribute("netUrl",merchant.getNetUrl());
        model.addAttribute("expiredTime",DateUtil.format(merchant.getRemainingTime(),"yyyy-MM-dd HH:mm:ss"));
        // 获取商户的到期时间
        Date remainingTime = merchant.getRemainingTime();
        if (remainingTime == null || remainingTime.before(new Date())) {
            // 如果到期时间为空或者早于当前时间，则说明套餐已过期
            // 动态设置class样式
            model.addAttribute("cName","mt-1 mb-0 badge badge-danger");
        }else {
            model.addAttribute("cName","mt-1 mb-0 badge badge-success");
        }
        boolean net = true;
        String netUrl = merchant.getNetUrl();
        // 判断是哪个网络
        if (netUrl.contains("nile")) {
            net = false;
        }
        model.addAttribute("isMainNet",net);
        return "views/tronpay/user/userInfo";
    }

    @GetMapping("/user/password")
    public String setUserPassword(Model model, HttpServletResponse response) {
        if (checkIsLogin(response))return null;
        return "views/tronpay/user/editPassword";
    }

    // 商户充值
//    @GetMapping("/user/recharge")
//    public String recharge(Model model, HttpServletResponse response) {
//        if (checkIsLogin(response))return null;
//        return "views/tronpay/user/userRecharge";
//    }

    // 接入文档
    @GetMapping("/access-document")
    public String accessDocument(Model model, HttpServletResponse response) {
        if (checkIsLogin(response))return null;
        return "views/tronpay/document/access-document";
    }

    @GetMapping("/order-list")
    public String orderList(Model model, HttpServletResponse response) {
        if (checkIsLogin(response))return null;
        return "views/tronpay/order/orderList";
    }

    @GetMapping("/order-statistics")
    public String orderStatistics(Model model, HttpServletResponse response) {
        if (checkIsLogin(response))return null;
        return "views/tronpay/order/orderStatistics";
    }

    @GetMapping("/pay/success")
    public String success(Model model) {

        return "views/tronpay/order/success";
    }

    @GetMapping("/pay/timeout")
    public String failure(Model model) {

        return "views/tronpay/order/timeout";
    }


    @GetMapping("/pay/wait")
    public String mobilePay(Model model, @RequestParam("token") String token, HttpServletRequest request) {
        String url = "mobilePay";
        String deviceType = "mobile";
        Order order = orderService.getById(token);
        if (order == null) {
            return "error/404";
        }
        boolean mobileDevice = CommonUtils.isMobileDevice(request);
        if (!mobileDevice) {
            url = "pcPay";
            deviceType = "pc";
            try {
                byte[] imageData = CommonUtils.generateQRCode(order.getCollectionAddress(), 600, 600);
                String base64Image = Base64.getEncoder().encodeToString(imageData);
                model.addAttribute("qrcode", base64Image);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        model.addAttribute("type",deviceType);
        model.addAttribute("title","收银台");
        model.addAttribute("address",order.getCollectionAddress());
        model.addAttribute("amount", order.getPayAmount());
        model.addAttribute("coinType", order.getCoinType());
        model.addAttribute("createTime", CommonUtils.convertMillisecondsToDateString(order.getCreateTimeStamp()));
        model.addAttribute("endTime", CommonUtils.convertMillisecondsToDateString(order.getFailureTimeStamp()));
        return "views/tronpay/order/" + url;
    }

    private boolean checkIsLogin(HttpServletResponse response) {
        if (!StpUtil.isLogin()) {
            try {
                response.sendRedirect("login");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return !StpUtil.isLogin();
    }
}
