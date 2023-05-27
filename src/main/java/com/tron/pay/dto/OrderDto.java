package com.tron.pay.dto;

import com.tron.pay.common.Pagination;
import lombok.Data;

@Data
public class OrderDto extends Pagination {

    private String token;

    private String orderId;

    private String transactionId;

    private String orderStatus;
}
