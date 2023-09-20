package com.ezhixuan.seckilldemo.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ezhixuan.seckilldemo.entity.SeckillVoucher;
import com.ezhixuan.seckilldemo.entity.VoucherOrder;
import com.ezhixuan.seckilldemo.mapper.VoucherOrderMapper;
import com.ezhixuan.seckilldemo.service.SeckillVoucherService;
import com.ezhixuan.seckilldemo.service.VoucherOrderService;
import com.ezhixuan.seckilldemo.utils.OrderIdFactory;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.Random;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author ezhixuan
 * @description 针对表【tb_voucher_order】的数据库操作Service实现
 * @createDate 2023-09-18 16:42:06
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder>
    implements VoucherOrderService {

  @Resource private SeckillVoucherService seckillVoucherService;
  private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
  @Resource private RedisTemplate<String, Object> redisTemplate;
  //  @Resource private KafkaTemplate<String, String> kafkaTemplate;
  @Resource private RabbitTemplate rabbitTemplate;

  static {
    SECKILL_SCRIPT = new DefaultRedisScript<>();
    SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/SeckillLua.lua"));
    SECKILL_SCRIPT.setResultType(Long.class);
  }

  private VoucherOrderService proxy;

  @Override
  public void seckillVoucher() {
    // 初始化变量
    Random random = new Random();
    Long voucherId = 10L;
    int userId = (random.nextInt(20000) + 20000) % 20000;
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
    //    proxy = (VoucherOrderService) AopContext.currentProxy();
    log.info("用户{}抢购代金券{}成功", userId, voucherId);
    // 2.2 使用kafka发送消息
    VoucherOrder voucherOrder = new VoucherOrder();
    voucherOrder.setId(orderId);
    voucherOrder.setUserId(userId);
    voucherOrder.setVoucherId(voucherId);
    //    kafkaTemplate.send("seckillVoucher", JSONUtil.toJsonStr(voucherOrder));
    rabbitTemplate.convertAndSend("order.seckill", JSONUtil.toJsonStr(voucherOrder));
    // 3. 返回订单号
    System.out.println("抢购成功！订单号：" + orderId);
  }

  //  /**
  //   * kafka消息监听器
  //   *
  //   * @param message
  //   */
  //  @KafkaListener(topics = "seckillVoucher")
  //  public void onMessage(String message) {
  //    if (StrUtil.isNotBlank(message)) {
  //      VoucherOrder voucherOrder = JSONUtil.toBean(message, VoucherOrder.class);
  ////      proxy.saveOrder(voucherOrder);
  //    }
  //  }

  /**
   * 保存订单信息
   *
   * @param order
   */
  @Override
  @Transactional
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
    save(order);
  }
}
