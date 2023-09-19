package com.ezhixuan.seckilldemo.utils;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @program: seckill-demo
 * @description: 全局订单id生成工具
 * @author: Mr.Xuan
 * @create: 2023-09-18 16:47
 */
public class OrderIdFactory {

  /** 2023-01-01 00:00:00 LocalDateTime.of(2023,1,1,0,0,0).toEpochSecond(ZoneOffset.UTC); */
  private static final Long BEGIN = 1672531200L;

  private RedisTemplate redisTemplate;

  private static final Integer BIT = 32;

  public OrderIdFactory(RedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public Long getOrderId() {
    // 1. 获取当前时间
    LocalDateTime now = LocalDateTime.now();
    long nowEpochSecond = now.toEpochSecond(ZoneOffset.UTC);
    // 2. 计算时间戳
    long timeStamp = BEGIN - nowEpochSecond;
    // 3. 获取当前时间并序列化作为key
    String format = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
    // 4. 获取自增
    Long increment = redisTemplate.opsForValue().increment("order:" + format);
    return timeStamp << BIT | increment;
  }
}
