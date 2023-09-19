package com.ezhixuan.seckilldemo;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ezhixuan.seckilldemo.entity.SeckillVoucher;
import com.ezhixuan.seckilldemo.entity.VoucherOrder;
import com.ezhixuan.seckilldemo.service.SeckillVoucherService;
import com.ezhixuan.seckilldemo.service.VoucherOrderService;
import com.ezhixuan.seckilldemo.service.VoucherService;
import com.ezhixuan.seckilldemo.utils.OrderIdFactory;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.Random;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest
@Slf4j
class SeckillDemoApplicationTests {

  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private SeckillVoucherService seckillVoucherService;
  @Resource private VoucherService voucherService;
  @Resource private VoucherOrderService voucherOrderService;
  @Resource private KafkaTemplate<String, String> kafkaTemplate;

  @Test
  void contextLoads() {}

  /** seckill测试 */
  @Test
  void test1() {
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
      return;
    }
    // 1.2 如果结束时间在当前时间之前，则表示活动已经结束
    if (seckillVoucher.getEndTime().before(new Date())) {
      System.out.println("抢购失败，活动已经结束");
      return;
    }
    // 1.3 如果当前库存小于0，则表示活动已结束
    if (stock <= 0) {
      System.out.println("抢购失败，活动已结束");
      return;
    }
    // 2. 查询用户是否已经购买过
    Boolean flag =
        (Boolean) redisTemplate.opsForHash().get("seckill:order", Integer.toString(userId));
    if (Boolean.TRUE.equals(flag)) {
      System.out.println("您已经购买过了");
      return;
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
      System.out.println("库存不足，活动已结束");
      return;
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
  }

  /** seckill测试2 */
  private static final Long SUCCESS = 1L;

  @Test
  void test2() {
    // 初始化变量
    Random random = new Random();
    Long voucherId = 10L;
    int userId = 1;
    log.info("用户{}开始抢购代金券{}", userId, voucherId);
    // 1. 查询对应代金券信息
    SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
    Integer stock = seckillVoucher.getStock();
    // 1.1 如果开始时间在当前时间之后，则表示活动还未开始
    if (seckillVoucher.getBeginTime().after(new Date())) {
      System.out.println("抢购失败，活动还未开始");
      return;
    }
    // 1.2 如果结束时间在当前时间之前，则表示活动已经结束
    if (seckillVoucher.getEndTime().before(new Date())) {
      System.out.println("抢购失败，活动已经结束");
      return;
    }
    // 1.3 如果当前库存小于0，则表示活动已结束
    if (stock <= 0) {
      System.out.println("抢购失败，活动已结束");
      return;
    }
    // 2. 查询用户是否已经购买过
    Long add = redisTemplate.opsForSet().add("seckill:order", userId);
    log.info("add:{}", add);
    if (!SUCCESS.equals(add)) {
      System.out.println("您已经购买过了");
      return;
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
      System.out.println("库存不足，活动已结束");
      return;
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
    // 4. 返回订单号
    System.out.println("抢购成功！订单号：" + orderId);
  }

  /** seckill测试3 */
  private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

  static {
    SECKILL_SCRIPT = new DefaultRedisScript<>();
    SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/SeckillLua.lua"));
    SECKILL_SCRIPT.setResultType(Long.class);
  }

  private VoucherOrderService proxy;
  
  @Test
  void test3() {
    // 初始化变量
    Random random = new Random();
    Long voucherId = 10L;
    int userId = 2;
    log.info("用户{}开始抢购代金券{}", userId, voucherId);
    // 1. redisLua处理
    Long execute =
        redisTemplate.execute(
            SECKILL_SCRIPT,
            Collections.emptyList(),
            voucherId.toString(),
            Integer.toString(userId),
            LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
    assert execute != null;
    log.info("execute = {}", execute);
    if (!execute.equals(0L)) {
      if (execute.equals(1L)) {
        System.out.println("活动未开始");
      } else if (execute.equals(2L)) {
        System.out.println("活动已结束");
      } else if (execute.equals(3L)) {
        System.out.println("库存不足");
      } else if (execute.equals(4L)) {
        System.out.println("用户已购买");
      }
      System.out.println("抢购失败，活动已结束");
      return;
    }
    // 2. 生成
    // 2.1 生成订单
    OrderIdFactory orderIdFactory = new OrderIdFactory(redisTemplate);
    Long orderId = orderIdFactory.getOrderId();
    proxy = (VoucherOrderService) AopContext.currentProxy();
    log.info("用户{}抢购代金券{}成功", userId, voucherId);
    // 2.2 使用kafka发送消息
    VoucherOrder voucherOrder = new VoucherOrder();
    voucherOrder.setId(orderId);
    voucherOrder.setUserId(userId);
    voucherOrder.setVoucherId(voucherId);
    kafkaTemplate.send("seckillVoucher", JSONUtil.toJsonStr(voucherOrder));
    // 3. 返回订单号
    System.out.println("抢购成功！订单号：" + orderId);
  }

  /**
   * kafka消息监听器
   *
   * @param message
   */
  @KafkaListener(topics = "seckillVoucher")
  public void onMessage(String message) {
    if (StrUtil.isNotBlank(message)) {
      VoucherOrder voucherOrder = JSONUtil.toBean(message, VoucherOrder.class);
      proxy.saveOrder(voucherOrder);
    }
  }

  public void saveOrder(VoucherOrder order) {
    // 初始化一些变量
    int userId = 2;
    // 1 扣减库存
    boolean update =
        seckillVoucherService.update(
            Wrappers.<SeckillVoucher>lambdaUpdate()
                .setSql("stock = stock - 1")
                .eq(SeckillVoucher::getVoucherId, order.getVoucherId())
                .gt(SeckillVoucher::getStock, 0));
    log.info("update:{}", update);
    if (!update) {
      System.out.println("库存不足，活动已结束");
      return;
    }
    // 2. 保存
    order.setPayType(1);
    order.setStatus(1);
    order.setCreateTime(new Date());
    order.setUpdateTime(new Date());
    voucherOrderService.save(order);
  }

  @Test
  void getTime() {
    OrderIdFactory orderIdFactory = new OrderIdFactory(redisTemplate);
    Long orderId = orderIdFactory.getOrderId();
    System.out.println(orderId);
  }

  @Test
  /** 初始化缓存 */
  void saveCache() {
    redisTemplate.opsForHash().put("seckill:10", "stock", 100);
    redisTemplate
        .opsForHash()
        .put(
            "seckill:10",
            "beginTime",
            LocalDateTime.of(2023, 9, 15, 0, 0).toEpochSecond(ZoneOffset.UTC));
    redisTemplate
        .opsForHash()
        .put(
            "seckill:10",
            "endTime",
            LocalDateTime.of(2023, 9, 30, 0, 0).toEpochSecond(ZoneOffset.UTC));
  }
}
