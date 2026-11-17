package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.ConsultarAsignaturasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.interactor.ConsultarAsignaturasDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.ConsultarAsignaturasDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.impl.ConsultarAsignaturasDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.ConsultarDetalleReclamoDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.interactor.ConsultarDetalleReclamoDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.ConsultarDetalleReclamoDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.impl.ConsultarDetalleReclamoDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.ConsultarPadronGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.interactor.ConsultarPadronGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.ConsultarPadronGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.impl.ConsultarPadronGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.PadronGrupoQueryPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.ConsultarReclamosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.interactor.ConsultarReclamosDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.ConsultarReclamosDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.impl.ConsultarReclamosDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudRevisionDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.ConsultarHorariosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.interactor.ConsultarHorariosDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.ConsultarHorariosDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.impl.ConsultarHorariosDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.RetiroEstudianteGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.RetirarEstudianteGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.interactor.RetirarEstudianteGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.RetirarEstudianteGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.impl.RetirarEstudianteGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultarmatrizasistencia.primaryports.ConsultarMatrizAsistenciaGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarmatrizasistencia.usecase.impl.ConsultarMatrizAsistenciaGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumengrupos.primaryports.ConsultarResumenGruposDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumengrupos.usecase.impl.ConsultarResumenGruposDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumensesiones.primaryports.ConsultarResumenSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumensesiones.usecase.impl.ConsultarResumenSesionesGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatrizAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenGruposDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenSesionesGrupoQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root (wiring) para el feature Docente: ensambla UseCase + Interactor a partir de
 * los Application Ports, sin conocer ninguna tecnologia concreta.
 *
 * <p>Consolida lo que antes eran dos clases distintas ({@code DocenteBeansConfig} y
 * {@code DocenteFeatureConfiguration}) sin cambiar ningun bean: mismos nombres, mismos tipos,
 * mismas dependencias.</p>
 */
@Configuration(proxyBeanMethods = false)
public class DocenteWiringConfiguration {

    @Bean
    ConsultarReclamosDocenteUseCase consultarReclamosDocenteUseCase(
            final InstitutionalScopePort scopePort,
            final SolicitudRevisionDocenteQueryPort queryPort
    ) {
        return new ConsultarReclamosDocenteUseCaseImpl(scopePort, queryPort);
    }

    @Bean
    ConsultarReclamosDocenteInputPort consultarReclamosDocenteInputPort(final ConsultarReclamosDocenteUseCase useCase) {
        return new ConsultarReclamosDocenteInteractor(useCase);
    }

    @Bean
    ConsultarPadronGrupoUseCase consultarPadronGrupoUseCase(
            final InstitutionalScopePort scopePort,
            final PadronGrupoQueryPort queryPort
    ) {
        return new ConsultarPadronGrupoUseCaseImpl(scopePort, queryPort);
    }

    @Bean
    ConsultarPadronGrupoInputPort consultarPadronGrupoInputPort(final ConsultarPadronGrupoUseCase useCase) {
        return new ConsultarPadronGrupoInteractor(useCase);
    }

    @Bean
    ConsultarDetalleReclamoDocenteUseCase consultarDetalleReclamoDocenteUseCase(
            final InstitutionalScopePort scopePort,
            final SolicitudRevisionDocenteQueryPort queryPort
    ) {
        return new ConsultarDetalleReclamoDocenteUseCaseImpl(scopePort, queryPort);
    }

    @Bean
    ConsultarDetalleReclamoDocenteInputPort consultarDetalleReclamoDocenteInputPort(
            final ConsultarDetalleReclamoDocenteUseCase useCase
    ) {
        return new ConsultarDetalleReclamoDocenteInteractor(useCase);
    }

    @Bean
    ConsultarHorariosDocenteUseCase consultarHorariosDocenteUseCase(final InstitutionalScopePort scopePort, final HorarioDocenteQueryPort queryPort) {
        return new ConsultarHorariosDocenteUseCaseImpl(scopePort, queryPort);
    }

    @Bean
    ConsultarHorariosDocenteInputPort consultarHorariosDocenteInputPort(final ConsultarHorariosDocenteUseCase useCase) {
        return new ConsultarHorariosDocenteInteractor(useCase);
    }

    @Bean
    ConsultarAsignaturasDocenteUseCase consultarAsignaturasDocenteUseCase(final InstitutionalScopePort scopePort, final AsignaturaDocenteQueryPort queryPort) {
        return new ConsultarAsignaturasDocenteUseCaseImpl(scopePort, queryPort);
    }

    @Bean
    ConsultarAsignaturasDocenteInputPort consultarAsignaturasDocenteInputPort(final ConsultarAsignaturasDocenteUseCase useCase) {
        return new ConsultarAsignaturasDocenteInteractor(useCase);
    }

    @Bean
    RetirarEstudianteGrupoUseCase retirarEstudianteGrupoUseCase(final InstitutionalScopePort scopePort, final RetiroEstudianteGrupoPort retiroPort, final NotificacionPort notificacionPort) {
        return new RetirarEstudianteGrupoUseCaseImpl(scopePort, retiroPort, notificacionPort);
    }

    @Bean
    RetirarEstudianteGrupoInputPort retirarEstudianteGrupoInputPort(final RetirarEstudianteGrupoUseCase useCase) {
        return new RetirarEstudianteGrupoInteractor(useCase);
    }

    /** CIERRE-W3M (punto 1): matriz de asistencia del grupo cruzada en la base, paginada por estudiante. */
    @Bean
    ConsultarMatrizAsistenciaGrupoInputPort consultarMatrizAsistenciaGrupoInputPort(
            final InstitutionalScopePort scopePort,
            final MatrizAsistenciaQueryPort matrizPort,
            final ResumenSesionesGrupoQueryPort sesionesPort
    ) {
        return new ConsultarMatrizAsistenciaGrupoUseCaseImpl(scopePort, matrizPort, sesionesPort);
    }

    /** CIERRE-W3M (puntos 2 y 3): resumen de sesiones del grupo con estado temporal y conteos resueltos en la base. */
    @Bean
    ConsultarResumenSesionesGrupoInputPort consultarResumenSesionesGrupoInputPort(
            final InstitutionalScopePort scopePort,
            final ResumenSesionesGrupoQueryPort sesionesPort
    ) {
        return new ConsultarResumenSesionesGrupoUseCaseImpl(scopePort, sesionesPort);
    }

    /** CIERRE-W3I: totales de los grupos habilitados del docente (grupos, estudiantes activos, cupo y ocupacion) resueltos en la base. */
    @Bean
    ConsultarResumenGruposDocenteInputPort consultarResumenGruposDocenteInputPort(final ResumenGruposDocenteQueryPort resumenPort) {
        return new ConsultarResumenGruposDocenteUseCaseImpl(resumenPort);
    }
}
