package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic.AsignaturaSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic.CoordinadorSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic.DecanoSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic.PlanEstudioSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.procedure.CanonicalStoredProcedureExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class SqlServerAcademicAdapterConfigurationTest {

    @Test
    void academic_sqlserver_ports_no_generan_beans_duplicados_para_adapters_multiport() {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);

        new ApplicationContextRunner()
                .withPropertyValues("app.adapters.persistence.provider=sqlserver")
                .withBean(NamedParameterJdbcOperations.class, () -> jdbcOperations)
                .withBean(CanonicalStoredProcedureExecutor.class, () -> new CanonicalStoredProcedureExecutor(jdbcOperations))
                .withUserConfiguration(SqlServerAcademicAdapterConfiguration.class)
                .run(context -> {
                    assertSingleBean(context, DecanoQueryPort.class);
                    assertSingleBean(context, DecanoCommandPort.class);
                    assertSingleBean(context, CoordinadorQueryPort.class);
                    assertSingleBean(context, CoordinadorCommandPort.class);
                    assertSingleBean(context, PlanEstudioQueryPort.class);
                    assertSingleBean(context, PlanEstudioCommandPort.class);
                    assertSingleBean(context, AsignaturaQueryPort.class);
                    assertSingleBean(context, AsignaturaCommandPort.class);

                    assertSame(context.getBean(DecanoSqlServerAdapter.class), context.getBean(DecanoQueryPort.class));
                    assertSame(context.getBean(DecanoSqlServerAdapter.class), context.getBean(DecanoCommandPort.class));
                    assertSame(context.getBean(CoordinadorSqlServerAdapter.class), context.getBean(CoordinadorQueryPort.class));
                    assertSame(context.getBean(CoordinadorSqlServerAdapter.class), context.getBean(CoordinadorCommandPort.class));
                    assertSame(context.getBean(PlanEstudioSqlServerAdapter.class), context.getBean(PlanEstudioQueryPort.class));
                    assertSame(context.getBean(PlanEstudioSqlServerAdapter.class), context.getBean(PlanEstudioCommandPort.class));
                    assertSame(context.getBean(AsignaturaSqlServerAdapter.class), context.getBean(AsignaturaQueryPort.class));
                    assertSame(context.getBean(AsignaturaSqlServerAdapter.class), context.getBean(AsignaturaCommandPort.class));
                });
    }

    private <T> void assertSingleBean(final AssertableApplicationContext context, final Class<T> beanType) {
        assertEquals(1, context.getBeansOfType(beanType).size(), beanType.getSimpleName());
    }
}
