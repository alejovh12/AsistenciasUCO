package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CierrePeriodoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.EstudianteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.InstitucionQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.MateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ParametroQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.AreaSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.AsignaturaDocenteSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.AsignaturaSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.CierrePeriodoSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.CoordinadorSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.DecanoSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.EstudianteProgramaSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.FacultadSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.HorarioDocenteSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.HorarioEstudianteSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.InstitucionSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.MateriaEstudianteSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.ParametroSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.PeriodoAcademicoSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.PlanEstudioSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic.SesionMateriaEstudianteSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * Composition Root: selecciona SQL Server como tecnología de persistencia para las
 * capacidades académicas (antes agrupadas artificialmente bajo "Portal", una preocupación de UI).
 *
 * <p>Cada adapter implementa únicamente los ports de UNA capability cohesionada
 * (p.ej. {@code DecanoSqlServerAdapter} implementa {@code DecanoQueryPort} y
 * {@code DecanoCommandPort}, que son la misma capability). Cuando un adapter implementa
 * más de un port se registra únicamente como bean concreto; Spring expone esa misma
 * instancia como cada port implementado.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.persistence",
        name = "provider",
        havingValue = "sqlserver",
        matchIfMissing = true
)
public class SqlServerAcademicAdapterConfiguration {

    // Decano: DecanoQueryPort + DecanoCommandPort
    @Bean
    public DecanoSqlServerAdapter decanoSqlServerAdapter(
            final NamedParameterJdbcOperations jdbcOperations,
            final CanonicalStoredProcedureExecutor procedureExecutor
    ) {
        return new DecanoSqlServerAdapter(jdbcOperations, procedureExecutor);
    }

    // Parametro: ParametroQueryPort
    @Bean
    public ParametroQueryPort parametroQueryPort(final NamedParameterJdbcOperations jdbcOperations) {
        return new ParametroSqlServerAdapter(jdbcOperations);
    }

    // Institucion: InstitucionQueryPort
    @Bean
    public InstitucionQueryPort institucionQueryPort(final NamedParameterJdbcOperations jdbcOperations) {
        return new InstitucionSqlServerAdapter(jdbcOperations);
    }

    // Facultad: FacultadQueryPort
    @Bean
    public FacultadQueryPort facultadQueryPort(final NamedParameterJdbcOperations jdbcOperations) {
        return new FacultadSqlServerAdapter(jdbcOperations);
    }

    // Area: AreaQueryPort
    @Bean
    public AreaQueryPort areaQueryPort(final NamedParameterJdbcOperations jdbcOperations) {
        return new AreaSqlServerAdapter(jdbcOperations);
    }

    // CierrePeriodo: CierrePeriodoCommandPort
    @Bean
    public CierrePeriodoCommandPort cierrePeriodoCommandPort(final CanonicalStoredProcedureExecutor procedureExecutor) {
        return new CierrePeriodoSqlServerAdapter(procedureExecutor);
    }

    // Coordinador: CoordinadorQueryPort + CoordinadorCommandPort
    @Bean
    public CoordinadorSqlServerAdapter coordinadorSqlServerAdapter(
            final NamedParameterJdbcOperations jdbcOperations,
            final CanonicalStoredProcedureExecutor procedureExecutor
    ) {
        return new CoordinadorSqlServerAdapter(jdbcOperations, procedureExecutor);
    }

    // PlanEstudio: PlanEstudioQueryPort + PlanEstudioCommandPort
    @Bean
    public PlanEstudioSqlServerAdapter planEstudioSqlServerAdapter(
            final NamedParameterJdbcOperations jdbcOperations,
            final CanonicalStoredProcedureExecutor procedureExecutor
    ) {
        return new PlanEstudioSqlServerAdapter(jdbcOperations, procedureExecutor);
    }

    // Asignatura: AsignaturaQueryPort + AsignaturaCommandPort
    @Bean
    public AsignaturaSqlServerAdapter asignaturaSqlServerAdapter(
            final NamedParameterJdbcOperations jdbcOperations,
            final CanonicalStoredProcedureExecutor procedureExecutor
    ) {
        return new AsignaturaSqlServerAdapter(jdbcOperations, procedureExecutor);
    }

    // PeriodoAcademico: PeriodoAcademicoQueryPort
    @Bean
    public PeriodoAcademicoQueryPort periodoAcademicoQueryPort(final NamedParameterJdbcOperations jdbcOperations) {
        return new PeriodoAcademicoSqlServerAdapter(jdbcOperations);
    }

    // EstudiantePrograma: EstudianteProgramaQueryPort
    @Bean
    public EstudianteProgramaQueryPort estudianteProgramaQueryPort(final NamedParameterJdbcOperations jdbcOperations) {
        return new EstudianteProgramaSqlServerAdapter(jdbcOperations);
    }

    // HorarioDocente: HorarioDocenteQueryPort
    @Bean
    public HorarioDocenteQueryPort horarioDocenteQueryPort(final NamedParameterJdbcOperations jdbcOperations) {
        return new HorarioDocenteSqlServerAdapter(jdbcOperations);
    }

    // AsignaturaDocente: AsignaturaDocenteQueryPort
    @Bean
    public AsignaturaDocenteQueryPort asignaturaDocenteQueryPort(final NamedParameterJdbcOperations jdbcOperations) {
        return new AsignaturaDocenteSqlServerAdapter(jdbcOperations);
    }

    // HorarioEstudiante: HorarioEstudianteQueryPort
    @Bean
    public HorarioEstudianteQueryPort horarioEstudianteQueryPort(final NamedParameterJdbcOperations jdbcOperations) {
        return new HorarioEstudianteSqlServerAdapter(jdbcOperations);
    }

    // MateriaEstudiante: MateriaEstudianteQueryPort
    @Bean
    public MateriaEstudianteQueryPort materiaEstudianteQueryPort(final NamedParameterJdbcOperations jdbcOperations) {
        return new MateriaEstudianteSqlServerAdapter(jdbcOperations);
    }

    // SesionMateriaEstudiante: SesionMateriaEstudianteQueryPort
    @Bean
    public SesionMateriaEstudianteQueryPort sesionMateriaEstudianteQueryPort(final NamedParameterJdbcOperations jdbcOperations) {
        return new SesionMateriaEstudianteSqlServerAdapter(jdbcOperations);
    }
}
