package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.entity.ShoppingCart;
import com.reggie.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    @GetMapping("/list")
    public R<List<ShoppingCart>> list(){
        Long currentId = BaseContext.getCurrentId();
        QueryWrapper<ShoppingCart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(currentId!=null, "user_id", currentId);
        queryWrapper.orderByDesc("create_time");
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(queryWrapper);
        return R.success(shoppingCartList);
    }

    /**
     * 添加购物车
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        //设置用户id
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);
        //查询当前菜品或套餐是否在购物车中
        QueryWrapper<ShoppingCart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", currentId);
        Long dishId = shoppingCart.getDishId();
        if(dishId != null){
            //添加到购物车的是菜品
            queryWrapper.eq("dish_id", shoppingCart.getDishId());
        } else{
            //添加到购物车的是套餐
            queryWrapper.eq("setmeal_id", shoppingCart.getSetmealId());
        }
        ShoppingCart savedShoppingCart = shoppingCartService.getOne(queryWrapper);
        if(savedShoppingCart != null) {
            //若以存在, 则在原数量基础上+1
            savedShoppingCart.setNumber(savedShoppingCart.getNumber()+1);
            shoppingCartService.updateById(savedShoppingCart);
        } else {
            //若不存在, 则添加到购物车中, 数量默认为1
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            savedShoppingCart = shoppingCart;
        }
        return R.success(savedShoppingCart);
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    public R<String> clean(){
        Long currentId = BaseContext.getCurrentId();
        QueryWrapper<ShoppingCart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", currentId);
        shoppingCartService.remove(queryWrapper);
        return R.success("清空购物车成功");
    }

    @PostMapping("/sub")
    public R<ShoppingCart> subtraction(@RequestBody ShoppingCart shoppingCart){
        Long currentId = BaseContext.getCurrentId();
        QueryWrapper<ShoppingCart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", currentId);
        if(shoppingCart.getDishId() != null){
            queryWrapper.eq("dish_id", shoppingCart.getDishId());
        } else {
            queryWrapper.eq("setmeal_id", shoppingCart.getSetmealId());
        }
        ShoppingCart savedShoppingCart = shoppingCartService.getOne(queryWrapper);
        if(savedShoppingCart != null){
            savedShoppingCart.setNumber(savedShoppingCart.getNumber()-1);
            if(savedShoppingCart.getNumber() == 0){
                shoppingCartService.removeById(savedShoppingCart);
            } else {
                shoppingCartService.updateById(savedShoppingCart);
            }
        }
        return R.success(savedShoppingCart);
    }

}
