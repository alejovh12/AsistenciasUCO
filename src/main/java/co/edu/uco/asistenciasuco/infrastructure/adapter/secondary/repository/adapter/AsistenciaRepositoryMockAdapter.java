package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador mock temporal para persistencia de asistencias.
 */
public final class AsistenciaRepositoryMockAdapter implements AsistenciaRepositoryPort {

    private static final UUID ASISTENCIA_1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID ASISTENCIA_2 = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID ESTUDIANTE_1 = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID ESTUDIANTE_2 = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final UUID SESION_1 = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID SESION_2 = UUID.fromString("00000000-0000-0000-0000-000000000302");

    @Override
    public void registrarAsistencia(final RegistrarAsistenciaRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para registrar asistencia es obligatorio.");
        }
    }

    @Override
    public List<AsistenciaRepositoryProjection> consultarAsistenciasPorGrupo(final ConsultarAsistenciasPorGrupoRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para consultar asistencias por grupo es obligatorio.");
        }

        final List<AsistenciaRepositoryProjection> asistencias = new ArrayList<>();
        final UUID sesionSolicitada = ObjectHelper.isNull(dto.getSesion()) ? SESION_1 : dto.getSesion();

        asistencias.add(new AsistenciaRepositoryProjection(
                ASISTENCIA_1,
                ESTUDIANTE_1,
                dto.getGrupo(),
                sesionSolicitada,
                true,
                null
        ));

        if (ObjectHelper.isNull(dto.getSesion())) {
            asistencias.add(new AsistenciaRepositoryProjection(
                    ASISTENCIA_2,
                    ESTUDIANTE_2,
                    dto.getGrupo(),
                    SESION_2,
                    false,
                    "Llego tarde y reporto novedad."
            ));
        }

        return asistencias;
    }

    @Override
    public void solicitarRevisionAsistencia(final SolicitarRevisionAsistenciaRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para solicitar revision de asistencia es obligatorio.");
        }
    }
}
