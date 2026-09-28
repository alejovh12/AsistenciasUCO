package co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.dto.HorarioDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.domain.HorarioDocenteDomain;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contrato TARGET (LB-001B.3, CONTRACT_FREEZE.md secc. 4 / punto D de TASK_AUTORIZADA.md &sect;27):
 * {@code HorarioDocenteDTO} es la respuesta HTTP directa de {@code GET /api/v1/docente/horarios}
 * (sin wrapper intermedio). Dado que {@code uv_horario_docente} congelada NO expone {@code aula},
 * {@code HorarioDocenteDomain} y {@code HorarioDocenteDTO} pasan de 11 a 10 componentes.
 *
 * <p>RED esperado: ambos records todavia declaran el constructor AS-IS de 11 parametros. Este test
 * invoca el constructor TARGET de 10 parametros de {@code HorarioDocenteDomain}; el modulo completo
 * falla en la fase {@code test-compile} de Maven hasta que 04-implementador retire {@code aula} de
 * {@code HorarioDocenteDomain.java} y {@code HorarioDocenteDTO.java} conforme a CONTRACT_FREEZE.md
 * secc. 4/7.</p>
 */
class ConsultarHorariosDocenteMapperTest {

    @Test
    void toDTO_mapea_domain_sin_aula() {
        final UUID id = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final HorarioDocenteDomain domain = new HorarioDocenteDomain(
                id, docente, grupo, "MAT-01", "Calculo", "G1", "LUNES",
                LocalTime.of(8, 0), LocalTime.of(10, 0), 25);

        final HorarioDocenteDTO dto = ConsultarHorariosDocenteMapper.toDTO(domain);

        assertEquals(id, dto.id());
        assertEquals(docente, dto.idDocente());
        assertEquals(grupo, dto.idGrupo());
        assertEquals("MAT-01", dto.codigoMateria());
        assertEquals("Calculo", dto.nombreMateria());
        assertEquals("G1", dto.seccion());
        assertEquals("LUNES", dto.dia());
        assertEquals(LocalTime.of(8, 0), dto.horaInicio());
        assertEquals(LocalTime.of(10, 0), dto.horaFin());
        assertEquals(25, dto.totalEstudiantes());
    }
}
