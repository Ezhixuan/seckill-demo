package com.ezhixuan.seckilldemo.service;

import com.ezhixuan.seckilldemo.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author ezhixuan
 * @description 针对表【tb_voucher_order】的数据库操作Service
 * @createDate 2023-09-18 16:42:06
 */
public interface VoucherOrderService extends IService<VoucherOrder> {

  void saveOrder(VoucherOrder order);
}
