package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.AdmitirEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.interactor.AdmitirEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.AdmitirEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.impl.AdmitirEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.ConsultarProgramasInstitucionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.interactor.ConsultarProgramasInstitucionInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.usecase.ConsultarProgramasInstitucionUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.usecase.impl.ConsultarProgramasInstitucionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.CrearDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.AvisoAlertasInasistenciaService;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.EvaluadorAlertasInasistencia;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.consultaralertasestudiante.primaryports.ConsultarAlertasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.consultaralertasestudiante.usecase.impl.ConsultarAlertasEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.evaluaralertasautomatico.primaryports.EvaluarAlertasInasistenciaAutomaticoInputPort;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.evaluaralertasautomatico.usecase.impl.EvaluarAlertasInasistenciaAutomaticoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.EjecutarCargaUsuariosInputPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.ValidarCargaUsuariosInputPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.interactor.EjecutarCargaUsuariosInteractor;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.interactor.ValidarCargaUsuariosInteractor;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CargaUsuariosValidacionPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CsvCargaUsuariosParserPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.EjecutarCargaUsuariosUseCase;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.ValidarCargaUsuariosUseCase;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.impl.EjecutarCargaUsuariosUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.impl.ValidarCargaUsuariosUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.VincularDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.CrearCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.AsegurarCuentaIdentidadService;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.primaryports.ResolverAudienciaRealtimeInputPort;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.usecase.impl.ResolverAudienciaRealtimeUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.AvisoRevisionCancelacionService;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionescoordinador.primaryports.ConsultarRevisionesCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionescoordinador.usecase.impl.ConsultarRevisionesCoordinadorUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdecano.primaryports.ConsultarRevisionesDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdecano.usecase.impl.ConsultarRevisionesDecanoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.decidirrevision.primaryports.DecidirRevisionCancelacionInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.decidirrevision.usecase.impl.DecidirRevisionCancelacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.resolverrevision.primaryports.ResolverRevisionCancelacionInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.resolverrevision.usecase.impl.ResolverRevisionCancelacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdmisionEstudianteCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaInstitucionQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeAudienciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AlertaInasistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RevisionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.cargas.CsvCargaUsuariosParser;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler.EvaluacionAlertasInasistenciaScheduler;
import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/** CULM-001: los composition roots entregan el caso de uso correcto, con sus dependencias, y fallan si falta alguna. */
class Culm001WiringConfigurationsTest {

    @Test
    void carga_masiva_cablea_parser_casos_de_uso_interactores_y_limitador() {
        final CargaMasivaWiringConfiguration config = new CargaMasivaWiringConfiguration();
        final CargaUsuariosValidacionPort validacion = mock(CargaUsuariosValidacionPort.class);

        final CsvCargaUsuariosParserPort parser = config.csvCargaUsuariosParserPort();
        final ValidarCargaUsuariosUseCase validarUseCase = config.validarCargaUsuariosUseCase(validacion, parser);
        final ValidarCargaUsuariosInputPort validarPort = config.validarCargaUsuariosInputPort(validarUseCase);
        final EjecutarCargaUsuariosUseCase ejecutarUseCase = config.ejecutarCargaUsuariosUseCase(validacion, parser,
                mock(VincularDocenteProgramaUseCase.class), mock(CrearCoordinadorUseCase.class), mock(CrearDecanoUseCase.class),
                mock(AsegurarCuentaIdentidadService.class));
        final EjecutarCargaUsuariosInputPort ejecutarPort = config.ejecutarCargaUsuariosInputPort(ejecutarUseCase);
        final SlidingWindowRateLimiter limitador = config.cargaMasivaRateLimiter(10);

        assertInstanceOf(CsvCargaUsuariosParser.class, parser);
        assertInstanceOf(ValidarCargaUsuariosUseCaseImpl.class, validarUseCase);
        assertInstanceOf(ValidarCargaUsuariosInteractor.class, validarPort);
        assertInstanceOf(EjecutarCargaUsuariosUseCaseImpl.class, ejecutarUseCase);
        assertInstanceOf(EjecutarCargaUsuariosInteractor.class, ejecutarPort);
        assertNotNull(limitador);
        // El limitador usa el maximo de claves configurado: con 3 intentos por minuto el cuarto se rechaza.
        assertTrue(limitador.tryAcquire("actor", 3));
        assertTrue(limitador.tryAcquire("actor", 3));
        assertTrue(limitador.tryAcquire("actor", 3));
        assertTrue(!limitador.tryAcquire("actor", 3));
        assertThrows(NullPointerException.class, () -> config.validarCargaUsuariosUseCase(null, parser));
    }

