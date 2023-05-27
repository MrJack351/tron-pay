package com.tron.pay.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tron.pay.config.TronConfig;
import com.tron.pay.entity.Merchant;
import com.tron.pay.entity.Order;
import com.tron.pay.entity.Transaction;
import com.tron.pay.entity.TransactionTRC20;
import com.tron.pay.service.MerchantService;
import com.tron.pay.service.OrderService;
import com.tron.pay.utils.TronWalletUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AsyncOrderService {

    @Resource
    private OrderService orderService;

    @Resource
    private MerchantService merchantService;

    @Resource
    private TronConfig tronConfig;

    @Resource
    private ScheduledExecutorService executorService;

    private final OkHttpClient client = new OkHttpClient();

    @Async
    public void executeTask(Order order, Merchant merchant) {
        monitorOrder(order, merchant);
    }

    private void monitorOrder(Order order, Merchant merchant) {

        Runnable task = new Runnable() {
            @Override
            public void run() {
                if (isOrderStatusChanged(order)) {
                    orderService.updateById(order);
                    throw new RuntimeException("Order status changed, stopping the task.");
                }

                long endTime = Long.parseLong(order.getFailureTimeStamp());
                if (isOrderExpired(endTime)) {
                    handleOrderFailure(order, merchant);
                    throw new RuntimeException("Order expired, stopping the task.");
                }
                queryAndUpdateOrderStatus(order, merchant);
            }
        };

        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(task, 0, 5, TimeUnit.SECONDS);

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            // 执行异常时取消任务，任务将从线程池中移除
            future.cancel(false);
            log.info("回收线程=======");
        }
    }

    private boolean isOrderStatusChanged(Order order) {
        return "success".equals(order.getOrderStatus()) || "failure".equals(order.getOrderStatus());
    }

    private boolean isOrderExpired(long endTime) {
        return System.currentTimeMillis() > endTime;
    }

    private void handleOrderFailure(Order order, Merchant merchant) {
        merchantService.releaseOrderNumber(merchant.getMerchantName(), order.getNumber());
        order.setOrderStatus("failure");
        orderService.updateById(order);
        log.info("订单支付超时：{}", order);
    }

    private void queryAndUpdateOrderStatus(Order order, Merchant merchant) {
        OkHttpClient client = new OkHttpClient();
        Map<String, String> params = buildQueryParams(order);
        // 构建请求url
        String url = buildUrl(order, merchant.getNetUrl());
        // 构建请求参数
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        addQueryParameters(urlBuilder, params);
        // 构建请求对象
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header("accept", "application/json")
                .get()
                .build();
        try {
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (200 == response.code()) {
                        assert response.body() != null;
                        String responseBody = response.body().string();
                        if ("TRX".equals(order.getCoinType())) {
                            handleTRXTransactions(order, merchant, responseBody);
                        }
                        if ("USDT".equals(order.getCoinType())) {
                            handleTRC20Transactions(order, merchant, responseBody);
                        }
                    }
                    assert response.body() != null;
                    response.body().close(); // 关闭响应体
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> buildQueryParams(Order order) {
        Map<String, String> params = new HashMap<>();
        params.put("only_to", "true");
//        params.put("only_confirmed", "true");
        params.put("limit", "200");
        params.put("min_timestamp", order.getCreateTimeStamp());
        params.put("max_timestamp", order.getFailureTimeStamp());
        return params;
    }

    private String buildUrl(Order order, String netUrl) {
        String url = "";
        if ("TRX".equals(order.getCoinType())) {
            // 判断商户使用的是哪个tron网络
            if (netUrl.contains("nile")) {
                url = tronConfig.getNileNet() + "/v1/accounts/" + order.getCollectionAddress() + "/transactions";
            }else url = tronConfig.getMainNet() + "/v1/accounts/" + order.getCollectionAddress() + "/transactions";
        }
        if ("USDT".equals(order.getCoinType())) {
            // 判断商户使用的是哪个tron网络
            if (netUrl.contains("nile")) {
                url = tronConfig.getNileNet() + "/v1/accounts/" + order.getCollectionAddress() + "/transactions/trc20";
            }else url = tronConfig.getMainNet() + "/v1/accounts/" + order.getCollectionAddress() + "/transactions/trc20";
        }
        return url;
    }

    private void addQueryParameters(HttpUrl.Builder urlBuilder, Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            urlBuilder.addQueryParameter(key, value);
        }
    }

    private void handleTRXTransactions(Order order, Merchant merchant, String responseBody) {
        Transaction result = JSON.parseObject(responseBody, Transaction.class);
        log.info("响应信息：{}", result);

        Predicate<Transaction.DataItem> matchingTransaction = createTRXTransactionPredicate(order);
        List<Transaction.DataItem> items = result.getData().stream()
                .filter(matchingTransaction)
                .collect(Collectors.toList());

        processMatchingTransactions(order, merchant, items);
    }

    private Predicate<Transaction.DataItem> createTRXTransactionPredicate(Order order) {
        Predicate<Transaction.DataItem> hasRawDataAndContract = d -> d.getRaw_data() != null && d.getRaw_data().getContract() != null && !d.getRaw_data().getContract().isEmpty();
        Predicate<Transaction.DataItem> hasParameterAndValue = d -> d.getRaw_data().getContract().get(0).getParameter() != null && d.getRaw_data().getContract().get(0).getParameter().getValue() != null;
        Predicate<Transaction.DataItem> matchingAmount = d -> TronWalletUtils.toTronAmount(order.getPayAmount()).equals(d.getRaw_data().getContract().get(0).getParameter().getValue().getAmount());
        Predicate<Transaction.DataItem> matchingToAddress = d -> TronWalletUtils.convertToBase58(d.getRaw_data().getContract().get(0).getParameter().getValue().getTo_address()).equals(order.getCollectionAddress());
        return hasRawDataAndContract.and(hasParameterAndValue).and(matchingAmount).and(matchingToAddress);
    }

    private void handleTRC20Transactions(Order order, Merchant merchant, String responseBody) {
        TransactionTRC20 result = JSON.parseObject(responseBody, TransactionTRC20.class);
        if (!result.isSuccess()) {
            log.error("查询交易记录失败：{}", result);
        }

        Predicate<TransactionTRC20.DataItem> matchingTransaction = createTRC20TransactionPredicate(order);
        List<TransactionTRC20.DataItem> items = result.getData().stream()
                .filter(matchingTransaction)
                .collect(Collectors.toList());

        processMatchingTransactions(order, merchant, items);
    }

    private Predicate<TransactionTRC20.DataItem> createTRC20TransactionPredicate(Order order) {
        Predicate<TransactionTRC20.DataItem> hasValue = d -> d.getValue() != null;
        Predicate<TransactionTRC20.DataItem> matchingValue = e -> e.getValue().equals(TronWalletUtils.toTronAmount(order.getPayAmount()));
        Predicate<TransactionTRC20.DataItem> matchingToAddress = e -> e.getTo().equals(order.getCollectionAddress());
        return hasValue.and(matchingValue).and(matchingToAddress);
    }

    private void processMatchingTransactions(Order order, Merchant merchant, List<?> items) {
        if (items.size() ==1) {
            updateOrderWithTransaction(order, merchant, items.get(0));
            // 发送异步通知
            sendNotify(order);
            log.info("订单已支付：{}", order);
        } else if (items.size() > 1) {
            log.error("存在多条记录：{}", items);
        } else {
            log.info("订单待支付：{}", order);
        }
    }

    private void updateOrderWithTransaction(Order order, Merchant merchant, Object item) {
        if (item instanceof Transaction.DataItem) {
            Transaction.DataItem trxItem = (Transaction.DataItem) item;
            order.setTransactionId(trxItem.getTxID());
            order.setPaymentAddress(TronWalletUtils.convertToBase58(trxItem.getRaw_data().getContract().get(0).getParameter().getValue().getOwner_address()));
        } else if (item instanceof TransactionTRC20.DataItem) {
            TransactionTRC20.DataItem trc20Item = (TransactionTRC20.DataItem) item;
            order.setTransactionId(trc20Item.getTransaction_id());
            order.setPaymentAddress(trc20Item.getFrom());
        }
        order.setOrderStatus("success");
        merchantService.releaseOrderNumber(merchant.getMerchantName(), order.getNumber());
        orderService.updateById(order);
    }
    public void sendNotify(Order order) {
        HashMap<String, String> params = new HashMap<>();
        String notifyUrl = order.getNotifyUrl();
        if (StrUtil.hasBlank(notifyUrl)) return;
        order.setNotifyCount(order.getNotifyCount() + 1);
        // 构建请求对象
        params.put("token", order.getToken());
        params.put("orderId", order.getOrderId());
        params.put("amount", order.getAmount());
        params.put("payAmount", order.getPayAmount());
        params.put("coinType", order.getCoinType());
        params.put("hash", order.getTransactionId());
        params.put("sign", order.getSign());
        if ("renewal".equals(order.getOrderType())) {
            params.put("orderType", order.getOrderType());
            params.put("renewalId", order.getRenewalId());
        }
        // 将参数Map转换为JSON字符串
        String jsonParams = JSON.toJSONString(params);
        // 创建JSON请求体
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonParams);
        Request request = new Request.Builder()
                .url(notifyUrl)
                .header("accept", "application/json")
                .post(requestBody)
                .build();
        try {
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("回调失败，请求url:{}出错,请求信息：{}", order.getNotifyUrl(), request);
                    order.setNotifyStatus("failure");
                    orderService.updateById(order);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (200 == response.code() && responseBody != null) {
                            String responseBodyStr = responseBody.string();
                            try {
                                JSONObject jsonResponse = JSON.parseObject(responseBodyStr);
                                int code = jsonResponse.getInteger("code");
                                String data = jsonResponse.getString("data");
                                if (code == 200 && "ok".equalsIgnoreCase(data)) {
                                    // 通知成功
                                    log.info("回调成功！{}", responseBodyStr);
                                    order.setState("ok");
                                    order.setNotifyStatus("success");
                                } else {
                                    // 通知失败
                                    log.error("回调失败,返回响应信息有误：{}", responseBodyStr);
                                    order.setNotifyStatus("failure");
                                }
                                orderService.updateById(order);
                            } catch (Exception e) {
                                log.error("回调失败！解析 JSON 响应失败：{}" , responseBodyStr);
                                order.setNotifyStatus("failure");
                                orderService.updateById(order);
                            }
                        } else {
                            log.error("回调失败！响应状态码：{}" ,response.code() );
                            order.setNotifyStatus("failure");
                            orderService.updateById(order);
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error("回调失败，发送请求时异常：{}" , request);
            order.setNotifyStatus("failure");
            orderService.updateById(order);
        }
    }

    /**
     * 校验钱包地址是否合法
     */
    public boolean validateAddress(String address , String netUrl) {
        boolean result = false;
        HashMap<String, String> params = new HashMap<>();
        // 构建请求对象
        params.put("address", address);
        // 将参数Map转换为JSON字符串
        String jsonParams = JSON.toJSONString(params);
        String url = "";
        // 判断使用的是哪个tron网络
        if (netUrl.contains("nile")) {
            url = tronConfig.getNileNet() + "/wallet/validateaddress";
        }else url = tronConfig.getMainNet() + "/wallet/validateaddress";
        // 创建JSON请求体
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonParams);
        Request request = new Request.Builder()
                .url(url)
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .post(requestBody)
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (response.code() == 200) {
                String responseBody = response.body().string();
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(responseBody);
                result = rootNode.get("result").asBoolean();
            }else {
                log.error("校验地址失败，响应状态码非200：{}", response);
            }
        } catch (Exception e) {
            log.error("校验地址失败，请求时发生异常：{}", response);
        }finally {
            assert response != null;
            response.close();
        }
        return result;
    }
}