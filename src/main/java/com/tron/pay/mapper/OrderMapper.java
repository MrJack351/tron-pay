package com.tron.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tron.pay.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    @Select("SELECT COUNT(*) FROM tb_order WHERE merchant_id = #{merchantId} AND create_time_stamp >= #{startTime} AND create_time_stamp <= #{endTime}")
    int getDailyOrderCount(@Param("merchantId") String merchantId, @Param("startTime") Long startTime, @Param("endTime") Long endTime);

    @Select("SELECT COUNT(*) FROM tb_order WHERE merchant_id = #{merchantId}")
    int getTotalOrderCount(@Param("merchantId") String merchantId);

    List<Map<String, Object>>  selectWeekDataAmount(@Param("merchantId") String merchantId);

    List<Map<String, Object>>  selectWeekDataCount(@Param("merchantId") String merchantId);
}
