package com.tron.pay.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import com.tron.pay.entity.Transaction;
import okhttp3.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/transaction")
public class TronTransactionController {

    private final String mainNet = "https://apilist.tronscanapi.com/api/transaction";


    @GetMapping("/transactions")
    public Transaction transactions(@RequestParam Map<String, String> paramsMap) {
        String address = paramsMap.get("address");
        String startTimestamp = paramsMap.get("startTimestamp");
        String endTimestamp = paramsMap.get("endTimestamp");

        // 获取今天的日期
        String today = DateUtil.today();

        // 获取今天的开始时间戳（精确到毫秒）
        long beginOfDay = DateUtil.beginOfDay(DateUtil.parse(today)).getTime();

        DateTime dateTime = DateUtil.offsetMinute(DateUtil.date(), 30);

        // 获取今天的结束时间戳（精确到毫秒）
        long endOfDay = DateUtil.endOfDay(DateUtil.parse(today)).getTime();

        OkHttpClient client = new OkHttpClient();
        // 创建请求参数 Map
        Map<String, String> params = new HashMap<>();
        params.put("only_to", "true");
        params.put("limit", "200");
//        params.put("min_timestamp", String.valueOf(beginOfDay));
//        params.put("max_timestamp", String.valueOf(endOfDay));

        String url = "https://api.trongrid.io" + "/v1/accounts/"+ address +"/transactions";
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();

        // 将所有的查询参数添加到URL中
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            urlBuilder.addQueryParameter(key, value);
        }
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header("accept","application/json")
                .get()
                .build();
        Transaction result = new Transaction();
        try (Response response = client.newCall(request).execute()) {
            if (200 == response.code()) {
                String responseBody = response.body().string();
                result = JSON.parseObject(responseBody,Transaction.class);
                result.getData().forEach(e -> {
                    System.out.println(e);
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
