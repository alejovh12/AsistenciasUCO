package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultarSesionesPorGrupoDomainTest {

    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USUARIO = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void construye_dominio_valido() {
        final ConsultarSesionesPorGrupoDomain domain = new ConsultarSesionesPorGrupoDomain(GRUPO, USUARIO);

        assertEquals(GRUPO, domain.getGrupo());
        assertEquals(USUARIO, domain.getUsuarioEjecutor());
    }

    @Test
    void rechaza_grupo_nulo_con_codigo_de_grupo_requerido() {
        final ValidationException ex = assertThrows(ValidationException.class,
                () -> new ConsultarSesionesPorGrupoDomain(null, USUARIO));

        assertEquals(GrupoErrorCode.ERR_GRUPO_REQUERIDO.code(), ex.getCode());
    }

    @Test
    void rechaza_usuario_ejecutor_nulo_con_codigo_de_docente_requerido() {
        final ValidationException ex = assertThrows(ValidationException.class,
                () -> new ConsultarSesionesPorGrupoDomain(GRUPO, null));

        assertEquals(SesionErrorCode.ERR_DOCENTE_REQUERIDO.code(), ex.getCode());
    }
}
