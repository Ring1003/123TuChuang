//package cn.cherry.imgwiki.config;
//
//
//import org.springframework.cache.annotation.CachingConfigurerSupport;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.core.script.DefaultRedisScript;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//import com.fasterxml.jackson.annotation.JsonAutoDetect;
//import com.fasterxml.jackson.annotation.JsonTypeInfo;
//import com.fasterxml.jackson.annotation.PropertyAccessor;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
//
///**
// * redis配置
// *
// */
//@Configuration
//@EnableCaching
//public class RedisConfig extends CachingConfigurerSupport
//{
//    // 配置RedisTemplate
//    @Bean
//    public RedisTemplate<byte[], byte[]> redisTemplate(RedisConnectionFactory connectionFactory) {
//        // 创建RedisTemplate实例
//        RedisTemplate<byte[], byte[]> template = new RedisTemplate<>();
//        // 设置连接工厂
//        template.setConnectionFactory(connectionFactory);
//        // 设置默认的序列化器为GenericJackson2JsonRedisSerializer，用于序列化键和值为JSON格式
//        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
//        // 设置键的序列化器为StringRedisSerializer
//        template.setKeySerializer(new StringRedisSerializer());
//        // 设置值的序列化器为GenericJackson2JsonRedisSerializer
//        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//
//        // 返回配置好的RedisTemplate实例
//        return template;
//    }
//
//    @Bean
//    public DefaultRedisScript<Long> limitScript()
//    {
//        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
//        redisScript.setScriptText(limitScriptText());
//        redisScript.setResultType(Long.class);
//        return redisScript;
//    }
//
//    /**
//     * 限流脚本
//     */
//    private String limitScriptText()
//    {
//        return "local key = KEYS[1]\n" +
//                "local count = tonumber(ARGV[1])\n" +
//                "local time = tonumber(ARGV[2])\n" +
//                "local current = redis.call('get', key);\n" +
//                "if current and tonumber(current) > count then\n" +
//                "    return tonumber(current);\n" +
//                "end\n" +
//                "current = redis.call('incr', key)\n" +
//                "if tonumber(current) == 1 then\n" +
//                "    redis.call('expire', key, time)\n" +
//                "end\n" +
//                "return tonumber(current);";
//    }
//}
