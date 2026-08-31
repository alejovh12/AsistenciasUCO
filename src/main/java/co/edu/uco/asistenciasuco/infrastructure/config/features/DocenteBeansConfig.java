package co.edu.uco.asistenciasuco.infrastructure.config.features;

import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.AsignarDocenteAGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.interactor.AsignarDocenteAGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.AsignarDocenteAGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.impl.AsignarDocenteAGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.ConsultarAsignacionesAcademicasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.interactor.ConsultarAsignacionesAcademicasDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.ConsultarAsignacionesAcademicasDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.impl.ConsultarAsignacionesAcademicasDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.ConsultarDocentesInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.interactor.ConsultarDocentesInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.ConsultarDocentesUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.impl.ConsultarDocentesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.ConsultarDocentePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.interactor.ConsultarDocentePorIdInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.ConsultarDocentePorIdUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.impl.ConsultarDocentePorIdUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.RegistrarDocenteDesdeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.interactor.RegistrarDocenteDesdeUsuarioInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.RegistrarDocenteDesdeUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.impl.RegistrarDocenteDesdeUsuarioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter.DocenteRepositorySqlServerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
public class DocenteBeansConfig {

    @Bean
    public DocenteRepositoryPort docenteRepositoryPort(final JdbcTemplate jdbcTemplate) {
        return new DocenteRepositorySqlServerAdapter(jdbcTemplate);
    }

    @Bean
    public ConsultarDocentesUseCase consultarDocentesUseCase(final DocenteRepositoryPort docenteRepositoryPort) {
        return new ConsultarDocentesUseCaseImpl(docenteRepositoryPort);
    }

    @Bean
    public ConsultarDocentesInputPort consultarDocentesInputPort(
            final ConsultarDocentesUseCase consultarDocentesUseCase
    ) {
        return new ConsultarDocentesInteractor(consultarDocentesUseCase);
    }

    @Bean
    public ConsultarDocentePorIdUseCase consultarDocentePorIdUseCase(
            final DocenteRepositoryPort docenteRepositoryPort
    ) {
        return new ConsultarDocentePorIdUseCaseImpl(docenteRepositoryPort);
    }

    @Bean
    public ConsultarDocentePorIdInputPort consultarDocentePorIdInputPort(
            final ConsultarDocentePorIdUseCase consultarDocentePorIdUseCase
    ) {
        return new ConsultarDocentePorIdInteractor(consultarDocentePorIdUseCase);
    }

    @Bean
    public ConsultarAsignacionesAcademicasDocenteUseCase consultarAsignacionesAcademicasDocenteUseCase(
            final DocenteRepositoryPort docenteRepositoryPort
    ) {
        return new ConsultarAsignacionesAcademicasDocenteUseCaseImpl(docenteRepositoryPort);
    }

    @Bean
    public ConsultarAsignacionesAcademicasDocenteInputPort consultarAsignacionesAcademicasDocenteInputPort(
            final ConsultarAsignacionesAcademicasDocenteUseCase consultarAsignacionesAcademicasDocenteUseCase
    ) {
        return new ConsultarAsignacionesAcademicasDocenteInteractor(consultarAsignacionesAcademicasDocenteUseCase);
    }

    @Bean
    public RegistrarDocenteDesdeUsuarioUseCase registrarDocenteDesdeUsuarioUseCase(
            final DocenteRepositoryPort docenteRepositoryPort
    ) {
        return new RegistrarDocenteDesdeUsuarioUseCaseImpl(docenteRepositoryPort);
    }

    @Bean
    public RegistrarDocenteDesdeUsuarioInputPort registrarDocenteDesdeUsuarioInputPort(
            final RegistrarDocenteDesdeUsuarioUseCase registrarDocenteDesdeUsuarioUseCase
    ) {
        return new RegistrarDocenteDesdeUsuarioInteractor(registrarDocenteDesdeUsuarioUseCase);
    }

    @Bean
    public AsignarDocenteAGrupoUseCase asignarDocenteAGrupoUseCase(final DocenteRepositoryPort docenteRepositoryPort) {
        return new AsignarDocenteAGrupoUseCaseImpl(docenteRepositoryPort);
    }

    @Bean
    public AsignarDocenteAGrupoInputPort asignarDocenteAGrupoInputPort(
            final AsignarDocenteAGrupoUseCase asignarDocenteAGrupoUseCase
    ) {
        return new AsignarDocenteAGrupoInteractor(asignarDocenteAGrupoUseCase);
    }
}
