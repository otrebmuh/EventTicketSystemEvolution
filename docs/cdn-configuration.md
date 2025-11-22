# CDN Configuration Guide

## Overview

This document describes the CloudFront CDN configuration for the Event Ticket Booking System to optimize static asset delivery and improve performance.

## CloudFront Distribution Configuration

### Distribution Settings

```yaml
Distribution:
  Enabled: true
  PriceClass: PriceClass_100  # Use only North America and Europe edge locations
  HttpVersion: http2and3      # Enable HTTP/2 and HTTP/3 for better performance
  
  Origins:
    - Id: S3-event-images
      DomainName: event-images-bucket.s3.amazonaws.com
      S3OriginConfig:
        OriginAccessIdentity: origin-access-identity/cloudfront/ABCDEFG1234567
      
    - Id: Frontend-ALB
      DomainName: frontend-alb.us-east-1.elb.amazonaws.com
      CustomOriginConfig:
        HTTPPort: 80
        HTTPSPort: 443
        OriginProtocolPolicy: https-only
        OriginSSLProtocols:
          - TLSv1.2
          - TLSv1.3
  
  DefaultCacheBehavior:
    TargetOriginId: Frontend-ALB
    ViewerProtocolPolicy: redirect-to-https
    AllowedMethods:
      - GET
      - HEAD
      - OPTIONS
      - PUT
      - POST
      - PATCH
      - DELETE
    CachedMethods:
      - GET
      - HEAD
      - OPTIONS
    Compress: true
    MinTTL: 0
    DefaultTTL: 86400      # 24 hours
    MaxTTL: 31536000       # 1 year
    
    ForwardedValues:
      QueryString: true
      Cookies:
        Forward: whitelist
        WhitelistedNames:
          - session_token
          - auth_token
      Headers:
        - Authorization
        - Origin
        - Access-Control-Request-Method
        - Access-Control-Request-Headers
    
    # Lambda@Edge for security headers
    LambdaFunctionAssociations:
      - EventType: viewer-response
        LambdaFunctionARN: arn:aws:lambda:us-east-1:123456789012:function:add-security-headers:1
  
  CacheBehaviors:
    # Static assets (images, CSS, JS)
    - PathPattern: /static/*
      TargetOriginId: Frontend-ALB
      ViewerProtocolPolicy: redirect-to-https
      Compress: true
      MinTTL: 86400          # 1 day
      DefaultTTL: 604800     # 7 days
      MaxTTL: 31536000       # 1 year
      ForwardedValues:
        QueryString: false
        Cookies:
          Forward: none
    
    # Event images from S3
    - PathPattern: /images/*
      TargetOriginId: S3-event-images
      ViewerProtocolPolicy: redirect-to-https
      Compress: true
      MinTTL: 3600           # 1 hour
      DefaultTTL: 86400      # 1 day
      MaxTTL: 604800         # 7 days
      ForwardedValues:
        QueryString: false
        Cookies:
          Forward: none
    
    # API requests (no caching)
    - PathPattern: /api/*
      TargetOriginId: Frontend-ALB
      ViewerProtocolPolicy: redirect-to-https
      Compress: true
      MinTTL: 0
      DefaultTTL: 0
      MaxTTL: 0
      ForwardedValues:
        QueryString: true
        Cookies:
          Forward: all
        Headers:
          - '*'
  
  CustomErrorResponses:
    - ErrorCode: 404
      ResponsePagePath: /404.html
      ResponseCode: 404
      ErrorCachingMinTTL: 300
    
    - ErrorCode: 500
      ResponsePagePath: /error.html
      ResponseCode: 500
      ErrorCachingMinTTL: 0
    
    - ErrorCode: 503
      ResponsePagePath: /maintenance.html
      ResponseCode: 503
      ErrorCachingMinTTL: 0
  
  Logging:
    Enabled: true
    IncludeCookies: false
    Bucket: cloudfront-logs.s3.amazonaws.com
    Prefix: event-booking/
  
  WebACLId: arn:aws:wafv2:us-east-1:123456789012:global/webacl/event-booking-waf/a1b2c3d4
```

## Cache Invalidation Strategy

### Automatic Invalidation

Invalidate cache when:
- Event images are updated
- Event details are modified
- Static assets are deployed

### Invalidation Patterns

```bash
# Invalidate all event images
aws cloudfront create-invalidation \
  --distribution-id E1234ABCD5678 \
  --paths "/images/*"

# Invalidate specific event image
aws cloudfront create-invalidation \
  --distribution-id E1234ABCD5678 \
  --paths "/images/event-{event-id}/*"

# Invalidate static assets after deployment
aws cloudfront create-invalidation \
  --distribution-id E1234ABCD5678 \
  --paths "/static/*" "/index.html"
```

