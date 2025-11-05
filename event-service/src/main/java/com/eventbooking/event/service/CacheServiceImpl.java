package com.eventbooking.event.service;

import com.eventbooking.event.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CacheServiceImpl implements CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String EVENT_CACHE_KEY = "event:";
    private static final String SEARCH_CACHE_KEY = "search:";
    private static final Duration EVENT_CACHE_TTL = Duration.ofHours(1);
    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(15);
    
    @Autowired
    public CacheServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public void cacheEvent(Event event) {
        String key = EVENT_CACHE_KEY + event.getId();
        redisTemplate.opsForValue().set(key, event, EVENT_CACHE_TTL);
    }
    
    @Override
    public Optional<Event> getCachedEvent(UUID eventId) {
        String key = EVENT_CACHE_KEY + eventId;
        Event event = (Event) redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(event);
    }
    
    @Override
    public void evictEvent(UUID eventId) {
        String key = EVENT_CACHE_KEY + eventId;
        redisTemplate.delete(key);
    }
    
    @Override
    public void cacheSearchResults(String cacheKey, Page<?> results) {
        redisTemplate.opsForValue().set(cacheKey, results, SEARCH_CACHE_TTL);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Page<T> getCachedSearchResults(String cacheKey, Class<T> type) {
        return (Page<T>) redisTemplate.opsForValue().get(cacheKey);
    }
    
    @Override
    public void evictSearchCaches(UUID categoryId, String city) {
        // Find and delete search cache keys that might be affected
        Set<String> keys = redisTemplate.keys(SEARCH_CACHE_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    
    @Override
    public String generateSearchCacheKey(Object criteria) {
        return SEARCH_CACHE_KEY + DigestUtils.md5DigestAsHex(criteria.toString().getBytes());
    }
}