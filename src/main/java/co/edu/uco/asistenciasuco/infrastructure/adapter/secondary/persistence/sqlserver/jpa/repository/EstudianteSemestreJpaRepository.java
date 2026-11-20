package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.EstudianteSemestreCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.ActualizarSemestreEstudianteCommand;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Registra el semestre actual del estudiante en el programa con usp_actualizar_semestre_estudiante_programa (migracion 63). */
@Repository
public class EstudianteSemestreJpaRepository implements EstudianteSemestreCommandPort {

    static final String SQL_ACTUALIZAR = """
            EXEC dbo.usp_actualizar_semestre_estudiante_programa
                 @idEstudiante = :idEstudiante,
                 @idPrograma = :idPrograma,
                 @semestreActual = :semestreActual,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String OP_ACTUALIZAR = "actualizarSemestreEstudiantePrograma";

    private final JpaProcedureExecutor procedureExecutor;

    public EstudianteSemestreJpaRepository(final JpaProcedureExecutor procedureExecutor) {
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    @Override
    public String actualizarSemestre(final ActualizarSemestreEstudianteCommand command) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idEstudiante", command.idEstudiante());
        parametros.put("idPrograma", command.idPrograma());
        parametros.put("semestreActual", command.semestreActual());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.usuarioEjecutor());
        return procedureExecutor.execute(OP_ACTUALIZAR, SQL_ACTUALIZAR, parametros, correlationId).getMensajeUsuarioResultado();
    }
}
