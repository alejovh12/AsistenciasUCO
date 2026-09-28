package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.domain.ConsultarAsistenciasPorGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.entity.AsistenciaConsultadaEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarAsistenciasPorGrupoUseCaseImplTest {

    private static final UUID GRUPO_PROPIO = UUID.randomUUID();
    private static final UUID GRUPO_AJENO = UUID.randomUUID();
    private static final UUID DOCENTE_A = UUID.randomUUID();
    private static final UUID DOCENTE_B = UUID.randomUUID();
    private static final UUID ADMINISTRADOR = UUID.randomUUID();

    private final AsistenciaRepositoryPort asistenciaRepositoryPort = mock(AsistenciaRepositoryPort.class);
    private final InstitutionalScopePort institutionalScopePort = mock(InstitutionalScopePort.class);
    private final ConsultarAsistenciasPorGrupoUseCaseImpl useCase =
            new ConsultarAsistenciasPorGrupoUseCaseImpl(asistenciaRepositoryPort, institutionalScopePort);

    @Test
    void docente_propietario_del_grupo_consulta_sus_asistencias() {
        when(institutionalScopePort.findDocenteIdByUsuario(DOCENTE_A)).thenReturn(Optional.of(UUID.randomUUID()));
        when(institutionalScopePort.canDocenteAccessGrupo(DOCENTE_A, GRUPO_PROPIO)).thenReturn(true);
        when(asistenciaRepositoryPort.consultarAsistenciasPorGrupo(any())).thenReturn(List.of());

        final List<AsistenciaConsultadaEntity> resultado =
                useCase.execute(new ConsultarAsistenciasPorGrupoDomain(GRUPO_PROPIO, null, DOCENTE_A));

        assertEquals(0, resultado.size());
    }

    @Test
    void docente_ajeno_al_grupo_es_rechazado_con_forbidden() {
        when(institutionalScopePort.findDocenteIdByUsuario(DOCENTE_B)).thenReturn(Optional.of(UUID.randomUUID()));
        when(institutionalScopePort.canDocenteAccessGrupo(DOCENTE_B, GRUPO_AJENO)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> useCase.execute(new ConsultarAsistenciasPorGrupoDomain(GRUPO_AJENO, null, DOCENTE_B)));
    }

    @Test
    void administrador_sin_identidad_docente_conserva_visibilidad_institucional_amplia() {
        when(institutionalScopePort.findDocenteIdByUsuario(ADMINISTRADOR)).thenReturn(Optional.empty());
        when(asistenciaRepositoryPort.consultarAsistenciasPorGrupo(any())).thenReturn(List.of(
                new AsistenciaRepositoryProjection(UUID.randomUUID(), UUID.randomUUID(), GRUPO_AJENO,
                        UUID.randomUUID(), true, "AN", null)
        ));

        final List<AsistenciaConsultadaEntity> resultado =
                useCase.execute(new ConsultarAsistenciasPorGrupoDomain(GRUPO_AJENO, null, ADMINISTRADOR));

        assertEquals(1, resultado.size());
    }
}