    @Test
    void alertas_de_inasistencia_cablea_evaluador_consulta_job_audiencias_y_avisos() {
        final AlertaInasistenciaWiringConfiguration config = new AlertaInasistenciaWiringConfiguration();
        final AlertaInasistenciaRepositoryPort repositorio = mock(AlertaInasistenciaRepositoryPort.class);
        final NotificacionPort notificaciones = mock(NotificacionPort.class);
        final RealtimePublisherPort realtime = mock(RealtimePublisherPort.class);

        final EvaluadorAlertasInasistencia evaluador = config.evaluadorAlertasInasistencia(repositorio, notificaciones, realtime);
        final AvisoRevisionCancelacionService avisos = config.avisoRevisionCancelacionService(mock(RevisionCancelacionRepositoryPort.class), notificaciones, realtime);
        final ConsultarAlertasEstudianteInputPort consulta = config.consultarAlertasEstudianteInputPort(repositorio, mock(MessageCatalogPort.class));
        final EvaluarAlertasInasistenciaAutomaticoInputPort automatico = config.evaluarAlertasInasistenciaAutomaticoInputPort(repositorio, evaluador, avisos);
        final EvaluacionAlertasInasistenciaScheduler scheduler = config.evaluacionAlertasInasistenciaScheduler(automatico, Duration.ofMinutes(1));
        final ResolverAudienciaRealtimeInputPort audiencias = config.resolverAudienciaRealtimeInputPort(mock(RealtimeAudienciaQueryPort.class));

        assertInstanceOf(AvisoAlertasInasistenciaService.class, evaluador);
        assertNotNull(avisos);
        assertInstanceOf(ConsultarAlertasEstudianteUseCaseImpl.class, consulta);
        assertInstanceOf(EvaluarAlertasInasistenciaAutomaticoUseCaseImpl.class, automatico);
        assertNotNull(scheduler);
        assertInstanceOf(ResolverAudienciaRealtimeUseCaseImpl.class, audiencias);
        assertThrows(IllegalArgumentException.class, () -> config.evaluacionAlertasInasistenciaScheduler(automatico, Duration.ZERO));
        assertThrows(NullPointerException.class, () -> config.evaluadorAlertasInasistencia(null, notificaciones, realtime));
    }

    @Test
    void revision_de_cancelacion_cablea_los_cuatro_casos_de_uso_sobre_el_mismo_repositorio() {
        final RevisionCancelacionWiringConfiguration config = new RevisionCancelacionWiringConfiguration();
        final RevisionCancelacionRepositoryPort repositorio = mock(RevisionCancelacionRepositoryPort.class);
        final AvisoRevisionCancelacionService avisos = new AlertaInasistenciaWiringConfiguration()
                .avisoRevisionCancelacionService(repositorio, mock(NotificacionPort.class), mock(RealtimePublisherPort.class));

        final DecidirRevisionCancelacionInputPort decidir = config.decidirRevisionCancelacionInputPort(repositorio, avisos);
        final ResolverRevisionCancelacionInputPort resolver = config.resolverRevisionCancelacionInputPort(repositorio, avisos);
        final ConsultarRevisionesCoordinadorInputPort coordinador = config.consultarRevisionesCoordinadorInputPort(repositorio);
        final ConsultarRevisionesDecanoInputPort decano = config.consultarRevisionesDecanoInputPort(repositorio);

        assertInstanceOf(DecidirRevisionCancelacionUseCaseImpl.class, decidir);
        assertInstanceOf(ResolverRevisionCancelacionUseCaseImpl.class, resolver);
        assertInstanceOf(ConsultarRevisionesCoordinadorUseCaseImpl.class, coordinador);
        assertInstanceOf(ConsultarRevisionesDecanoUseCaseImpl.class, decano);
        assertThrows(NullPointerException.class, () -> config.consultarRevisionesDecanoInputPort(null));
    }

    @Test
    void admision_de_estudiantes_cablea_admision_y_consulta_de_programas() {
        final AdmisionEstudianteWiringConfiguration config = new AdmisionEstudianteWiringConfiguration();

        final AdmitirEstudianteUseCase admitir = config.admitirEstudianteUseCase(mock(AdmisionEstudianteCommandPort.class), mock(UsuarioRepositoryPort.class),
                mock(PasswordEncoderPort.class), mock(CorreoInstitucionalPolicy.class), mock(AsegurarCuentaIdentidadService.class),
                mock(InstitutionalScopePort.class), mock(NotificacionPort.class));
        final AdmitirEstudianteInputPort admitirPort = config.admitirEstudianteInputPort(admitir);
        final ConsultarProgramasInstitucionUseCase programas = config.consultarProgramasInstitucionUseCase(mock(ProgramaInstitucionQueryPort.class));
        final ConsultarProgramasInstitucionInputPort programasPort = config.consultarProgramasInstitucionInputPort(programas);

        assertInstanceOf(AdmitirEstudianteUseCaseImpl.class, admitir);
        assertInstanceOf(AdmitirEstudianteInteractor.class, admitirPort);
        assertInstanceOf(ConsultarProgramasInstitucionUseCaseImpl.class, programas);
        assertInstanceOf(ConsultarProgramasInstitucionInteractor.class, programasPort);
    }
}
