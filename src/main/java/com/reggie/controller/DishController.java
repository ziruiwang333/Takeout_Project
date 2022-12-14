package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.dto.DishDto;
import com.reggie.entity.Category;
import com.reggie.entity.Dish;
import com.reggie.entity.DishFlavor;
import com.reggie.service.CategoryService;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        dishService.saveWithFlavor(dishDto);
        return R.success("新增菜品成功");
    }

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        //构造分页构造器对象
        Page<Dish> pageInfo = new Page<Dish>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>(page, pageSize);

        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name!=null, Dish::getName, name);
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        //执行分页查询
        dishService.page(pageInfo, queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            //分类id
            Long categoryId = item.getCategoryId();
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());
        dishDtoPage.setRecords(list);
        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 更新菜品信息
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        dishService.updateWithFlavor(dishDto);
        //更新时删除所有该菜品所属种类的缓存
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);
        //更新时删除所更新该菜品所属种类的缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return R.success("更新成功");
    }

    /**
     * 根据id(批量)更新菜品状态
     * @param status_id
     * @param ids
     * @return
     */
    @PostMapping("/status/{status_id}")
    public R<String> updateStatus(@PathVariable Integer status_id, @RequestParam(value = "id") Long[] ids){
        for(Long id : ids){
            Dish dish = new Dish();
            dish.setStatus(status_id);
            dish.setId(id);
            dishService.updateById(dish);
        }
        return R.success("状态更新成功");
    }

    /**
     * 根据id(批量)删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> batchDelete(@RequestParam(value = "id") Long[] ids){
        for(Long id : ids){
            Dish dish = new Dish();
            dish.setId(id);
            dishService.removeById(dish);
            LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DishFlavor::getDishId, dish.getId());
            dishFlavorService.remove(queryWrapper);
        }
        return R.success("删除成功");
    }

    /**
     * 根据种类id(category_id)查询对应菜品数据
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtoList = null;
        //动态构造key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        //先从Redis中获取缓存数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        //若存在直接返回无需查询数据库
        if(dishDtoList != null){
            return R.success(dishDtoList);
        }
        //不存在则查询数据库
        //构造查询条件
        QueryWrapper<Dish> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null,  "category_id", dish.getCategoryId());
        //只查询状态为1(起售)的菜品
        queryWrapper.eq("status", 1);
        //添加排序条件
        queryWrapper.orderByAsc("sort").orderByDesc("update_time");
        List<Dish> dishes = dishService.list(queryWrapper);
        //将属性赋值到Dto中
        dishDtoList = new ArrayList<>();
        for(Dish currDish : dishes){
            DishDto dishDto = new DishDto();
            QueryWrapper<DishFlavor> dishFlavorQueryWrapper = new QueryWrapper<>();
            dishFlavorQueryWrapper.eq("dish_id", currDish.getId());
            List<DishFlavor> dishFlavorList = dishFlavorService.list(dishFlavorQueryWrapper);
            BeanUtils.copyProperties(currDish, dishDto);
            dishDto.setFlavors(dishFlavorList);
            dishDtoList.add(dishDto);
        }
        //将查询到的菜品数据缓存到Redis
        redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);
        return R.success(dishDtoList);
    }
//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish){
//        //构造查询条件
//        QueryWrapper<Dish> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq(dish.getCategoryId() != null,  "category_id", dish.getCategoryId());
//        //只查询状态为1(起售)的菜品
//        queryWrapper.eq("status", 1);
//        //添加排序条件
//        queryWrapper.orderByAsc("sort").orderByDesc("update_time");
//        List<Dish> list = dishService.list(queryWrapper);
//        return R.success(list);
//    }

}
