package co.edu.uco.asistenciasuco.infrastructure.config.jackson;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

@Configuration
public class JacksonInputConfig {

    @Bean
    JsonMapperBuilderCustomizer strictJsonInputCoercionCustomizer() {
        return builder -> {
            builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            builder.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            builder.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION);
            builder.withCoercionConfig(LogicalType.Integer, coercionConfig -> {
                coercionConfig.setCoercion(CoercionInputShape.String, CoercionAction.Fail);
                coercionConfig.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
                coercionConfig.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
            });
            builder.withCoercionConfig(LogicalType.Boolean, coercionConfig -> {
                coercionConfig.setCoercion(CoercionInputShape.String, CoercionAction.Fail);
                coercionConfig.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
                coercionConfig.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
            });
            builder.withCoercionConfig(LogicalType.Textual, coercionConfig -> {
                coercionConfig.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
                coercionConfig.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
                coercionConfig.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
            });
        };
    }
}
