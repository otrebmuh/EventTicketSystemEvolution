package com.eventbooking.event.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class SearchCriteria {
    
    private String query;
    private String city;
    private UUID categoryId;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    
    // Default constructor
    public SearchCriteria() {}
    
    // Getters and setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public UUID getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }
    
    public LocalDateTime getDateFrom() {
        return dateFrom;
    }
    
    public void setDateFrom(LocalDateTime dateFrom) {
        this.dateFrom = dateFrom;
    }
    
    public LocalDateTime getDateTo() {
        return dateTo;
    }
    
    public void setDateTo(LocalDateTime dateTo) {
        this.dateTo = dateTo;
    }
    
    public BigDecimal getMinPrice() {
        return minPrice;
    }
    
    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }
    
    public BigDecimal getMaxPrice() {
        return maxPrice;
    }
    
    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }
    
    @Override
    public String toString() {
        return "SearchCriteria{" +
                "query='" + query + '\'' +
                ", city='" + city + '\'' +
                ", categoryId=" + categoryId +
                ", dateFrom=" + dateFrom +
                ", dateTo=" + dateTo +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                '}';
    }
}