package com.ezhixuan.seckilldemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ezhixuan.seckilldemo.entity.Voucher;
import com.ezhixuan.seckilldemo.service.VoucherService;
import com.ezhixuan.seckilldemo.mapper.VoucherMapper;
import org.springframework.stereotype.Service;

/**
* @author ezhixuan
* @description 针对表【tb_voucher】的数据库操作Service实现
* @createDate 2023-09-18 16:41:07
*/
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher>
    implements VoucherService{

}




