package com.eventbooking.event.service;

import com.eventbooking.event.dto.CategoryDto;

import java.util.List;

public interface CategoryService {
    
    List<CategoryDto> getActiveCategories();
}