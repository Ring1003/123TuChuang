package cn.cherry.imgwiki.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class LocalCacheConfig {

    @Value("${localCache.path}")
    private String path;

    @Value("${localCache.size}")
    private String size;

}
