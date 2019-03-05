package com.heyiming.seckilling.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SeckillingServiceImpl implements SeckillingService{

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void prepareInventory() {
        for(int i = 0; i < 1000; i ++) {
            redissonClient.getQueue("inventory").add(1);
        }
        log.info("prepare success for key reserve");
    }

    @Override
    public boolean reserve(String member) {
        int remain = redissonClient.getQueue("inventory").size();
        log.info(String.valueOf(remain));
        if(remain <= 0){
            return false;
        }
        Object result = redissonClient.getQueue("inventory").poll();
        if(result != null){
            redissonClient.getQueue("members").add(member);
            log.info("get success");
        }else{
            log.info("get failed");
        }
        return result != null;
    }

    @Override
    public List<String> getSuccessMember() {
        return null;
    }
}
