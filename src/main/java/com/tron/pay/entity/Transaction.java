package com.tron.pay.entity;

import lombok.Data;

import java.util.List;

@Data
public class Transaction {
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
        private List<Result> ret;
        private List<String> signature;
        private String txID;
        private int net_usage;
        private String raw_data_hex;
        private int net_fee;
        private int energy_usage;
        private long blockNumber;
        private long block_timestamp;
        private int energy_fee;
        private int energy_usage_total;
        private RawData raw_data;
        private List<Object> internal_transactions;

        @Data
        public static class Result {
            private String contractRet;
            private int fee;
        }

        @Data
        public static class RawData {
            private List<Contract> contract;
            private String ref_block_bytes;
            private String ref_block_hash;
            private long expiration;
            private long timestamp;

            @Data
            public static class Contract {
                private Parameter parameter;
                private String type;

                @Data
                public static class Parameter {
                    private Value value;
                    private String type_url;

                    @Data
                    public static class Value {
                        private String amount;
                        private String asset_name;
                        private String owner_address;
                        private String to_address;
                    }
                }
            }
        }
    }
}
