package com.ezhixuan.seckilldemo.listen;

import cn.hutool.json.JSONUtil;
import com.ezhixuan.seckilldemo.entity.VoucherOrder;
import com.ezhixuan.seckilldemo.service.VoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @program: seckill-demo
 * @description: rabbitMq监听器
 * @author: Mr.Xuan
 * @create: 2023-09-20 22:05
 */
@Component
@Slf4j
public class RabbitListener {

    @Resource private VoucherOrderService voucherOrderService;

    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "order.seckill")
    public void listenMessage(String onMessage){
        log.info("秒杀订单生成中");
        VoucherOrder voucherOrder = JSONUtil.toBean(onMessage, VoucherOrder.class);
        voucherOrderService.saveOrder(voucherOrder);
    }
}
