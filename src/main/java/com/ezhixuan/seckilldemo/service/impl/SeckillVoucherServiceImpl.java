package com.ezhixuan.seckilldemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ezhixuan.seckilldemo.entity.SeckillVoucher;
import com.ezhixuan.seckilldemo.service.SeckillVoucherService;
import com.ezhixuan.seckilldemo.mapper.SeckillVoucherMapper;
import org.springframework.stereotype.Service;

/**
* @author ezhixuan
* @description 针对表【tb_seckill_voucher(秒杀优惠券表，与优惠券是一对一关系)】的数据库操作Service实现
* @createDate 2023-09-18 16:41:14
*/
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher>
    implements SeckillVoucherService{

}




