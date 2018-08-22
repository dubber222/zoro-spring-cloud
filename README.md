# zoro-spring-cloud  --- springcloud学习



## 第3章 服务治理：Spring Cloud Eureka

包括三个项目  zoro-eureka-server，zoro-ribbon-client，zoro-eureka-register01

* zoro-eureka-server  --> 注册中心
* zoro-eureka-register01 --> 生产者（<u>服务发现</u>）
* zoro-ribbon-client  --> 消费者（服务发现和<u>消费</u>）

### 1、注册中心

创建两个application.properties文件, application-peer1.properties、application-peer2.properties；

####（1）高可用注册中心

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



### 2、服务发现和消费

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

* 创建应用主类 ConsumerApplication, 通过 @EnableDiscoveryClient 注解 让该应用注册为 Eureka 客户端应用， 以获得服务发现的能力。 同时， 在该主类中创建 RestTemplate 的 Spring Bean 实例，并通过 @LoadBalanced 注解开启客户端 ``负载均衡``。    

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

* 创建ConsumerController 类并实现／Ribbon-consumer接口。 在该接口中， 通过在上面创 建的RestTemplate 来实现对 服 务提供的 REGISTERE01/hello接口进行调用。 可以看到这里访问的地址是服务名REGISTERE01 , 而 不是一个具体的地址，在服务治理框架中， 这是一个非常重要的特性，也符合在本 章 一开始对``服务治理``的解释。    

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



* 通过向 http://localhost： 3333/ribbon-consumer 发起 GET 请求， 成功返 回了 "Hello World"。 此时， 我们可以在 ribbon-consumer 应用的控制台中看到 如下信息， Ribbon 输出了当前客户端维护的 REGISTERE01  的服务列表情况。其 中包含了各个实例的位置， 和 Ribbon 就是按照此信息进行轮询访问， 以实现基于客户 端的负载均衡。 另外还输出了一些其他非常有用的信息， 如对各个实例的请求总数 量、 第 一次连接信息、 上 一次连接信息、 总的请求失败数量等。    

