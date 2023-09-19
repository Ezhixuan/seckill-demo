package com.ezhixuan.seckilldemo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ezhixuan.seckilldemo.entity.SeckillVoucher;
import com.ezhixuan.seckilldemo.entity.VoucherOrder;
import com.ezhixuan.seckilldemo.mapper.VoucherOrderMapper;
import com.ezhixuan.seckilldemo.service.SeckillVoucherService;
import com.ezhixuan.seckilldemo.service.VoucherOrderService;
import java.util.Date;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

  /**
   * 保存订单信息
   *
   * @param order
   */
  @Override
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
