package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.dto.OrderDto;
import com.reggie.entity.OrderDetail;
import com.reggie.entity.Orders;
import com.reggie.service.OrderDetailService;
import com.reggie.service.OrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("submit")
    public R<String> submit(@RequestBody Orders orders){
        orderService.submit(orders);
        return R.success("订单下单成功");
    }

    /**
     * 查看历史订单
     */
    @GetMapping("/userPage")
    public R<Page<OrderDto>> page(@RequestParam Integer page, @RequestParam Integer pageSize){
        Page<Orders> pageInfo = new Page<>();
        QueryWrapper<Orders> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", BaseContext.getCurrentId());
        queryWrapper.orderByDesc("order_time");
        orderService.page(pageInfo, queryWrapper);
        Page<OrderDto> orderDtoPage = new Page<>();
        BeanUtils.copyProperties(pageInfo, orderDtoPage, "records");
        List<Orders> records = pageInfo.getRecords();
        List<OrderDto> orderDtoList = new ArrayList<>();
        for(Orders order : records){
            OrderDto orderDto = new OrderDto();
            BeanUtils.copyProperties(order, orderDto);
            QueryWrapper<OrderDetail> orderDetailQueryWrapper = new QueryWrapper<>();
            orderDetailQueryWrapper.eq("order_id", orderDto.getId());
            List<OrderDetail> orderDetailList = orderDetailService.list(orderDetailQueryWrapper);
            orderDto.setOrderDetails(orderDetailList);
            orderDtoList.add(orderDto);
        }
        orderDtoPage.setRecords(orderDtoList);
        return R.success(orderDtoPage);
    }

}
