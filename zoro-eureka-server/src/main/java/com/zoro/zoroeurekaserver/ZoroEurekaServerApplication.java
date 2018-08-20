package com.zoro.zoroeurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class ZoroEurekaServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZoroEurekaServerApplication.class, args);
	}
}
