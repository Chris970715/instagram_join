package instagram_join.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession // Redis 세션 사용 활성화
public class RedisConfig {

    // 세션 데이터를 직렬화하는 RedisSerializer ->  Bean 등록
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer(objectMapper());
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    @Bean
    // RedisTemplate 객체를 생성하고 Redis 와의 연결을 설정한 후에 직렬화 방식을 지정
    // RedisConnectionFactory connectionFactory - Redis 서버와의 연결을 설정하는 역할
    // RedisTemplate<String, Object> - Redis 와 상호작용하는데 사용되는 객체
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // RedisTemplate 객체를 생성한다
        // <String, Object> - 키는 문자열, 값은 객체로 지정
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // RedisTemplate & Redis 서버간의 연결을 설정
        template.setConnectionFactory(connectionFactory);

        // Redis 에 저장할 때 키를 직렬화하는 방식을 지정
        // StringRedisSerializer -> Redis 키를 문자열로 직렬화
        template.setKeySerializer(new StringRedisSerializer());

        // Redis 에 저장할 때 값을 직렬화하는 방식을 지정
        // GenericJackson2JsonRedisSerializer -> Jackson 라이브러리를 사용하여 JSON 형식으로 객체를 직렬화
        // objectMapper()를 사용하여 ObjectMapper 객체를 제공함으로써
        // 객체를 JSON 으로 변환하고, 저장된 JSON 데이터를 다시 객체로 역직렬화할 수 있다
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper()));

        // Redis 에서 Hash 구조의 키를 직렬화하는 방식을 지정
        // Hash -> 여러 개의 필드와 값을 저장하는 데이터 구조
        // Hash 의 각 필드 이름을 문자열로 직렬화한다
        template.setHashKeySerializer(new StringRedisSerializer());

        // Redis 에서 Hash 구조의 값을 직렬화하는 방식을 지정
        // Hash 구조의 값도 객체로 저장되므로, JSON 으로 직렬화하기 위해
        // GenericJackson2JsonRedisSerializer 사용
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper()));

        // Redis 에서 트랜잭션을 활성화하는 설정
        // 트랜잭션은 일련의 Redis 명령을 묶어서 실행하고, 모든 명령이 성공했을 때만 그 결과를
        // 적용하는 방식
        // true 로 설정하면 트랜잭션을 사용할 수 있게 되어 여러 Redis 명령을 하나의 원자적인 작업으로 처리 가능
        template.setEnableTransactionSupport(true);

        // RedisTemplate 의 모든 속성이 설정된 후에 초기화를 완료하는 역할
        template.afterPropertiesSet();
        return template;
    }
}