## Lambda@Edge Functions

### Security Headers Function

```javascript
// add-security-headers.js
exports.handler = async (event) => {
    const response = event.Records[0].cf.response;
    const headers = response.headers;
    
    // Add security headers
    headers['strict-transport-security'] = [{
        key: 'Strict-Transport-Security',
        value: 'max-age=31536000; includeSubDomains; preload'
    }];
    
    headers['x-content-type-options'] = [{
        key: 'X-Content-Type-Options',
        value: 'nosniff'
    }];
    
    headers['x-frame-options'] = [{
        key: 'X-Frame-Options',
        value: 'DENY'
    }];
    
    headers['x-xss-protection'] = [{
        key: 'X-XSS-Protection',
        value: '1; mode=block'
    }];
    
    headers['referrer-policy'] = [{
        key: 'Referrer-Policy',
        value: 'strict-origin-when-cross-origin'
    }];
    
    headers['content-security-policy'] = [{
        key: 'Content-Security-Policy',
        value: "default-src 'self'; script-src 'self' 'unsafe-inline' https://js.stripe.com; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' https://api.stripe.com;"
    }];
    
    return response;
};
```

## Performance Optimization

### Compression

- **Gzip/Brotli**: Enabled for all text-based content
- **Image Optimization**: Use WebP format with fallback to JPEG/PNG
- **Minimum Size**: 1KB (files smaller than this won't be compressed)

### Cache Control Headers

Set appropriate Cache-Control headers in the application:

```java
// For static assets
response.setHeader("Cache-Control", "public, max-age=604800, immutable");

// For event images
response.setHeader("Cache-Control", "public, max-age=86400");

// For API responses (no cache)
response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
response.setHeader("Pragma", "no-cache");
response.setHeader("Expires", "0");
```

## Monitoring and Metrics

### CloudWatch Metrics

Monitor these CloudFront metrics:
- **Requests**: Total number of requests
- **BytesDownloaded**: Total bytes served
- **BytesUploaded**: Total bytes uploaded
- **4xxErrorRate**: Client error rate
- **5xxErrorRate**: Server error rate
- **CacheHitRate**: Percentage of requests served from cache

### Alarms

Set up CloudWatch alarms for:
- Cache hit rate < 80%
- 5xx error rate > 1%
- Origin latency > 1 second

## Cost Optimization

### Strategies

1. **Use appropriate price class**: PriceClass_100 for North America and Europe only
2. **Optimize cache hit rate**: Aim for >80% cache hit rate
3. **Use S3 Transfer Acceleration**: For faster uploads from distant locations
4. **Enable compression**: Reduce data transfer costs
5. **Set appropriate TTLs**: Balance freshness with cache efficiency

### Estimated Costs

Based on 1 million requests/month:
- **Data Transfer**: ~$85/month (1TB)
- **HTTP Requests**: ~$1/month
- **HTTPS Requests**: ~$1.20/month
- **Total**: ~$87/month

## Implementation Steps

1. **Create S3 bucket** for event images with proper CORS configuration
2. **Create CloudFront distribution** with the configuration above
3. **Configure Origin Access Identity** for S3 bucket access
4. **Deploy Lambda@Edge functions** for security headers
5. **Update application configuration** to use CloudFront URLs
6. **Set up monitoring** and alarms
7. **Test cache behavior** and invalidation
8. **Configure DNS** to point to CloudFront distribution

## Testing

### Cache Testing

```bash
# Test cache hit
curl -I https://d1234abcd5678.cloudfront.net/images/event-123.jpg

# Check X-Cache header
# X-Cache: Hit from cloudfront (cache hit)
# X-Cache: Miss from cloudfront (cache miss)
```

### Performance Testing

Use tools like:
- **WebPageTest**: Measure load times from different locations
- **Lighthouse**: Check performance scores
- **CloudWatch**: Monitor cache hit rates and latency

## Troubleshooting

### Low Cache Hit Rate

- Check Cache-Control headers from origin
- Verify query strings and cookies are handled correctly
- Review cache behaviors and path patterns

### High Latency

- Check origin server performance
- Verify edge location coverage
- Consider using Lambda@Edge for dynamic content

### Invalidation Issues

- Wait 10-15 minutes for invalidation to complete
- Use specific paths instead of wildcards when possible
- Consider versioning static assets instead of invalidating
