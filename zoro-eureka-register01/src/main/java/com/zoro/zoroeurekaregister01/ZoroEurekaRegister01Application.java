package com.zoro.zoroeurekaregister01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@EnableEurekaClient
@SpringBootApplication
public class ZoroEurekaRegister01Application {

    public static void main(String[] args) {
        SpringApplication.run(ZoroEurekaRegister01Application.class, args);
    }
}
