package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.CrearDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain.CrearDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.exception.CargaMasivaErrorCode;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CargaUsuariosValidacionPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CsvCargaUsuariosParserPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaCsvParaSp;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaValidacionDbProjection;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.EjecutarCargaUsuariosUseCase;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EjecutarCargaUsuariosDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ErrorFilaCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EstadoFilaResultado;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.FilaCsvParseada;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.FilaResultadoCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ResultadoCargaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.VincularDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.domain.VincularDocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.CrearCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.AsegurarCuentaIdentidadService;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.ResultadoCuentaIdentidad;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.SolicitudCuentaIdentidad;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class EjecutarCargaUsuariosUseCaseImpl implements EjecutarCargaUsuariosUseCase {

    private final CargaUsuariosValidacionPort validacionPort;
    private final CsvCargaUsuariosParserPort parserPort;
    private final VincularDocenteProgramaUseCase vincularDocenteUseCase;
    private final CrearCoordinadorUseCase crearCoordinadorUseCase;
    private final CrearDecanoUseCase crearDecanoUseCase;
    private final AsegurarCuentaIdentidadService asegurarCuentaService;

    public EjecutarCargaUsuariosUseCaseImpl(
            final CargaUsuariosValidacionPort validacionPort,
            final CsvCargaUsuariosParserPort parserPort,
            final VincularDocenteProgramaUseCase vincularDocenteUseCase,
            final CrearCoordinadorUseCase crearCoordinadorUseCase,
            final CrearDecanoUseCase crearDecanoUseCase,
            final AsegurarCuentaIdentidadService asegurarCuentaService
    ) {
        this.validacionPort = Objects.requireNonNull(validacionPort, "CargaUsuariosValidacionPort es obligatorio.");
        this.parserPort = Objects.requireNonNull(parserPort, "CsvCargaUsuariosParserPort es obligatorio.");
        this.vincularDocenteUseCase = Objects.requireNonNull(vincularDocenteUseCase, "VincularDocenteProgramaUseCase es obligatorio.");
        this.crearCoordinadorUseCase = Objects.requireNonNull(crearCoordinadorUseCase, "CrearCoordinadorUseCase es obligatorio.");
        this.crearDecanoUseCase = Objects.requireNonNull(crearDecanoUseCase, "CrearDecanoUseCase es obligatorio.");
        this.asegurarCuentaService = Objects.requireNonNull(asegurarCuentaService, "AsegurarCuentaIdentidadService es obligatorio.");
    }

    @Override
    public ResultadoCargaDomain execute(final EjecutarCargaUsuariosDomain domain) {
        Objects.requireNonNull(domain, "EjecutarCargaUsuariosDomain es obligatorio.");

        final String huellaRecalculada = ValidarCargaUsuariosUseCaseImpl.calcularSha256(domain.contenidoBytes());
        if (!huellaRecalculada.equalsIgnoreCase(domain.huella())) {
            throw new ConflictException(CargaMasivaErrorCode.CMA_003);
        }

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

        final List<FilaResultadoCargaDomain> filasResultado = new ArrayList<>();
        int creadas = 0;
        int yaExistentes = 0;
        int conError = 0;
        int cuentasPendientes = 0;

        for (final FilaCsvParseada fila : filasParseadas) {
            final List<ErrorFilaCargaDomain> erroresLocales = new ArrayList<>(fila.erroresLocales());
            final FilaValidacionDbProjection dbProj = mapaDb.get(fila.numeroFila());

            boolean yaExisteDb = false;
            boolean esValidaDb = false;

            if (dbProj != null) {
                yaExisteDb = dbProj.yaExiste();
                esValidaDb = dbProj.esValida();

                if (dbProj.codigoMensaje() != null && !dbProj.codigoMensaje().isBlank()) {
                    erroresLocales.add(new ErrorFilaCargaDomain(
                            "fila",
                            dbProj.codigoMensaje(),
                            dbProj.mensajeUsuario() != null ? dbProj.mensajeUsuario() : dbProj.codigoMensaje()
                    ));
                }
            }

            if (!yaExisteDb && (fila.claveInicial() == null || fila.claveInicial().isBlank())) {
                erroresLocales.add(new ErrorFilaCargaDomain("clave_inicial", CargaMasivaErrorCode.CMA_012.code(), "Falta la clave inicial para una persona nueva."));
            }

            if (!erroresLocales.isEmpty() || (!yaExisteDb && !esValidaDb)) {
                conError++;
                filasResultado.add(new FilaResultadoCargaDomain(
                        fila.numeroFila(),
                        fila.numeroDocumento(),
                        fila.correo(),
                        EstadoFilaResultado.ERROR,
                        null,
                        erroresLocales
                ));
                continue;
            }

            if (yaExisteDb) {
                final UUID usuarioExistenteId = dbProj != null ? dbProj.idUsuarioExistente() : null;
                EstadoFilaResultado estadoYaExiste = EstadoFilaResultado.YA_EXISTE;

                try {
                    final SolicitudCuentaIdentidad solicitud = new SolicitudCuentaIdentidad(
                            usuarioExistenteId,
                            fila.numeroDocumento(),
                            fila.correo(),
                            fila.primerNombre(),
                            fila.primerApellido(),
                            fila.claveInicial(),
                            mapearRol(domain.perfilDestino()),
                            false
                    );
                    final ResultadoCuentaIdentidad reparacion = asegurarCuentaService.repararOFallar(solicitud);
                    if (reparacion != null && reparacion.cuentaReparada()) {
                        estadoYaExiste = EstadoFilaResultado.YA_EXISTE_CUENTA_REPARADA;
                    }
                } catch (final ConflictException e) {
                    // Ya existia en IdP
                    estadoYaExiste = EstadoFilaResultado.YA_EXISTE;
                } catch (final Exception e) {
                    estadoYaExiste = EstadoFilaResultado.YA_EXISTE;
                }

                yaExistentes++;
                filasResultado.add(new FilaResultadoCargaDomain(
                        fila.numeroFila(),
                        fila.numeroDocumento(),
                        fila.correo(),
                        estadoYaExiste,
                        usuarioExistenteId,
                        List.of()
                ));
                continue;
            }

            // Fila valida: creacion secuencial aislada
            try {
                UUID idUsuarioCreado = null;
                switch (domain.perfilDestino()) {
                    case DOCENTE -> {
                        final VincularDocenteProgramaDomain docenteDomain = new VincularDocenteProgramaDomain(
                                domain.usuarioEjecutor(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                dbProj.idTipoIdentificacion(),
                                Integer.parseInt(fila.numeroDocumento()),
                                fila.primerNombre(),
                                fila.segundoNombre().isBlank() ? null : fila.segundoNombre(),
                                fila.primerApellido(),
                                fila.segundoApellido().isBlank() ? null : fila.segundoApellido(),
                                fila.correo(),
                                fila.claveInicial(),
                                dbProj.idTipoVinculacion()
                        );
                        final DocenteProgramaDomain resultadoDocente = vincularDocenteUseCase.execute(docenteDomain);
                        idUsuarioCreado = resultadoDocente != null ? resultadoDocente.idUsuario() : null;
                    }
                    case COORDINADOR -> {
                        final CrearCoordinadorDomain coordDomain = new CrearCoordinadorDomain(
                                dbProj.idTipoIdentificacion(),
                                fila.numeroDocumento(),
                                fila.primerNombre(),
                                fila.segundoNombre().isBlank() ? null : fila.segundoNombre(),
                                fila.primerApellido(),
                                fila.segundoApellido().isBlank() ? null : fila.segundoApellido(),
                                fila.correo(),
                                dbProj.idPrograma(),
                                fila.claveInicial(),
                                domain.usuarioEjecutor()
                        );
                        crearCoordinadorUseCase.execute(coordDomain);
                        idUsuarioCreado = dbProj.idUsuarioExistente();
                    }
                    case DECANO -> {
                        final CrearDecanoDomain decanoDomain = new CrearDecanoDomain(
                                dbProj.idTipoIdentificacion(),
                                Integer.parseInt(fila.numeroDocumento()),
                                fila.primerNombre(),
                                fila.segundoNombre().isBlank() ? null : fila.segundoNombre(),
                                fila.primerApellido(),
                                fila.segundoApellido().isBlank() ? null : fila.segundoApellido(),
                                fila.correo(),
                                fila.claveInicial(),
                                dbProj.idFacultad(),
                                domain.usuarioEjecutor()
                        );
                        crearDecanoUseCase.execute(decanoDomain);
                        idUsuarioCreado = dbProj.idUsuarioExistente();
                    }
                }

                creadas++;
                filasResultado.add(new FilaResultadoCargaDomain(
                        fila.numeroFila(),
                        fila.numeroDocumento(),
                        fila.correo(),
                        EstadoFilaResultado.CREADA,
                        idUsuarioCreado,
                        List.of()
                ));
            } catch (final Exception ex) {
                conError++;
                final String codigo = ex instanceof ApplicationException appEx ? appEx.getCode() : "CMA_006";
                final String mensaje = ex.getMessage() != null ? ex.getMessage() : "Error procesando la fila.";
                filasResultado.add(new FilaResultadoCargaDomain(
                        fila.numeroFila(),
                        fila.numeroDocumento(),
                        fila.correo(),
                        EstadoFilaResultado.ERROR,
                        null,
                        List.of(new ErrorFilaCargaDomain("fila", codigo, mensaje))
                ));
            }
        }

        final String mensaje = String.format("Carga procesada: %d creadas, %d ya existentes, %d con error.",
                creadas, yaExistentes, conError);

        return new ResultadoCargaDomain(
                filasParseadas.size(),
                creadas,
                yaExistentes,
                conError,
                cuentasPendientes,
                filasResultado,
                mensaje
        );
    }

    private static InstitutionalRole mapearRol(final PerfilDestinoCarga perfilDestino) {
        return switch (perfilDestino) {
            case DOCENTE -> InstitutionalRole.DOCENTE;
            case COORDINADOR -> InstitutionalRole.COORDINADOR;
            case DECANO -> InstitutionalRole.DECANO;
        };
    }
}
