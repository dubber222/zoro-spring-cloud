package com.zoro.zororibbonclient.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Created on 2018/8/20.
 *
 * @author dubber
 */

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
