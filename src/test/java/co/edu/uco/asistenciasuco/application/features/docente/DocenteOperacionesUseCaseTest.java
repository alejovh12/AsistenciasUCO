package co.edu.uco.asistenciasuco.application.features.docente;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.domain.ConsultarDocentePorIdDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.impl.ConsultarDocentePorIdUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteAsignacionAcademicaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteIdentidadRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocenteOperacionesUseCaseTest {

    private static final UUID DOCENTE = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void consultar_docente_por_id_inexistente_lanza_resourceNotFoundException_con_codigo_especifico() {
        final ConsultarDocentePorIdUseCaseImpl useCase = new ConsultarDocentePorIdUseCaseImpl(
                new StubDocenteRepositoryPort() {
                }
        );

        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> useCase.execute(ConsultarDocentePorIdDomain.crear(DOCENTE))
        );

        assertEquals("ERR_DOCENTE_NO_EXISTE", exception.getCode());
        assertEquals("El docente consultado no existe.", exception.getMessage());
    }

    private abstract static class StubDocenteRepositoryPort implements DocenteRepositoryPort {

        @Override
        public List<DocenteIdentidadRepositoryProjection> consultarDocentes() {
            return List.of();
        }

        @Override
        public Optional<DocenteIdentidadRepositoryProjection> consultarDocentePorId(
                final ConsultarDocentePorIdRepositoryDTO dto
        ) {
            return Optional.empty();
        }

        @Override
        public List<DocenteAsignacionAcademicaRepositoryProjection> consultarAsignacionesAcademicas(
                final ConsultarAsignacionesAcademicasDocenteRepositoryDTO dto
        ) {
            return List.of();
        }


    }
}
