package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.mapper.ConsultarMateriasEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.domain.MateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.mapper.ConsultarMateriasEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MateriaEstudianteProjection;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** CULM-001 D.9-a (CONTRACT §11.1, F-EST-006): los creditos de la asignatura llegan a la materia del estudiante (antes siempre nulos). */
class MateriaEstudianteCreditosMapperTest {

    private static final UUID ASIGNATURA = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    @Test
    void los_creditos_viajan_de_la_proyeccion_al_dominio_y_al_dto() {
        final MateriaEstudianteDomain domain = ConsultarMateriasEstudianteRepositoryMapper.toDomain(
                new MateriaEstudianteProjection(ASIGNATURA, "Calculo", GRUPO, "G1", 4));

        final MateriaEstudianteDTO dto = ConsultarMateriasEstudianteMapper.toDTO(domain);

        assertEquals(4, domain.creditos());
        assertEquals(4, dto.creditos());
        assertEquals("Calculo", dto.nombreAsignatura());
    }

    @Test
    void sin_creditos_en_la_proyeccion_el_dto_los_deja_nulos() {
        final MateriaEstudianteProjection sin = new MateriaEstudianteProjection(ASIGNATURA, "Calculo", GRUPO, "G1");

        assertNull(sin.creditos());
        assertNull(ConsultarMateriasEstudianteMapper.toDTO(ConsultarMateriasEstudianteRepositoryMapper.toDomain(sin)).creditos());
        assertNull(new MateriaEstudianteDTO(ASIGNATURA, "Calculo", GRUPO, "G1").creditos());
        assertNull(new MateriaEstudianteDomain(ASIGNATURA, "Calculo", GRUPO, "G1").creditos());
    }
}
