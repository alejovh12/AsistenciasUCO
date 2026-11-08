package co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.EjecutarCargaUsuariosInputPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.EjecutarCargaUsuariosCommand;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ErrorFilaCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.FilaResultadoCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.EjecutarCargaUsuariosUseCase;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EjecutarCargaUsuariosDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ErrorFilaCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.FilaResultadoCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ResultadoCargaDomain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EjecutarCargaUsuariosInteractor implements EjecutarCargaUsuariosInputPort {

    private final EjecutarCargaUsuariosUseCase useCase;

    public EjecutarCargaUsuariosInteractor(final EjecutarCargaUsuariosUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "EjecutarCargaUsuariosUseCase es obligatorio.");
    }

    @Override
    public ResultadoCargaDTO ejecutar(final EjecutarCargaUsuariosCommand command) {
        Objects.requireNonNull(command, "EjecutarCargaUsuariosCommand es obligatorio.");
        final EjecutarCargaUsuariosDomain domain = new EjecutarCargaUsuariosDomain(
                command.usuarioEjecutor(),
                command.perfilDestino(),
                command.contenidoBytes(),
                command.huella()
        );
        final ResultadoCargaDomain resultadoDomain = useCase.execute(domain);

        final List<FilaResultadoCargaDTO> filasDTO = new ArrayList<>();
        for (final FilaResultadoCargaDomain fila : resultadoDomain.filas()) {
            final List<ErrorFilaCargaDTO> erroresDTO = new ArrayList<>();
            for (final ErrorFilaCargaDomain err : fila.errores()) {
                erroresDTO.add(new ErrorFilaCargaDTO(err.campo(), err.codigoError(), err.mensaje()));
            }
            filasDTO.add(new FilaResultadoCargaDTO(
                    fila.numeroFila(),
                    fila.numeroDocumento(),
                    fila.correo(),
                    fila.estado(),
                    fila.idUsuarioCreado(),
                    erroresDTO
            ));
        }

        return new ResultadoCargaDTO(
                resultadoDomain.totalFilas(),
                resultadoDomain.creadas(),
                resultadoDomain.yaExistentes(),
                resultadoDomain.conError(),
                resultadoDomain.cuentasPendientes(),
                filasDTO,
                resultadoDomain.mensaje()
        );
    }
}
