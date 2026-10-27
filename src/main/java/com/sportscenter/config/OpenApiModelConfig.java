package com.sportscenter.config;

import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documents the existing Jackson LocalTime wire representation without changing serialization. */
@Configuration
public class OpenApiModelConfig {
    @Bean
    OpenApiCustomizer localTimeWireSchema() {
        return api -> {
            if (api.getComponents() == null || api.getComponents().getSchemas() == null
                    || !api.getComponents().getSchemas().containsKey("LocalTime")) return;
            api.getComponents().getSchemas().put("LocalTime", new StringSchema()
                    .description("Local wall-clock time in Asia/Ho_Chi_Minh, without an offset; HH:mm or HH:mm:ss[.fraction]")
                    .pattern("^([01]\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d(?:\\.\\d{1,9})?)?$")
                    .example("07:45:00"));
        };
    }
}
