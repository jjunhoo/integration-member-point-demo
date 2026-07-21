package com.retail.membership.auth.social;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * provider → {@link SocialLoginClient} 조회 레지스트리.
 *
 * <p>스프링이 주입한 모든 {@link SocialLoginClient} 빈을 provider 기준으로 색인한다.
 * 신규 provider 구현체를 빈으로 추가하면 자동으로 등록된다(수정 불필요).
 */
@Component
public class SocialLoginClientRegistry {

    private final Map<SocialProvider, SocialLoginClient> clients;

    public SocialLoginClientRegistry(List<SocialLoginClient> clientList) {
        this.clients = clientList.stream()
                .collect(Collectors.toMap(SocialLoginClient::provider, Function.identity()));
    }

    public SocialLoginClient get(SocialProvider provider) {
        SocialLoginClient client = clients.get(provider);
        if (client == null) {
            throw new SocialAuthException("지원하지 않는 소셜 provider: " + provider);
        }
        return client;
    }
}
