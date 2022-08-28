package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.Category;
import com.reggie.entity.Employee;
import org.springframework.stereotype.Service;

public interface CategoryService extends IService<Category> {
    public abstract void remove(Long id);
}
