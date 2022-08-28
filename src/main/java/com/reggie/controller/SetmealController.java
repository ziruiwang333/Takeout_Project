package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.dto.SetmealDto;
import com.reggie.entity.Dish;
import com.reggie.entity.Setmeal;
import com.reggie.entity.SetmealDish;
import com.reggie.service.CategoryService;
import com.reggie.service.SetmealDishService;
import com.reggie.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增套餐
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto){
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功");
    }

    /**
     * 套餐分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page<SetmealDto>> page(int page, int pageSize, String name){
        //分页构造器对象
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        Page<SetmealDto> dtoPage = new Page<>();
        QueryWrapper<Setmeal> queryWrapper = new QueryWrapper<>();
        //添加查询条件, 根据name进行like模糊查询
        queryWrapper.like(name != null, "name", name);
        //添加排序条件, 根据更新时间降序排序
        queryWrapper.orderByDesc("update_time");
        //调用service进行查询
        setmealService.page(pageInfo, queryWrapper);
        //进行对象的拷贝
        BeanUtils.copyProperties(pageInfo, dtoPage, "records");
        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDto> dtoList = new ArrayList<>();
        for(Setmeal setmeal : records){
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(setmeal, setmealDto);
            String categoryName = categoryService.getById(setmeal.getCategoryId()).getName();
            if(categoryName != null){
                setmealDto.setCategoryName(categoryName);
                dtoList.add(setmealDto);
            }
        }
        dtoPage.setRecords(dtoList);
        return R.success(dtoPage);
    }

    /**
     * 删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        setmealService.removeWithDish(ids);
        return R.success("套餐删除成功");
    }

    /**
     * 修改套餐状态
     * @return
     */
    @PostMapping("/status/{statusId}")
    public R<String> status(@PathVariable Integer statusId, @RequestParam List<Long> ids){
        List<Setmeal> setmeals = new ArrayList<>();
        for(Long id : ids){
            Setmeal setmeal = new Setmeal();
            setmeal.setStatus(statusId);
            setmeal.setId(id);
            setmeals.add(setmeal);
        }
        setmealService.updateBatchById(setmeals);
        return R.success("套餐状态修改成功");
    }

    /**
     * 根据id查询套餐信息, 回显
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> getSetmealById(@PathVariable Long id){
        SetmealDto setmealDto = setmealService.getWithDish(id);
        return R.success(setmealDto);
    }

    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal){
        QueryWrapper<Setmeal> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId()!=null, "category_id", setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus()!=null, "status", setmeal.getStatus());
        queryWrapper.orderByDesc("update_time");
        List<Setmeal> setmealList = setmealService.list(queryWrapper);
        return R.success(setmealList);
    }

    @GetMapping("/dish/{id}")
    public R<List<SetmealDish>> getSetmealDishes(Setmeal setmeal){
        QueryWrapper<SetmealDish> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("setmeal_id", setmeal.getId());
        List<SetmealDish> setmealDishList = setmealDishService.list(queryWrapper);
        return R.success(setmealDishList);
    }

}
