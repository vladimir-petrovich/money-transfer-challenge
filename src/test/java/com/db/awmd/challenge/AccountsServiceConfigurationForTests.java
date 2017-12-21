package com.db.awmd.challenge;

import com.db.awmd.challenge.service.NotificationService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;

@Profile("integration_tests")
@Configuration
public class AccountsServiceConfigurationForTests {
    @Bean
    public NotificationService notificationService() {
        return Mockito.mock(NotificationService.class);
    }

}
