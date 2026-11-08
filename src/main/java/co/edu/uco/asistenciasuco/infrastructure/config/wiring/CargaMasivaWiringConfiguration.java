package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.CrearDecanoUseCase;
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
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.cargas.CsvCargaUsuariosParser;
import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/**
 * Composition Root para el modulo de Carga Masiva por CSV (CULM-001 D.5, CONTRACT §6, HU171).
 */
@Configuration(proxyBeanMethods = false)
public class CargaMasivaWiringConfiguration {

    @Bean
    public CsvCargaUsuariosParserPort csvCargaUsuariosParserPort() {
        return new CsvCargaUsuariosParser();
    }

    @Bean
    public ValidarCargaUsuariosUseCase validarCargaUsuariosUseCase(
            final CargaUsuariosValidacionPort validacionPort,
            final CsvCargaUsuariosParserPort parserPort
    ) {
        return new ValidarCargaUsuariosUseCaseImpl(validacionPort, parserPort);
    }

    @Bean
    public ValidarCargaUsuariosInputPort validarCargaUsuariosInputPort(
            final ValidarCargaUsuariosUseCase useCase
    ) {
        return new ValidarCargaUsuariosInteractor(useCase);
    }

    @Bean
    public EjecutarCargaUsuariosUseCase ejecutarCargaUsuariosUseCase(
            final CargaUsuariosValidacionPort validacionPort,
            final CsvCargaUsuariosParserPort parserPort,
            final VincularDocenteProgramaUseCase vincularDocenteUseCase,
            final CrearCoordinadorUseCase crearCoordinadorUseCase,
            final CrearDecanoUseCase crearDecanoUseCase,
            final AsegurarCuentaIdentidadService asegurarCuentaService
    ) {
        return new EjecutarCargaUsuariosUseCaseImpl(
                validacionPort,
                parserPort,
                vincularDocenteUseCase,
                crearCoordinadorUseCase,
                crearDecanoUseCase,
                asegurarCuentaService
        );
    }

    @Bean
    public EjecutarCargaUsuariosInputPort ejecutarCargaUsuariosInputPort(
            final EjecutarCargaUsuariosUseCase useCase
    ) {
        return new EjecutarCargaUsuariosInteractor(useCase);
    }

    @Bean
    public SlidingWindowRateLimiter cargaMasivaRateLimiter(
            @Value("${app.rate-limit.cargas.max-claves:5000}") final int maxClaves
    ) {
        return new SlidingWindowRateLimiter(Duration.ofSeconds(60), maxClaves, Clock.systemUTC());
    }
}
