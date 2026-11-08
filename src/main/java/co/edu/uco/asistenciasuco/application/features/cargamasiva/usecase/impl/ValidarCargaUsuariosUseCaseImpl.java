package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.exception.CargaMasivaErrorCode;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CargaUsuariosValidacionPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CsvCargaUsuariosParserPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaCsvParaSp;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaValidacionDbProjection;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.ValidarCargaUsuariosUseCase;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ErrorFilaCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EstadoFilaValidacion;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.FilaCsvParseada;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.FilaValidacionCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ResultadoValidacionCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ValidarCargaUsuariosDomain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ValidarCargaUsuariosUseCaseImpl implements ValidarCargaUsuariosUseCase {

    private final CargaUsuariosValidacionPort validacionPort;
    private final CsvCargaUsuariosParserPort parserPort;

    public ValidarCargaUsuariosUseCaseImpl(
            final CargaUsuariosValidacionPort validacionPort,
            final CsvCargaUsuariosParserPort parserPort
    ) {
        this.validacionPort = Objects.requireNonNull(validacionPort, "CargaUsuariosValidacionPort es obligatorio.");
        this.parserPort = Objects.requireNonNull(parserPort, "CsvCargaUsuariosParserPort es obligatorio.");
    }

    @Override
    public ResultadoValidacionCargaDomain execute(final ValidarCargaUsuariosDomain domain) {
        Objects.requireNonNull(domain, "ValidarCargaUsuariosDomain es obligatorio.");

        final String huella = calcularSha256(domain.contenidoBytes());
        final List<FilaCsvParseada> filasParseadas = parserPort.parsear(domain.contenidoBytes(), domain.perfilDestino());

        final List<FilaCsvParaSp> filasParaSp = new ArrayList<>();
        for (final FilaCsvParseada fila : filasParseadas) {
            filasParaSp.add(new FilaCsvParaSp(
                    fila.numeroFila(),
                    fila.tipoDocumento(),
                    fila.numeroDocumento(),
                    fila.correo(),
                    fila.codigoTipoVinculacion(),
                    fila.codigoPrograma(),
                    fila.codigoFacultad()
            ));
        }

        final List<FilaValidacionDbProjection> proyeccionesDb = validacionPort.validarEnBaseDatos(
                domain.usuarioEjecutor(), domain.perfilDestino(), filasParaSp
        );

        final Map<Integer, FilaValidacionDbProjection> mapaDb = new HashMap<>();
        if (proyeccionesDb != null) {
            for (final FilaValidacionDbProjection proj : proyeccionesDb) {
                mapaDb.put(proj.numeroFila(), proj);
            }
        }

        final List<FilaValidacionCargaDomain> filasResultado = new ArrayList<>();
        int validas = 0;
        int yaExistentes = 0;
        int conError = 0;

        for (final FilaCsvParseada fila : filasParseadas) {
            final List<ErrorFilaCargaDomain> errores = new ArrayList<>(fila.erroresLocales());
            final FilaValidacionDbProjection dbProj = mapaDb.get(fila.numeroFila());

            boolean yaExiste = false;
            boolean esValidaDb = false;

            if (dbProj != null) {
                yaExiste = dbProj.yaExiste();
                esValidaDb = dbProj.esValida();

                if (dbProj.codigoMensaje() != null && !dbProj.codigoMensaje().isBlank()) {
                    errores.add(new ErrorFilaCargaDomain(
                            "fila",
                            dbProj.codigoMensaje(),
                            dbProj.mensajeUsuario() != null ? dbProj.mensajeUsuario() : dbProj.codigoMensaje()
                    ));
                }
            }

            // Si es persona nueva y no tiene clave inicial, reportar CMA_012
            if (!yaExiste && (fila.claveInicial() == null || fila.claveInicial().isBlank())) {
                errores.add(new ErrorFilaCargaDomain("clave_inicial", CargaMasivaErrorCode.CMA_012.code(), "Falta la clave inicial para una persona nueva."));
            }

            final EstadoFilaValidacion estado;
            if (!errores.isEmpty()) {
                estado = EstadoFilaValidacion.ERROR;
                conError++;
            } else if (yaExiste) {
                estado = EstadoFilaValidacion.YA_EXISTE;
                yaExistentes++;
            } else if (esValidaDb) {
                estado = EstadoFilaValidacion.VALIDA;
                validas++;
            } else {
                estado = EstadoFilaValidacion.ERROR;
                conError++;
            }

            filasResultado.add(new FilaValidacionCargaDomain(
                    fila.numeroFila(),
                    fila.numeroDocumento(),
                    fila.correo(),
                    estado,
                    errores
            ));
        }

        return new ResultadoValidacionCargaDomain(
                huella,
                domain.perfilDestino(),
                filasParseadas.size(),
                validas,
                yaExistentes,
                conError,
                filasResultado
        );
    }

    public static String calcularSha256(final byte[] bytes) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculando SHA-256", e);
        }
    }
}
