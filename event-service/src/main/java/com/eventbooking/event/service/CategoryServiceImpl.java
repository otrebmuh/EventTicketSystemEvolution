package com.eventbooking.event.service;

import com.eventbooking.event.dto.CategoryDto;
import com.eventbooking.event.entity.EventCategory;
import com.eventbooking.event.mapper.EventMapper;
import com.eventbooking.event.repository.EventCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    
    private final EventCategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    
    @Autowired
    public CategoryServiceImpl(EventCategoryRepository categoryRepository, EventMapper eventMapper) {
        this.categoryRepository = categoryRepository;
        this.eventMapper = eventMapper;
    }
    
    @Override
    public List<CategoryDto> getActiveCategories() {
        List<EventCategory> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return categories.stream()
            .map(eventMapper::toCategoryDto)
            .collect(Collectors.toList());
    }
}