package co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.domain.ConsultarAsignacionesAcademicasDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.entity.DocenteAsignacionAcademicaEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteAsignacionAcademicaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarAsignacionesAcademicasDocenteRepositoryMapperTest {

    private static final UUID DOCENTE = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID CORRELACION = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void toRepositoryDTO_mapea_dominio_a_contrato_de_persistencia() {
        final ConsultarAsignacionesAcademicasDocenteRepositoryDTO dto =
                ConsultarAsignacionesAcademicasDocenteRepositoryMapper.toRepositoryDTO(
                        ConsultarAsignacionesAcademicasDocenteDomain.crear(
                                DOCENTE
                        )
                );

        assertEquals(DOCENTE, dto.getDocente());
    }

    @Test
    void toUseCaseEntities_retorna_lista_vacia_si_entrada_es_nula() {
        assertTrue(ConsultarAsignacionesAcademicasDocenteRepositoryMapper.toUseCaseEntities(null).isEmpty());
    }

    @Test
    void toUseCaseEntities_convierte_estado_activo_cero_y_uno() {
        final List<DocenteAsignacionAcademicaEntity> inactivo =
                ConsultarAsignacionesAcademicasDocenteRepositoryMapper.toUseCaseEntities(List.of(entity(0)));
        final List<DocenteAsignacionAcademicaEntity> activo =
                ConsultarAsignacionesAcademicasDocenteRepositoryMapper.toUseCaseEntities(List.of(entity(1)));

        assertFalse(inactivo.get(0).isEstaActivoDocente());
        assertTrue(activo.get(0).isEstaActivoDocente());
        assertEquals("ACTIVO", activo.get(0).getEstaActivoTextoDocente());
    }

    @Test
    void toUseCaseEntities_rechaza_estado_activo_fuera_de_cero_uno() {
        assertThrows(
                CrosscuttingException.class,
                () -> ConsultarAsignacionesAcademicasDocenteRepositoryMapper.toUseCaseEntities(List.of(entity(2)))
        );
    }

    private DocenteAsignacionAcademicaRepositoryProjection entity(final Integer estadoDocente) {
        return new DocenteAsignacionAcademicaRepositoryProjection(
                DOCENTE,
                USUARIO,
                123456789,
                "Ana Perez",
                true,
                UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c"),
                "UCO",
                UUID.fromString("43641bab-e3cd-485c-b275-47e7b731e18c"),
                "Ingenieria",
                UUID.fromString("53641bab-e3cd-485c-b275-47e7b731e18c"),
                "Sistemas",
                UUID.fromString("63641bab-e3cd-485c-b275-47e7b731e18c"),
                "2024",
                UUID.fromString("73641bab-e3cd-485c-b275-47e7b731e18c"),
                "Backend",
                UUID.fromString("83641bab-e3cd-485c-b275-47e7b731e18c"),
                "G1",
                UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c"),
                "DO",
                "Docente",
                estadoDocente,
                "ACTIVO"
        );
    }
}
