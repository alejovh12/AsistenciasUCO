package co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.domain.HorarioDocenteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioDocenteProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contrato TARGET (LB-001B.3, CONTRACT_FREEZE.md secc. 4 / punto D de TASK_AUTORIZADA.md &sect;27):
 * {@code uv_horario_docente} congelada NO expone {@code aula}. {@code HorarioDocenteProjection} y
 * {@code HorarioDocenteDomain} pasan de 11 a 10 componentes (sin {@code aula}).
 *
 * <p>RED esperado: ambos records todavia declaran el constructor AS-IS de 11 parametros (con
 * {@code aula} como decimo componente, antes de {@code totalEstudiantes}). Este test invoca el
 * constructor TARGET de 10 parametros de {@code HorarioDocenteProjection}; el modulo completo falla
 * en la fase {@code test-compile} de Maven ("no suitable constructor found") hasta que
 * 04-implementador retire {@code aula} de {@code HorarioDocenteProjection.java} y
 * {@code HorarioDocenteDomain.java} conforme a CONTRACT_FREEZE.md secc. 4/7.</p>
 */
class ConsultarHorariosDocenteRepositoryMapperTest {

    @Test
    void toDomain_mapea_proyeccion_sin_aula() {
        final UUID id = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final HorarioDocenteProjection projection = new HorarioDocenteProjection(
                id, docente, grupo, "MAT-01", "Calculo", "G1", "LUNES",
                LocalTime.of(8, 0), LocalTime.of(10, 0), 25);

        final HorarioDocenteDomain domain = ConsultarHorariosDocenteRepositoryMapper.toDomain(projection);

        assertEquals(id, domain.id());
        assertEquals(docente, domain.idDocente());
        assertEquals(grupo, domain.idGrupo());
        assertEquals("MAT-01", domain.codigoMateria());
        assertEquals("Calculo", domain.nombreMateria());
        assertEquals("G1", domain.seccion());
        assertEquals("LUNES", domain.dia());
        assertEquals(LocalTime.of(8, 0), domain.horaInicio());
        assertEquals(LocalTime.of(10, 0), domain.horaFin());
        assertEquals(25, domain.totalEstudiantes());
    }
}
