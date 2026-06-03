package co.edu.uco.asistenciasuco.infrastructure.config;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.ConsultarAsistenciasPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.interactor.ConsultarAsistenciasPorGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.ConsultarAsistenciasPorGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.impl.ConsultarAsistenciasPorGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.RegistrarAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.interactor.RegistrarAsistenciaInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.RegistrarAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.impl.RegistrarAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.SolicitarRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.interactor.SolicitarRevisionAsistenciaInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.SolicitarRevisionAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.impl.SolicitarRevisionAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.RegistrarEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.interactor.RegistrarEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.RegistrarEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.impl.RegistrarEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.AsistenciaRepositoryMockAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.GrupoRepositoryMockAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado manual de dependencias para las features activas.
 */
@Configuration(proxyBeanMethods = false)
public class BeansConfig {

    @Bean
    public AsistenciaRepositoryPort asistenciaRepositoryPort() {
        return new AsistenciaRepositoryMockAdapter();
    }

    @Bean
    public RegistrarAsistenciaUseCase registrarAsistenciaUseCase(final AsistenciaRepositoryPort asistenciaRepositoryPort) {
        return new RegistrarAsistenciaUseCaseImpl(asistenciaRepositoryPort);
    }

    @Bean
    public RegistrarAsistenciaInputPort registrarAsistenciaInputPort(
            final RegistrarAsistenciaUseCase registrarAsistenciaUseCase
    ) {
        return new RegistrarAsistenciaInteractor(registrarAsistenciaUseCase);
    }

    @Bean
    public ConsultarAsistenciasPorGrupoUseCase consultarAsistenciasPorGrupoUseCase(
            final AsistenciaRepositoryPort asistenciaRepositoryPort
    ) {
        return new ConsultarAsistenciasPorGrupoUseCaseImpl(asistenciaRepositoryPort);
    }

    @Bean
    public ConsultarAsistenciasPorGrupoInputPort consultarAsistenciasPorGrupoInputPort(
            final ConsultarAsistenciasPorGrupoUseCase consultarAsistenciasPorGrupoUseCase
    ) {
        return new ConsultarAsistenciasPorGrupoInteractor(consultarAsistenciasPorGrupoUseCase);
    }

    @Bean
    public SolicitarRevisionAsistenciaUseCase solicitarRevisionAsistenciaUseCase(
            final AsistenciaRepositoryPort asistenciaRepositoryPort
    ) {
        return new SolicitarRevisionAsistenciaUseCaseImpl(asistenciaRepositoryPort);
    }

    @Bean
    public SolicitarRevisionAsistenciaInputPort solicitarRevisionAsistenciaInputPort(
            final SolicitarRevisionAsistenciaUseCase solicitarRevisionAsistenciaUseCase
    ) {
        return new SolicitarRevisionAsistenciaInteractor(solicitarRevisionAsistenciaUseCase);
    }

    @Bean
    public GrupoRepositoryPort grupoRepositoryPort() {
        return new GrupoRepositoryMockAdapter();
    }

    @Bean
    public RegistrarEstudianteUseCase registrarEstudianteUseCase(final GrupoRepositoryPort grupoRepositoryPort) {
        return new RegistrarEstudianteUseCaseImpl(grupoRepositoryPort);
    }

    @Bean
    public RegistrarEstudianteInputPort registrarEstudianteInputPort(
            final RegistrarEstudianteUseCase registrarEstudianteUseCase
    ) {
        return new RegistrarEstudianteInteractor(registrarEstudianteUseCase);
    }
}
