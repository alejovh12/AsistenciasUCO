package co.edu.uco.asistenciasuco.application.features.usuario.dominiocorreo;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IDENT-001 BE-P01/BE-P02: la politica aplica la misma regla que {@code ufn_correo_cumple_dominio_institucional}
 * (coincidencia exacta del dominio, sin distinguir mayusculas) y lee el dominio del parametro, sin valor quemado.
 */
class DominioCorreoInstitucionalPolicyTest {

    private static final String DOMINIO = "@uco.edu.co";

    @Test
    void BE_P01_acepta_solo_el_dominio_exacto_sin_distinguir_mayusculas_ni_espacios_laterales() {
        final DominioCorreoInstitucionalPolicy policy = politica(DOMINIO);

        assertTrue(policy.cumple("a@uco.edu.co"));
        assertTrue(policy.cumple("A@UCO.EDU.CO "));
        assertTrue(policy.cumple("  maria.perez@uco.edu.co"));
        assertFalse(policy.cumple("a@a.uco.edu.co"));
        assertFalse(policy.cumple("a@uco.edu.co.evil.com"));
        assertFalse(policy.cumple("@uco.edu.co"));
        assertFalse(policy.cumple("a@@uco.edu.co"));
        assertFalse(policy.cumple("a@uco.edu.co@x.com"));
        assertFalse(policy.cumple("a@gmail.com"));
        assertFalse(policy.cumple("auco.edu.co"));
        assertFalse(policy.cumple(null));
        assertFalse(policy.cumple(""));
    }

    @Test
    void BE_P01_un_dominio_mal_configurado_rechaza_todo_fail_closed() {
        assertFalse(politica("").cumple("a@uco.edu.co"));
        assertFalse(politica("   ").cumple("a@uco.edu.co"));
        assertFalse(politica("uco.edu.co").cumple("a@uco.edu.co"));
        assertFalse(politica("@").cumple("a@uco.edu.co"));
        assertFalse(politica("@uco.edu.co@x").cumple("a@uco.edu.co"));
    }

    @Test
    void BE_P02_el_dominio_se_lee_del_parametro_y_cambiarlo_cambia_el_resultado() {
        final AtomicReference<String> valor = new AtomicReference<>(DOMINIO);
        final DominioCorreoInstitucionalPolicy policy = new DominioCorreoInstitucionalPolicy(
                parametros(valor), mensajes());

        assertTrue(policy.cumple("a@uco.edu.co"));
        assertFalse(policy.cumple("a@otra.edu"));

        valor.set("@otra.edu");

        assertFalse(policy.cumple("a@uco.edu.co"));
        assertTrue(policy.cumple("a@otra.edu"));
        assertEquals("@otra.edu", policy.dominio());
    }

    @Test
    void exigir_con_correo_valido_no_lanza() {
        politica(DOMINIO).exigir("ana.perez@uco.edu.co");
    }

    @Test
    void exigir_con_correo_invalido_lanza_validacion_VCE_001_con_el_mensaje_del_catalogo_y_el_dominio() {
        final AtomicReference<Object[]> argumentos = new AtomicReference<>();
        final MessageCatalogPort mensajes = new MessageCatalogPort() {
            @Override
            public String getUserMessage(final String code, final Object... args) {
                argumentos.set(args);
                return "El correo del estudiante debe ser institucional y terminar en " + args[0] + ".";
            }

            @Override
            public String getTechnicalMessage(final String code, final Object... args) {
                return code;
            }

            @Override
            public java.util.Optional<String> findUserMessage(final String code) {
                return java.util.Optional.empty();
            }
        };
        final DominioCorreoInstitucionalPolicy policy = new DominioCorreoInstitucionalPolicy(
                parametros(new AtomicReference<>(DOMINIO)), mensajes);

        final ValidationException exception = assertThrows(ValidationException.class, () -> policy.exigir("ana@gmail.com"));

        assertEquals("El correo del estudiante debe ser institucional y terminar en @uco.edu.co.", exception.getMessage());
        assertNotNull(argumentos.get());
        assertEquals(DOMINIO, argumentos.get()[0]);
    }

    @Test
    void sin_parametro_configurado_falla_en_lugar_de_aceptar() {
        final ParameterCatalogPort sinParametro = new ParameterCatalogPort() {
            @Override
            public java.util.Optional<String> getParameter(final String group, final String key) {
                return java.util.Optional.empty();
            }

            @Override
            public String getRequiredParameter(final String group, final String key) {
                throw new ParameterNotFoundException(group, key);
            }

            @Override
            public <T> T getParameterAs(final String group, final String key, final Class<T> targetType) {
                throw new ParameterNotFoundException(group, key);
            }
        };
        final DominioCorreoInstitucionalPolicy policy = new DominioCorreoInstitucionalPolicy(sinParametro, mensajes());

        assertThrows(ParameterCatalogPort.ParameterNotFoundException.class, () -> policy.cumple("a@uco.edu.co"));
    }

    @Test
    void los_colaboradores_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new DominioCorreoInstitucionalPolicy(null, mensajes()));
        assertThrows(NullPointerException.class,
                () -> new DominioCorreoInstitucionalPolicy(parametros(new AtomicReference<>(DOMINIO)), null));
    }

    private static DominioCorreoInstitucionalPolicy politica(final String dominio) {
        return new DominioCorreoInstitucionalPolicy(parametros(new AtomicReference<>(dominio)), mensajes());
    }

    private static ParameterCatalogPort parametros(final AtomicReference<String> valor) {
        return new ParameterCatalogPort() {
            @Override
            public java.util.Optional<String> getParameter(final String group, final String key) {
                return java.util.Optional.ofNullable(valor.get());
            }

            @Override
            public String getRequiredParameter(final String group, final String key) {
                if (!"REGISTRO".equals(group) || !"DOMINIO_CORREO_ESTUDIANTE".equals(key)) {
                    throw new ParameterNotFoundException(group, key);
                }
                return Objects.requireNonNull(valor.get());
            }

            @Override
            public <T> T getParameterAs(final String group, final String key, final Class<T> targetType) {
                return targetType.cast(getRequiredParameter(group, key));
            }
        };
    }

    private static MessageCatalogPort mensajes() {
        return new MessageCatalogPort() {
            @Override
            public String getUserMessage(final String code, final Object... args) {
                return code;
            }

            @Override
            public String getTechnicalMessage(final String code, final Object... args) {
                return code;
            }

            @Override
            public java.util.Optional<String> findUserMessage(final String code) {
                return java.util.Optional.empty();
            }
        };
    }
}
