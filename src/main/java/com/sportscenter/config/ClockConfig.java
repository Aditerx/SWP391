package com.sportscenter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {
    @Bean
    public Clock applicationClock() {
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
