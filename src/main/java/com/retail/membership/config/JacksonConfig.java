package com.retail.membership.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 설정. 도메인 이벤트의 {@code Instant}(java.time) 직렬화를 지원한다.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            builder.modules(new JavaTimeModule());
            builder.simpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            // 요청 본문의 enum(Channel 등)을 대소문자 구분 없이 매핑 (kakao == KAKAO)
            builder.postConfigurer(mapper -> mapper.configure(
                    com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true));
        };
    }

    /** Kafka Producer/Consumer 에서 직접 사용할 ObjectMapper. */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
