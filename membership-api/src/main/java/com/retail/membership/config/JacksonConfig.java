package com.retail.membership.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p><b>용도:</b> HTTP JSON 직렬화용 Jackson 커스터마이저.</p>
 *
 * <p>Kafka 등에서 쓰는 {@code ObjectMapper} 빈은 {@code membership-common} 의
 * {@link ObjectMapperConfig} 가 제공한다.
 */
@Configuration
public class JacksonConfig {

    /** HTTP JSON 직렬화에 JavaTime 모듈과 enum 대소문자 무시 설정을 적용한다. */
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
}
