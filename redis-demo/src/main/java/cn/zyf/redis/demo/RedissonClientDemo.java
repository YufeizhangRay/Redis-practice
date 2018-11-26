package cn.zyf.redis.demo;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;


public class RedissonClientDemo {
    public static void main(String[] args) {
        Config config=new Config();
        config.useClusterServers().
                addNodeAddress("redis://192.168.11.153:7000",
                        "redis://192.168.11.153:7001");
        RedissonClient redissonClient=Redisson.create(config);
        /*redissonClient.getLock();//分布式锁
        redissonClient.getBucket("mic").set("value");*/

    }
}
