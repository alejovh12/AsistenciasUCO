package co.edu.uco.asistenciasuco.infrastructure.config;

import co.edu.uco.asistenciasuco.application.secondaryports.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.AsistenciaRepositoryMockAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.SesionRepositoryMockAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.features.AsistenciaBeansConfig;
import co.edu.uco.asistenciasuco.infrastructure.config.features.SesionBeansConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MockFeatureProfileTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SesionBeansConfig.class, AsistenciaBeansConfig.class);

    @Test
    void default_profile_no_carga_mocks_de_sesion_ni_asistencia() {
        contextRunner.run(context -> {
            assertFalse(context.containsBean("sesionRepositoryPort"));
            assertFalse(context.containsBean("asistenciaRepositoryPort"));
            assertFalse(context.getBeansOfType(SesionRepositoryPort.class).containsKey("sesionRepositoryPort"));
            assertFalse(context.getBeansOfType(AsistenciaRepositoryPort.class).containsKey("asistenciaRepositoryPort"));
        });
    }

    @Test
    void mock_profile_carga_mocks_temporales_de_sesion_y_asistencia() {
        contextRunner
                .withPropertyValues("spring.profiles.active=mock")
                .run(context -> {
                    assertInstanceOf(SesionRepositoryMockAdapter.class, context.getBean(SesionRepositoryPort.class));
                    assertInstanceOf(
                            AsistenciaRepositoryMockAdapter.class,
                            context.getBean(AsistenciaRepositoryPort.class)
                    );
                });
    }
}
