package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.dto.SetmealDto;
import com.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    /**
     * 新增套餐, 同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     */
    public abstract void saveWithDish(SetmealDto setmealDto);

    /**
     * 删除套餐, 同时删除套餐和菜品的关联数据
     * @param ids
     */
    public abstract void removeWithDish(List<Long> ids);

    /**
     * 根据id获取套餐信息和菜品
     * @param id
     * @return
     */
    public abstract SetmealDto getWithDish(Long id);
}
