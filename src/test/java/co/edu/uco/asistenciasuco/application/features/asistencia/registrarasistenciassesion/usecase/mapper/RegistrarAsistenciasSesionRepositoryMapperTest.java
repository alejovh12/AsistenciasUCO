package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistrarAsistenciasSesionDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistroAsistenciaSesionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrarAsistenciasSesionRepositoryMapperTest {

    @Test
    void toRepositoryDTO_con_dominio_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> RegistrarAsistenciasSesionRepositoryMapper.toRepositoryDTO(null));
    }

    @Test
    void toRepositoryDTO_con_dominio_valido_mapea_registros() {
        final UUID sesion = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        final RegistrarAsistenciasSesionDomain domain = new RegistrarAsistenciasSesionDomain(
                sesion, List.of(new RegistroAsistenciaSesionDomain(estudiante, "ASISTIO")));

        final RegistrarAsistenciasSesionRepositoryDTO dto = RegistrarAsistenciasSesionRepositoryMapper.toRepositoryDTO(domain);

        assertEquals(sesion, dto.sesion());
        assertEquals(1, dto.registros().size());
        assertEquals(estudiante, dto.registros().getFirst().estudiante());
    }
}
