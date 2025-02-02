package ltd.wrb.payment.config;

import java.io.File;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfig = new RedisStandaloneConfiguration();
        redisStandaloneConfig.setHostName(System.getenv("REDIS_HOST"));
        redisStandaloneConfig.setPort(Integer.parseInt(System.getenv("REDIS_PORT")));
        String password = System.getenv("REDIS_PASSWORD");
        if (password != null) {
            redisStandaloneConfig.setPassword(password);
        }

        String username = System.getenv("REDIS_USERNAME");
        if (username != null) {
            redisStandaloneConfig.setUsername(username);
        }

        log.info("Redis host: " + redisStandaloneConfig.getHostName());
        log.info("Redis port: " + redisStandaloneConfig.getPort());

        boolean useSsl = Boolean.parseBoolean(System.getenv("REDIS_SSL"));
        if (useSsl) {
            String caCrt = System.getenv("REDIS_TRUST_STORE");
            String clientP12 = System.getenv("REDIS_KEY_STORE");

            File clientP12File = new File(clientP12);
            File caCrtFile = new File(caCrt);

            if (!clientP12File.exists()) {
                log.error("Client certificate file not found: " + clientP12);
                throw new RuntimeException("Client certificate file not found: " + clientP12);
            }

            if (!caCrtFile.exists()) {
                log.error("CA certificate file not found: " + caCrt);
                throw new RuntimeException("CA certificate file not found: " + caCrt);
            }

            String keystoreType = System.getenv("REDIS_KEY_STORE_TYPE");
            String keystorePassword = System.getenv("REDIS_KEY_STORE_PASSWORD");

            // 双向证书认证， 服务端 sni: xredis, 客户端 0x00.xnode.xrocket.network

            // SNIHostName sniServerName = new SNIHostName("xredis");


            // SNIHostName sniClientName = new SNIHostName("0x00.xnode.xrocket.network");

            SslOptions sslOptions = SslOptions.builder()
                    .jdkSslProvider()
                    .keyStoreType(keystoreType)
                    .trustManager(caCrtFile)
                    .keystore(clientP12File, keystorePassword.toCharArray())
                    .build();
            ClientOptions sslClientOptions = ClientOptions.builder().sslOptions(sslOptions).build();
            LettuceClientConfiguration lettuceClientConfiguration = LettuceClientConfiguration.builder()
                    .useSsl()
                    .and()
                    .clientOptions(sslClientOptions)
                    .commandTimeout(Duration.ofSeconds(10))
                    .build();
            return new LettuceConnectionFactory(redisStandaloneConfig, lettuceClientConfiguration);
        } else {
            return new LettuceConnectionFactory(redisStandaloneConfig);
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}