package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain.SolicitarRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolicitarRevisionAsistenciaRepositoryMapperTest {

    @Test
    void toRepositoryDTO_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class,
                () -> SolicitarRevisionAsistenciaRepositoryMapper.toRepositoryDTO(null, UUID.randomUUID()));
    }

    @Test
    void toRepositoryDTO_usa_estudianteId_resuelto_y_propaga_usuarioEjecutor_del_dominio() {
        final UUID usuario = UUID.randomUUID();
        final UUID estudianteId = UUID.randomUUID();
        final UUID sesion = UUID.randomUUID();
        final UUID soporte = UUID.randomUUID();
        final UUID soporte2 = UUID.randomUUID();
        final SolicitarRevisionAsistenciaDomain domain = new SolicitarRevisionAsistenciaDomain(
                sesion, "SALUD", "Justificacion", List.of(soporte, soporte2), usuario
        );

        final var dto = SolicitarRevisionAsistenciaRepositoryMapper.toRepositoryDTO(domain, estudianteId);

        assertEquals(estudianteId, dto.estudiante());
        assertEquals(sesion, dto.sesion());
        assertEquals(List.of(soporte, soporte2), dto.soportes());
        assertEquals(usuario, dto.usuarioEjecutor());
        assertEquals(usuario, domain.getUsuario());
    }
}
