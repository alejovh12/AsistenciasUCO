package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultipartConfigurationTest {

    @Test
    void multipart_threshold_conserva_archivos_validos_de_hasta_5_mib_en_memoria() throws IOException {
        final List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application.yml", new FileSystemResource("src/main/resources/application.yml"));

        final Object threshold = sources.stream()
                .map(source -> source.getProperty("spring.servlet.multipart.file-size-threshold"))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);

        assertEquals("5MB", threshold);
    }
}


