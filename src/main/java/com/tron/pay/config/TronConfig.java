package com.tron.pay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tron")
@Data
public class TronConfig {

    private String apiKey;
    private String mainNet;
    private String shastaNet;
    private String nileNet;
}
