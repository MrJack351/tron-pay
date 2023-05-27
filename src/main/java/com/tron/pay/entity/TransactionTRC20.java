package com.tron.pay.entity;

import lombok.Data;

import java.util.List;

@Data
public class TransactionTRC20 {
    private boolean success;
    private Meta meta;
    private List<DataItem> data;

    @Data
    public static class Meta {
        private long at;
        private int page_size;
    }

    @Data
    public static class DataItem {
        private String transaction_id;
        private TokenInfo token_info;
        private long block_timestamp;
        private String from;
        private String to;
        private String type;
        private String value;
        @Data
        public static class TokenInfo {
            private String symbol;
            private String address;
            private int decimals;
            private String name;
        }

    }
}
