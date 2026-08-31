package co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.entity.GrupoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarGruposUseCaseImplTest {

    private static final UUID GRUPO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID ASIGNATURA = UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID DOCENTE = UUID.fromString("43641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void execute_consulta_puerto_secundario_y_mapea_grupos() {
        final GrupoRepositoryPort repositoryPort = new GrupoRepositoryPort() {
            @Override
            public RegistrarEstudianteRepositoryProjection registrarEstudianteEnGrupo(final RegistrarEstudianteRepositoryDTO dto) {
                return new RegistrarEstudianteRepositoryProjection("ok");
            }

            @Override
            public List<GrupoRepositoryProjection> consultarGrupos() {
                return List.of(new GrupoRepositoryProjection(
                        GRUPO,
                        "G1",
                        "Grupo 1",
                        ASIGNATURA,
                        "Backend",
                        DOCENTE,
                        30,
                        12,
                        18,
                        true,
                        LocalDate.of(2026, 1, 20),
                        LocalDate.of(2026, 5, 30)
                ));
            }
        };
        final ConsultarGruposUseCaseImpl useCase = new ConsultarGruposUseCaseImpl(repositoryPort);

        final List<GrupoEntity> resultado = useCase.execute();

        assertEquals(1, resultado.size());
        assertEquals(GRUPO, resultado.getFirst().getId());
        assertEquals("Backend", resultado.getFirst().getNombreAsignatura());
        assertTrue(resultado.getFirst().isGrupoHabilitado());
    }
}
