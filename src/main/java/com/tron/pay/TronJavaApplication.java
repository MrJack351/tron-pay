package com.tron.pay;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.tron.pay.mapper")
@EnableScheduling
public class TronJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TronJavaApplication.class, args);
    }

}
