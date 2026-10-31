package co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** IDENT-001 D3: el frontend obtiene el dominio institucional vigente; no existe un dominio escrito en el cliente. */
class ConsultarConfiguracionRegistroEstudianteUseCaseImplTest {

    private static CorreoInstitucionalPolicy politica(final String dominio) {
        final ParameterCatalogPort parametros = new ParameterCatalogPort() {
            @Override
            public Optional<String> getParameter(final String group, final String key) {
                return Optional.of(dominio);
            }

            @Override
            public String getRequiredParameter(final String group, final String key) {
                return dominio;
            }

            @Override
            public <T> T getParameterAs(final String group, final String key, final Class<T> targetType) {
                return targetType.cast(dominio);
            }
        };
        final MessageCatalogPort mensajes = new MessageCatalogPort() {
            @Override
            public String getUserMessage(final String code, final Object... args) {
                return code;
            }

            @Override
            public String getTechnicalMessage(final String code, final Object... args) {
                return code;
            }

            @Override
            public Optional<String> findUserMessage(final String code) {
                return Optional.empty();
            }
        };
        return new CorreoInstitucionalPolicy(parametros, mensajes);
    }

    @Test
    void devuelve_el_dominio_configurado() {
        assertEquals("@uco.edu.co", new ConsultarConfiguracionRegistroEstudianteUseCaseImpl(politica("@uco.edu.co")).execute());
        assertEquals("@otra.edu", new ConsultarConfiguracionRegistroEstudianteUseCaseImpl(politica("@otra.edu")).execute());
    }

    @Test
    void la_politica_es_obligatoria() {
        assertThrows(NullPointerException.class, () -> new ConsultarConfiguracionRegistroEstudianteUseCaseImpl(null));
    }
}
