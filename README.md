# zoro-spring-cloud

#一、springcloud学习

包括三个项目  zoro-eureka-server，zoro-ribbon-client，zoro-eureka-register01

* zoro-eureka-server  --> 注册中心
* zoro-eureka-register01 --> 生产者（<u>服务发现</u>）
* zoro-ribbon-client  --> 消费者（服务发现和<u>消费</u>）

## 1、注册中心

创建两个application.properties文件, application-peer1.properties、application-peer2.properties；

###（1）高可用注册中心

​	EurekaServer的设计一开始就考虑了高可用问题， 在Eureka的服务治理设计中， 所有 节点即是服务提供方， 也是服务消费方， 服务注册中心也不例外; 

​	EurekaServer的高可用实际上就是将自己作为服务向其他服务注册中心注册自己， 这 样就可以形成一组互相注册的服务注册中心， 以实现服务清单的互相同步， 达到高可用的 效果。 下面我们就来尝试搭建高可用服务注册中心的集群。 可以在本章第1节中实现的服 务注册中心的基础之上进行扩展， 构建一个双节点的服务注册中心集群。    

* 创建application-peer1.properties, 作为 peer1 服务中心的配置， 并将 serviceUri指向peer2

```java
spring.application.name=eureka-server  ## 应用名称
server.port=1111  ## tomcat容器端口
eureka.instance.hostname=peer1  ## 主机名称，需要在 hosts 中配置

eureka.client.serviceUrl.defaultZone=http://peer2:1112/eureka/ ## 注册中心
```

* 创建application-peer2.properties, 作为 peer2 服务中心的配置， 并将 serviceUri指向peer1

```java
spring.application.name=eureka-server  ## 应用名称
server.port=1112  ## tomcat容器端口
eureka.instance.hostname=peer2  ## 主机名称，需要在 hosts 中配置

eureka.client.serviceUrl.defaultZone=http://peer1:1111/eureka/ ## 注册中心
```

* 在 /etc/hosts 文件中添加对peerl 和 peer2的转换， 让上面配置的 host 形式的 serviceUrl 能在本地正确访问到； Windows系统路径为C:\Windows\System32\ drivers\etc\hosts。   

  ```java
  127.0.0.1 peer1
  127.0.0.1 peer2
  ```

* 通过 spring.profiles.active 属性来分别启动 peer1、peer2:

  ```java
  java -jar zoro-eureka-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=peer1
  java -jar zoro-eureka-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=peer2
  ```



## 2、服务发现和消费

​	服务发现的任务由 Eureka 的客户端完成，而服务消费的任务由 Ribbon 完成。Ribbon是一个基于 HTTP 和 TCP 的客户端负载均衡器， 它可以在通过客户端中配置的 ribbonServerList 服务端列表去轮询访问以达到均衡负载 的作用。 当Ribbon与Eureka联合使用时，Ribbon 的服务实例清单 RibbonServerList 会被 DiscoveryEnabledNIWSServerList 重 写， 扩展成从Eureka注册中心中获取服务端列表。 同时它也会用 NIWSDiscoveryPing 来取代 IPing, 它将职责委托给 Eureka 来确定服务端是否已经启动 。 在本章中， 我们对 Ribbon不做详细的介绍，读者只需要理解它在Eureka服务发现的基础上，实现了 一套对服 务实例的选择策略，从而实现对服务的消费。下一 章我们会对Ribbon做详细的介绍和分析。   

####注册服务

* 首先，我们做 一些准备工作 。启动之前实现的服务注册中心 zoro-eureka-server 以及 zoro-eureka-register01 服务， 为了实验 Ribbon 的客户端负载均衡功能， 我们通过 java -jar 命令行的方式来启动两个不同端口的 zoro-eureka-register01, 具体如下： 

  ```java
  java -jar zoro-eureka-register01-0.0.1-SNAPSHOT.jar --server.port=8081
  java -jar zoro-eureka-register01-0.0.1-SNAPSHOT.jar --server.port=8082
  ```

* application.properties配置,其中 ``spring.application.name=REGISTERE01 ##（应用名称）服务名称``  ``REGISTERE01``非常重要，消费者以 ``http://REGISTERE01/hello`` 这种形式去调用生产者的方法。

  ```java
  server.port=2222
  
  server.register1.port=1111
  eureka.register1.hostname=peer1
  server.register2.port=1112
  eureka.register2.hostname=peer2
  eureka.client.serviceUrl.defaultZone=http://${eureka.register1.hostname}:${server.register1.port}/eureka/,http://${eureka.register2.hostname}:${server.register2.port}/eureka/  ## 多注册中心（高可用注册中心）
  
  spring.application.name=REGISTERE01 ##（应用名称）服务名称
  
  logging.config=classpath:log4j2.xml
  ```

####消费服务

​	创建应用主类 ConsumerApplication, 通过 @EnableDiscoveryClient 注解 让该应用注册为 Eureka 客户端应用， 以获得服务发现的能力。 同时， 在该主类中创建 RestTemplate 的 Spring Bean 实例，并通过 @LoadBalanced 注解开启客户端 ``负载均衡``。    

```java
@EnableEurekaClient
@SpringBootApplication
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

​	创建ConsumerController 类并实现／Ribbon-consumer接口。 在该接口中， 通过在上面创 建的RestTemplate 来实现对 服 务提供的 REGISTERE01/hello接口进行调用。 可以看到这里访问的地址是服务名REGISTERE01 , 而 不是一个具体的地址，在服务治理框架中， 这是一个非常重要的特性，也符合在本 章 一开始对``服务治理``的解释。    

ConsumerController.java

```java
@RestController
public class ConsumerController {

    @Autowired
    RestTemplate restTemplate;


    @RequestMapping(value = "/ribbon-consumer", method = RequestMethod.GET)
    public String helloConsumer() {
        return restTemplate.getForEntity("http://REGISTERE01/hello", String.class)
                .getBody();
    }
}
```











