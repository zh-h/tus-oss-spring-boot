package me.zonghua.tus.config;


import me.zonghua.tus.model.TusFileUpload;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, TusFileUpload> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, TusFileUpload> template = new RedisTemplate<String, TusFileUpload>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<TusFileUpload>(TusFileUpload.class));
        return template;
    }


}