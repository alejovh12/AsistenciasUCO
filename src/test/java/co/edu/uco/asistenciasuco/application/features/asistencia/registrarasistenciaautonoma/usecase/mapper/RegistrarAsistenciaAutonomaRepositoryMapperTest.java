package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.domain.RegistrarAsistenciaAutonomaDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrarAsistenciaAutonomaRepositoryMapperTest {

    @Test
    void toRepositoryDTO_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class,
                () -> RegistrarAsistenciaAutonomaRepositoryMapper.toRepositoryDTO(null, UUID.randomUUID()));
    }

    @Test
    void toRepositoryDTO_usa_estudianteId_resuelto_y_propaga_usuarioEjecutor_del_dominio() {
        final UUID usuario = UUID.randomUUID();
        final UUID estudianteId = UUID.randomUUID();
        final UUID sesion = UUID.randomUUID();
        final RegistrarAsistenciaAutonomaDomain domain =
                new RegistrarAsistenciaAutonomaDomain(sesion, "123456", usuario);

        final var dto = RegistrarAsistenciaAutonomaRepositoryMapper.toRepositoryDTO(domain, estudianteId);

        assertEquals(estudianteId, dto.estudiante());
        assertEquals(sesion, dto.sesion());
        assertEquals(usuario, dto.usuarioEjecutor());
    }
}
