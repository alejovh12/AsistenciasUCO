package co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.dto.HorarioEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.domain.HorarioEstudianteDomain;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultarHorariosEstudianteMapperTest {

    @Test
    void toDTO_conserva_todos_los_campos_del_dominio() {
        final UUID id = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final HorarioEstudianteDomain domain = new HorarioEstudianteDomain(
                id, estudiante, grupo, "MAT101", "Calculo", "G1", "LUNES",
                LocalTime.of(8, 0), LocalTime.of(10, 0), "Docente Uno");

        final HorarioEstudianteDTO dto = ConsultarHorariosEstudianteMapper.toDTO(domain);

        assertEquals(id, dto.id());
        assertEquals(estudiante, dto.idEstudiante());
        assertEquals(grupo, dto.idGrupo());
        assertEquals("MAT101", dto.codigoMateria());
        assertEquals("Calculo", dto.nombreMateria());
        assertEquals("G1", dto.grupo());
        assertEquals("LUNES", dto.dia());
        assertEquals(LocalTime.of(8, 0), dto.horaInicio());
        assertEquals(LocalTime.of(10, 0), dto.horaFin());
        assertEquals("Docente Uno", dto.docente());
    }

    @Test
    void toDTO_con_dominio_nulo_falla() {
        assertThrows(NullPointerException.class, () -> ConsultarHorariosEstudianteMapper.toDTO(null));
    }
}
