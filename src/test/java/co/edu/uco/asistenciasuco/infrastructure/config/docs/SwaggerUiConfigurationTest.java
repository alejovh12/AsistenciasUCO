package co.edu.uco.asistenciasuco.infrastructure.config.docs;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SwaggerUiConfigurationTest {

    @Test
    void registra_el_mime_type_yaml_para_servir_el_contrato_openapi() {
        final ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);

        new SwaggerUiConfiguration().openApiYamlMimeMapping().customize(factory);

        final ArgumentCaptor<MimeMappings> mappings = ArgumentCaptor.forClass(MimeMappings.class);
        verify(factory).addMimeMappings(mappings.capture());
        assertEquals("application/yaml", mappings.getValue().get("yaml"));
    }
}
