package co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudesdocente.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.usecase.mapper.SolicitudesSesionMapper;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudesdocente.usecase.ConsultarSolicitudesSesionDocenteUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.PaginaSolicitudesSesion;

import java.util.Objects;

/** ATA D6: consulta paginada de las solicitudes del docente titular; traduce los DATETIME2 en UTC de la DB a instantes. */
public final class ConsultarSolicitudesSesionDocenteUseCaseImpl implements ConsultarSolicitudesSesionDocenteUseCase {

    static final int PAGINA_POR_DEFECTO = 1;
    static final int TAMANO_POR_DEFECTO = 20;

    private final SesionManualRepositoryPort repositorio;

    public ConsultarSolicitudesSesionDocenteUseCaseImpl(final SesionManualRepositoryPort repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "SesionManualRepositoryPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para consultar las solicitudes del docente es obligatorio.");
        final int pagina = comando.pagina() == null ? PAGINA_POR_DEFECTO : comando.pagina();
        final int tamano = comando.tamano() == null ? TAMANO_POR_DEFECTO : comando.tamano();
        final PaginaSolicitudesSesion datos = repositorio.consultarSolicitudesDocente(
                comando.usuarioId(), comando.soloPendientes(), comando.grupoId(), pagina, tamano);
        return SolicitudesSesionMapper.aResultado(datos, pagina, tamano);
    }
}
