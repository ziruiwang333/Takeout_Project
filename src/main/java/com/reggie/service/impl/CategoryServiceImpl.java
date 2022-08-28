package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.entity.Category;
import com.reggie.entity.Dish;
import com.reggie.entity.Setmeal;
import com.reggie.mapper.CategoryMapper;
import com.reggie.service.CategoryService;
import com.reggie.service.DishService;
import com.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据id删除分类, 删除之前需要进行判断
     * sql: SELECT * FROM dish/setmeal WHERE category_id=?
     * sql: SELECT count(*) FROM dish/setmeal WHERE category_id=?
     * @param id
     */
    @Override
    public void remove(Long id) {
        //查询当前分类是否关联了菜品, 若已关联, 则抛出业务异常
        //创建query wrapper
        LambdaQueryWrapper<Dish> dishQueryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件, 根据分类id进行查询
        dishQueryWrapper.eq(Dish::getCategoryId, id);
        int dishCount = dishService.count(dishQueryWrapper);
        if(dishCount > 0){
            //已经关联菜品, 抛出异常
            throw new CustomException("当前分类下关联了菜品, 不能删除");
        }

        //查询当前分类是否关联了套餐, 若以关联, 则抛出业务异常
        LambdaQueryWrapper<Setmeal> setmealQueryWrapper = new LambdaQueryWrapper();
        setmealQueryWrapper.eq(Setmeal::getCategoryId, id);
        int setmealCount = setmealService.count(setmealQueryWrapper);
        if(setmealCount > 0){
            //已经关联套餐, 抛出异常
            throw new CustomException("当前分类下关联了套餐, 不能删除");
        }

        //正常删除分类
        super.removeById(id);
    }
}
