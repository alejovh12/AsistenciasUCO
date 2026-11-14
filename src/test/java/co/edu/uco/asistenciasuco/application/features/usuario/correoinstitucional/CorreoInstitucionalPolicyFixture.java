package co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;

import java.util.Optional;

/**
 * Política de correo institucional para pruebas (DEC-073): dominio {@code @uco.edu.co} y catálogo de mensajes que
 * devuelve el código, como si el catálogo aún no tuviera el texto (se usa el respaldo del enum).
 */
public final class CorreoInstitucionalPolicyFixture {

    public static final String DOMINIO = "@uco.edu.co";

    private CorreoInstitucionalPolicyFixture() {
    }

    public static CorreoInstitucionalPolicy institucional() {
        return new CorreoInstitucionalPolicy(new ParameterCatalogPort() {
            @Override
            public Optional<String> getParameter(final String group, final String key) {
                return Optional.of(DOMINIO);
            }

            @Override
            public String getRequiredParameter(final String group, final String key) {
                return DOMINIO;
            }

            @Override
            public <T> T getParameterAs(final String group, final String key, final Class<T> targetType) {
                return targetType.cast(DOMINIO);
            }
        }, new MessageCatalogPort() {
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
        });
    }
}