```java
2018-08-21 11:36:37.166  INFO 11420 --- [nio-3333-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring FrameworkServlet 'dispatcherServlet'
2018-08-21 11:36:37.166  INFO 11420 --- [nio-3333-exec-1] o.s.web.servlet.DispatcherServlet        : FrameworkServlet 'dispatcherServlet': initialization started
2018-08-21 11:36:37.220  INFO 11420 --- [nio-3333-exec-1] o.s.web.servlet.DispatcherServlet        : FrameworkServlet 'dispatcherServlet': initialization completed in 53 ms
2018-08-21 11:36:57.663  INFO 11420 --- [nio-3333-exec-5] s.c.a.AnnotationConfigApplicationContext : Refreshing org.springframework.context.annotation.AnnotationConfigApplicationContext@792f010: startup date [Tue Aug 21 11:36:57 CST 2018]; parent: org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext@51b01960
2018-08-21 11:36:57.759  INFO 11420 --- [nio-3333-exec-5] f.a.AutowiredAnnotationBeanPostProcessor : JSR-330 'javax.inject.Inject' annotation found and supported for autowiring
2018-08-21 11:36:58.225  INFO 11420 --- [nio-3333-exec-5] c.netflix.config.ChainedDynamicProperty  : Flipping property: REGISTERE01.ribbon.ActiveConnectionsLimit to use NEXT property: niws.loadbalancer.availabilityFilteringRule.activeConnectionsLimit = 2147483647
2018-08-21 11:36:58.288  INFO 11420 --- [nio-3333-exec-5] c.n.u.concurrent.ShutdownEnabledTimer    : Shutdown hook installed for: NFLoadBalancer-PingTimer-REGISTERE01
2018-08-21 11:36:58.405  INFO 11420 --- [nio-3333-exec-5] c.netflix.loadbalancer.BaseLoadBalancer  : Client:REGISTERE01 instantiated a LoadBalancer:DynamicServerListLoadBalancer:{NFLoadBalancer:name=REGISTERE01,current list of Servers=[],Load balancer stats=Zone stats: {},Server stats: []}ServerList:null
2018-08-21 11:36:58.414  INFO 11420 --- [nio-3333-exec-5] c.n.l.DynamicServerListLoadBalancer      : Using serverListUpdater PollingServerListUpdater
2018-08-21 11:36:58.494  INFO 11420 --- [nio-3333-exec-5] c.netflix.config.ChainedDynamicProperty  : Flipping property: REGISTERE01.ribbon.ActiveConnectionsLimit to use NEXT property: niws.loadbalancer.availabilityFilteringRule.activeConnectionsLimit = 2147483647
2018-08-21 11:36:58.497  INFO 11420 --- [nio-3333-exec-5] c.n.l.DynamicServerListLoadBalancer      : DynamicServerListLoadBalancer for client REGISTERE01 initialized: DynamicServerListLoadBalancer:{NFLoadBalancer:name=REGISTERE01,current list of Servers=[purang739.purang.com:8082, purang739.purang.com:8081],Load balancer stats=Zone stats: {defaultzone=[Zone:defaultzone;	Instance count:2;	Active connections count: 0;	Circuit breaker tripped count: 0;	Active connections per server: 0.0;]
},Server stats: [[Server:purang739.purang.com:8081;	Zone:defaultZone;	Total Requests:0;	Successive connection failure:0;	Total blackout seconds:0;	Last connection made:Thu Jan 01 08:00:00 CST 1970;	First connection made: Thu Jan 01 08:00:00 CST 1970;	Active Connections:0;	total failure count in last (1000) msecs:0;	average resp time:0.0;	90 percentile resp time:0.0;	95 percentile resp time:0.0;	min resp time:0.0;	max resp time:0.0;	stddev resp time:0.0]
, [Server:purang739.purang.com:8082;	Zone:defaultZone;	Total Requests:0;	Successive connection failure:0;	Total blackout seconds:0;	Last connection made:Thu Jan 01 08:00:00 CST 1970;	First connection made: Thu Jan 01 08:00:00 CST 1970;	Active Connections:0;	total failure count in last (1000) msecs:0;	average resp time:0.0;	90 percentile resp time:0.0;	95 percentile resp time:0.0;	min resp time:0.0;	max resp time:0.0;	stddev resp time:0.0]
]}ServerList:org.springframework.cloud.netflix.ribbon.eureka.DomainExtractingServerList@ae536bb
2018-08-21 11:36:59.420  INFO 11420 --- [erListUpdater-0] c.netflix.config.ChainedDynamicProperty  : Flipping property: REGISTERE01.ribbon.ActiveConnectionsLimit to use NEXT property: niws.loadbalancer.availabilityFilteringRule.activeConnectionsLimit = 2147483647

```

* 展现负载均衡： 再尝试发送几次请求，启动的两个 生产者会交替执行



### 3、Eureka 详解

#### 基础架构

​	在 “ 服务治理 ”一节中， 我们所讲解的示例虽然简单， 但是麻雀虽小、 五脏俱全。 它 已经包含了整个 Eureka 服务治理基础架构的三个核心要素。 

* `服务注册中心`： Eureka 提供的服务端， 提供服务注册与发现的功能， 也就是在上一 节中我们实现的 eureka-server。 
* `服务提供者`：提供服务的应用， 可以是 Spring Boot 应用， 也可以是其他技术平台且 遵循 Eureka 通信机制的应用。它将自己提供的服务注册到 Eureka, 以供其他应用发 现， 也就是在上一节中我们实现的 HELLO-SERVICE 应用。 
* `服务消费者`：消费者应用从服务注册中心获取服务列表， 从而使消费者可以知道去 何处调用其所需要的服务，在上一节中使用了 Ribbon 来实现服务消费，另外后续还 会介绍使用 Feign 的消费方式。 很多时候， 客户端既是服务提供者也是服务消费者。    

#### 服务治理机制 

​	在体验了 Spring Cloud Eureka 通过简单的注解配置就能实现强大的服务治理功能之 后， 我们来进一步了解一下 Eureka 基础架构中各个元素的一些通信行为， 以此来理解基于 Eureka 实现的服务治理体系是如何运作起来的。 以下图为例， 其中有这样几个重要元素： 

* "服务注册中心-1" 和 “ 服务注册中心-2", 它们互相注册组成了高可用集群。 •
* "服务提供者 ” 启动了两个实例， 一个注册到 “ 服务注册中心-1" 上， 另外一个注 册到 “服务注册中心-2" 上。 
* 还有两个 “ 服务消费者 “， 它们也都分别只指向了一个注册中心。    



