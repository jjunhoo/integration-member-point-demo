package com.retail.membership.config;

import com.retail.membership.auth.social.SocialProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p><b>용도:</b> URL path 의 SocialProvider 를 대소문자 무시로 변환하는 MVC 설정.</p>
 *
 * 경로 변수의 소셜 provider 를 대소문자 구분 없이 변환한다 (예: /social/kakao → KAKAO).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, SocialProvider>() {
            @Override
            public SocialProvider convert(String source) {
                return SocialProvider.valueOf(source.trim().toUpperCase());
            }
        });
    }
}
