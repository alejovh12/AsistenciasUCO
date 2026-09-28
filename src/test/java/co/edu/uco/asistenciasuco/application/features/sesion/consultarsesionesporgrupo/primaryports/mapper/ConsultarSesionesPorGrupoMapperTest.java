package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.dto.ConsultarSesionesPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.domain.ConsultarSesionesPorGrupoDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarSesionesPorGrupoMapperTest {

    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USUARIO = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ConsultarSesionesPorGrupoMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_conserva_grupo_y_usuario_ejecutor() {
        final ConsultarSesionesPorGrupoDomain domain =
                ConsultarSesionesPorGrupoMapper.toDomain(new ConsultarSesionesPorGrupoDTO(GRUPO, USUARIO));

        assertEquals(GRUPO, domain.getGrupo());
        assertEquals(USUARIO, domain.getUsuarioEjecutor());
    }

    @Test
    void toDTOs_con_lista_nula_devuelve_lista_vacia() {
        assertTrue(ConsultarSesionesPorGrupoMapper.toDTOs(null).isEmpty());
    }

    @Test
    void toDTOs_con_lista_vacia_devuelve_lista_vacia() {
        assertTrue(ConsultarSesionesPorGrupoMapper.toDTOs(List.of()).isEmpty());
    }

    @Test
    void toDTOs_mapea_cada_sesion_en_orden() {
        final UUID sesionA = UUID.randomUUID();
        final UUID sesionB = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.of(2026, 3, 2, 8, 0);
        final SesionConsultadaEntity a = new SesionConsultadaEntity(
                sesionA, GRUPO, "Tema A", 1, "SES-01", 1, "G1", "Grupo 1", inicio, inicio.plusHours(2));
        final SesionConsultadaEntity b = new SesionConsultadaEntity(
                sesionB, GRUPO, "Tema B", 2, "SES-02", 2, "G1", "Grupo 1", inicio.plusDays(7), inicio.plusDays(7).plusHours(2));

        final List<SesionConsultadaDTO> dtos = ConsultarSesionesPorGrupoMapper.toDTOs(List.of(a, b));

        assertEquals(2, dtos.size());
        assertEquals(sesionA, dtos.get(0).getSesion());
        assertEquals("Tema A", dtos.get(0).getNombre());
        assertEquals(sesionB, dtos.get(1).getSesion());
        assertEquals("SES-02", dtos.get(1).getCodigo());
        assertEquals(GRUPO, dtos.get(1).getGrupo());
    }
}
