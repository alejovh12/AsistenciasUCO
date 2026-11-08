package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolicitarRevisionAsistenciaDomainTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USUARIO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SOPORTE = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SOPORTE_2 = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void construye_domain_valido_y_normaliza_justificacion() {
        final SolicitarRevisionAsistenciaDomain domain =
                new SolicitarRevisionAsistenciaDomain(
                        SESION,
                        "  ERROR_REGISTRO  ",
                        "  Llegue despues del registro  ",
                        List.of(SOPORTE),
                        USUARIO
                );

        assertEquals(SESION, domain.getSesion());
        assertEquals("ERROR_REGISTRO", domain.getCategoria());
        assertEquals("Llegue despues del registro", domain.getJustificacion());
        assertEquals(USUARIO, domain.getUsuario());
        assertEquals(List.of(SOPORTE), domain.getSoportes());
    }

    @Test
    void el_soporte_es_opcional() {
        final SolicitarRevisionAsistenciaDomain domain =
                new SolicitarRevisionAsistenciaDomain(SESION, "CAT", "motivo valido", null, USUARIO);

        assertEquals(List.of(), domain.getSoportes());
        assertEquals(List.of(), new SolicitarRevisionAsistenciaDomain(SESION, "CAT", "motivo valido", List.of(), USUARIO).getSoportes());
    }

    // CULM-001 D.2 (CONTRACT D 3.3): de 0 a N soportes; la lista del dominio es una copia inmutable en el orden recibido.
    @Test
    void conserva_varios_soportes_en_orden_como_copia_inmutable() {
        final List<UUID> origen = new ArrayList<>(List.of(SOPORTE, SOPORTE_2));
        final SolicitarRevisionAsistenciaDomain domain =
                new SolicitarRevisionAsistenciaDomain(SESION, "CAT", "motivo valido", origen, USUARIO);
        origen.add(UUID.randomUUID());

        assertEquals(List.of(SOPORTE, SOPORTE_2), domain.getSoportes());
        assertThrows(UnsupportedOperationException.class, () -> domain.getSoportes().add(UUID.randomUUID()));
    }

    @Test
    void rechaza_un_soporte_nulo_dentro_de_la_lista() {
        final List<UUID> conNulo = Arrays.asList(SOPORTE, null);
        assertThrows(ValidationException.class,
                () -> new SolicitarRevisionAsistenciaDomain(SESION, "CAT", "motivo valido", conNulo, USUARIO));
    }

    @Test
    void rechaza_sesion_usuario_y_textos_invalidos() {
        assertThrows(ValidationException.class, () -> new SolicitarRevisionAsistenciaDomain(null, "CAT", "motivo valido", null, USUARIO));
        assertThrows(ValidationException.class, () -> new SolicitarRevisionAsistenciaDomain(SESION, "CAT", "motivo valido", null, null));
        assertThrows(ValidationException.class, () -> new SolicitarRevisionAsistenciaDomain(SESION, null, "motivo valido", null, USUARIO));
        assertThrows(ValidationException.class, () -> new SolicitarRevisionAsistenciaDomain(SESION, "CAT", null, null, USUARIO));
        assertThrows(ValidationException.class, () -> new SolicitarRevisionAsistenciaDomain(SESION, "CAT", "a".repeat(301), null, USUARIO));
    }
}
