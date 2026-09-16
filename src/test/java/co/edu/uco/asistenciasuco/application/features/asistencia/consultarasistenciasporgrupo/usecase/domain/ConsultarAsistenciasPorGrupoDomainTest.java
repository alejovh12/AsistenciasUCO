package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultarAsistenciasPorGrupoDomainTest {

    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USUARIO_EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void construye_domain_valido() {
        final ConsultarAsistenciasPorGrupoDomain domain =
                new ConsultarAsistenciasPorGrupoDomain(GRUPO, SESION, USUARIO_EJECUTOR);

        assertEquals(GRUPO, domain.getGrupo());
        assertEquals(SESION, domain.getSesion());
        assertEquals(USUARIO_EJECUTOR, domain.getUsuarioEjecutor());
    }

    @Test
    void rechaza_grupo_obligatorio() {
        assertThrows(ValidationException.class, () -> new ConsultarAsistenciasPorGrupoDomain(null, SESION, USUARIO_EJECUTOR));
    }
}
