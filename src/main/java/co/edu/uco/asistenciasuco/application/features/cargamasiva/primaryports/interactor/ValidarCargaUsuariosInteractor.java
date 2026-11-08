package co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.ValidarCargaUsuariosInputPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ErrorFilaCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.FilaValidacionCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoValidacionCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ValidarCargaUsuariosCommand;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.ValidarCargaUsuariosUseCase;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ErrorFilaCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.FilaValidacionCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ResultadoValidacionCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ValidarCargaUsuariosDomain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ValidarCargaUsuariosInteractor implements ValidarCargaUsuariosInputPort {

    private final ValidarCargaUsuariosUseCase useCase;

    public ValidarCargaUsuariosInteractor(final ValidarCargaUsuariosUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ValidarCargaUsuariosUseCase es obligatorio.");
    }

    @Override
    public ResultadoValidacionCargaDTO validar(final ValidarCargaUsuariosCommand command) {
        Objects.requireNonNull(command, "ValidarCargaUsuariosCommand es obligatorio.");
        final ValidarCargaUsuariosDomain domain = new ValidarCargaUsuariosDomain(
                command.usuarioEjecutor(),
                command.perfilDestino(),
                command.contenidoBytes()
        );
        final ResultadoValidacionCargaDomain resultadoDomain = useCase.execute(domain);

        final List<FilaValidacionCargaDTO> filasDTO = new ArrayList<>();
        for (final FilaValidacionCargaDomain fila : resultadoDomain.filas()) {
            final List<ErrorFilaCargaDTO> erroresDTO = new ArrayList<>();
            for (final ErrorFilaCargaDomain err : fila.errores()) {
                erroresDTO.add(new ErrorFilaCargaDTO(err.campo(), err.codigoError(), err.mensaje()));
            }
            filasDTO.add(new FilaValidacionCargaDTO(
                    fila.numeroFila(),
                    fila.numeroDocumento(),
                    fila.correo(),
                    fila.estado(),
                    erroresDTO
            ));
        }

        return new ResultadoValidacionCargaDTO(
                resultadoDomain.huella(),
                resultadoDomain.perfilDestino(),
                resultadoDomain.totalFilas(),
                resultadoDomain.validas(),
                resultadoDomain.yaExistentes(),
                resultadoDomain.conError(),
                filasDTO
        );
    }
}
