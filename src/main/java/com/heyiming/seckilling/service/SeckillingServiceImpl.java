package com.heyiming.seckilling.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RQueue;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.AvroJacksonCodec;
import org.redisson.codec.FstCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.codec.SerializationCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SeckillingServiceImpl implements SeckillingService{

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void prepareInventory() {
//        for(int i = 0; i < 1000; i ++) {
//            redissonClient.getQueue("inventory").add(1);
//        }
        redissonClient.getBucket("inventory", LongCodec.INSTANCE).set(1000);
        log.info("prepare success for key reserve");
    }

    @Override
    public boolean reserve(List<String> members, String itemKey) {
        RBucket<Long> inventoryBucket = redissonClient.getBucket("inventory", LongCodec.INSTANCE);
        Long inventory = inventoryBucket.get();
        log.info(String.valueOf(inventory));
        if(inventory <= 0){
            return false;
        }

        RLock lock = null;
        try {
        	// lock key can not the same as real key
            lock = redissonClient.getLock("_"+itemKey);
            if(lock.tryLock(1, TimeUnit.SECONDS)){
                inventoryBucket = redissonClient.getBucket("inventory", LongCodec.INSTANCE);
                inventory = inventoryBucket.get();
				RList<Object> memberList = redissonClient.getList(itemKey, StringCodec.INSTANCE);
				for(int i = 0; i < members.size() && inventory > 0; i ++, inventory --){
					String member = members.get(i);
					memberList.add(member);

				}
				redissonClient.getExecutorService(itemKey);
                inventoryBucket.set(inventory);
            }
        } catch (InterruptedException e) {
            log.warn("get lock error: {}", e.getCause());
        } catch (Exception e){
        	log.error("error happened, {}", e.getMessage());
		}finally {
            if(lock != null){
                lock.unlock();
            }
        }
        return true;
    }

    @Override
    public List<String> getSuccessMember(String itemKey) {
		List<String> memberList = redissonClient.getList(itemKey, StringCodec.INSTANCE);
		memberList.sort(String::compareTo);
		return memberList;
	}
}
