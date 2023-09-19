package com.ezhixuan.seckilldemo.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ezhixuan.seckilldemo.entity.SeckillVoucher;
import com.ezhixuan.seckilldemo.entity.VoucherOrder;
import com.ezhixuan.seckilldemo.service.SeckillVoucherService;
import com.ezhixuan.seckilldemo.service.VoucherOrderService;
import com.ezhixuan.seckilldemo.utils.OrderIdFactory;
import java.util.Date;
import java.util.Random;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: seckill-demo
 * @description: 测试秒杀模式
 * @author: Mr.Xuan
 * @create: 2023-09-18 20:59
 */
@RestController
@RequestMapping("/seckill")
@Slf4j
public class SeckillController {

  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private SeckillVoucherService seckillVoucherService;
  @Resource private VoucherOrderService voucherOrderService;

  @GetMapping("/test1")
  public String test1() {
    // 初始化变量
    Random random = new Random();
    Long voucherId = 10L;
    int userId = (random.nextInt(20000) + 20000) % 20000;
    log.info("用户{}开始抢购代金券{}", userId, voucherId);
    // 1. 查询对应代金券信息
    SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
    Integer stock = seckillVoucher.getStock();
    // 1.1 如果开始时间在当前时间之后，则表示活动还未开始
    if (seckillVoucher.getBeginTime().after(new Date())) {
      System.out.println("抢购失败，活动还未开始");
      return "抢购失败，活动还未开始";
    }
    // 1.2 如果结束时间在当前时间之前，则表示活动已经结束
    if (seckillVoucher.getEndTime().before(new Date())) {
      System.out.println("抢购失败，活动已经结束");
      return "抢购失败，活动已经结束";
    }
    // 1.3 如果当前库存小于0，则表示活动已结束
    if (stock <= 0) {
      System.out.println("抢购失败，活动已结束");
      return "库存不足，活动已结束";
    }
    // 2. 查询用户是否已经购买过
    Boolean flag =
        (Boolean) redisTemplate.opsForHash().get("seckill:order", Integer.toString(userId));
    if (Boolean.TRUE.equals(flag)) {
      System.out.println("您已经购买过了");
      return "您已经购买过了";
    }
    // 3. 生成
    // 3.1 扣减库存
    boolean update =
        seckillVoucherService.update(
            Wrappers.<SeckillVoucher>lambdaUpdate()
                    .setSql("stock = stock - 1")
                .eq(SeckillVoucher::getVoucherId, voucherId)
                .gt(SeckillVoucher::getStock, 0));
    log.info("update:{}", update);
    if (!update) {
      return "库存不足，活动已结束";
    }
    // 3.2 生成订单
    OrderIdFactory orderIdFactory = new OrderIdFactory(redisTemplate);
    Long orderId = orderIdFactory.getOrderId();
    VoucherOrder voucherOrder = new VoucherOrder();
    voucherOrder.setId(orderId);
    voucherOrder.setUserId(userId);
    voucherOrder.setVoucherId(voucherId);
    voucherOrder.setPayType(1);
    voucherOrder.setStatus(1);
    voucherOrder.setCreateTime(new Date());
    voucherOrder.setUpdateTime(new Date());
    log.info("用户{}抢购代金券{}成功", userId, voucherId);
    try {
      voucherOrderService.save(voucherOrder);
    } catch (Exception e) {
      log.error("用户{}抢购代金券{}成功", userId, voucherId);
      throw new RuntimeException(e);
    }
    // 3.3 将用户购买信息存入redis
    redisTemplate.opsForHash().put("seckill:order", Integer.toString(userId), true);
    // 4. 返回订单号
    System.out.println("抢购成功！订单号：" + orderId);
    return "抢购成功！订单号：" + orderId;
  }
}
