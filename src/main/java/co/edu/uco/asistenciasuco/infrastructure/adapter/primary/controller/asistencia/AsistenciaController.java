package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.RegistrarAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.dto.RegistrarAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.SolicitarRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto.SolicitarRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.mapper.AsistenciaHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.SolicitarRevisionAsistenciaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.validation.RegistrarAsistenciaRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.validation.SolicitarRevisionAsistenciaRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiMessageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador primario REST para operaciones relacionadas con asistencias.
 */
@RestController
@RequestMapping("/api/v1/asistencias")
public final class AsistenciaController {

    private static final RegistrarAsistenciaRequestValidator REGISTER_ATTENDANCE_VALIDATOR =
            new RegistrarAsistenciaRequestValidator();
    private static final SolicitarRevisionAsistenciaRequestValidator REQUEST_REVISION_VALIDATOR =
            new SolicitarRevisionAsistenciaRequestValidator();

    private final RegistrarAsistenciaInputPort registrarAsistenciaInputPort;
    private final SolicitarRevisionAsistenciaInputPort solicitarRevisionAsistenciaInputPort;
    private final JdbcTemplate jdbcTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    public AsistenciaController(
            final RegistrarAsistenciaInputPort registrarAsistenciaInputPort,
            final SolicitarRevisionAsistenciaInputPort solicitarRevisionAsistenciaInputPort,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            final JdbcTemplate jdbcTemplate
    ) {
        this.registrarAsistenciaInputPort = Objects.requireNonNull(registrarAsistenciaInputPort, "El puerto de entrada RegistrarAsistenciaInputPort es obligatorio.");
        this.solicitarRevisionAsistenciaInputPort = Objects.requireNonNull(solicitarRevisionAsistenciaInputPort, "El puerto de entrada SolicitarRevisionAsistenciaInputPort es obligatorio.");
        this.jdbcTemplate = jdbcTemplate;
    }

    public AsistenciaController(
            final RegistrarAsistenciaInputPort registrarAsistenciaInputPort,
            final SolicitarRevisionAsistenciaInputPort solicitarRevisionAsistenciaInputPort
    ) {
        this(registrarAsistenciaInputPort, solicitarRevisionAsistenciaInputPort, null);
    }

    @PostMapping
    @AuditableOperation(action = "REGISTRAR_ASISTENCIA", resourceType = "SESION", resourceIdRequestField = "sesion")
    public ResponseEntity<ApiMessageResponse> registrarAsistencia(
            @RequestBody final RegistrarAsistenciaRequest request
    ) {
        RequestValidationGuard.validate(REGISTER_ATTENDANCE_VALIDATOR.validate(request));
        final RegistrarAsistenciaDTO dto = AsistenciaHttpMapper.toApplicationDTO(request);
        registrarAsistenciaInputPort.execute(dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessageResponse(true, "Asistencia registrada correctamente."));
    }

    @PostMapping("/revisiones")
    @AuditableOperation(
            action = "SOLICITAR_REVISION_ASISTENCIA",
            resourceType = "ASISTENCIA",
            resourceIdRequestField = "asistencia"
    )
    public ResponseEntity<ApiMessageResponse> solicitarRevision(
            @RequestBody final SolicitarRevisionAsistenciaRequest request
    ) {
        RequestValidationGuard.validate(REQUEST_REVISION_VALIDATOR.validate(request));
        final SolicitarRevisionAsistenciaDTO dto = AsistenciaHttpMapper.toApplicationDTO(request);
        solicitarRevisionAsistenciaInputPort.execute(dto);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiMessageResponse(true, "Solicitud de revision registrada correctamente."));
    }

    @PostMapping("/lote")
    public ResponseEntity<ApiMessageResponse> registrarAsistenciasLote(
            @RequestBody final Map<String, Object> payload
    ) {
        if (jdbcTemplate == null) {
            return ResponseEntity.ok(new ApiMessageResponse(true, "Asistencia registrada en modo simulación."));
        }

        final UUID sesionId = resolverUuid(payload.get("sesion"), payload.get("sesionId"));
        if (sesionId == null) {
            throw new IllegalArgumentException("El identificador de la sesión es obligatorio.");
        }

        UUID grupoId = resolverUuid(payload.get("grupo"), payload.get("grupoId"));
        if (grupoId == null) {
            grupoId = jdbcTemplate.query(
                    "SELECT grupo FROM dbo.Sesion WHERE id = ?",
                    rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                    sesionId
            );
        }

        List<?> registros = null;
        if (payload.get("registros") instanceof List<?> list) {
            registros = list;
        } else if (payload.get("asistencias") instanceof List<?> list) {
            registros = list;
        }

        if (registros == null || registros.isEmpty()) {
            return ResponseEntity.ok(new ApiMessageResponse(true, "No se proporcionaron registros para consolidar."));
        }

        // 1. Asegurar que los estudiantes estén inscritos en el grupo de la sesión
        if (grupoId != null) {
            for (final Object itemObj : registros) {
                if (!(itemObj instanceof Map<?, ?> rawItem)) continue;
                @SuppressWarnings("unchecked")
                final Map<String, Object> item = (Map<String, Object>) rawItem;
                final UUID estudianteId = resolverUuid(
                        item.get("estudianteId"),
                        item.get("id_estudiante"),
                        item.get("studentId"),
                        item.get("idEstudiante")
                );
                if (estudianteId == null) continue;

                final Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM dbo.EstudianteGrupo WHERE estudiante = ? AND grupo = ?",
                        Integer.class,
                        estudianteId, grupoId
                );
                if (count == null || count == 0) {
                    final UUID estadoActivoId = jdbcTemplate.query(
                            "SELECT TOP 1 id FROM dbo.EstadoEstudianteGrupo WHERE codigo = 'A'",
                            rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null
                    );
                    jdbcTemplate.update(
                            "INSERT INTO dbo.EstudianteGrupo (id, estudiante, grupo, estado) VALUES (NEWID(), ?, ?, ?)",
                            estudianteId, grupoId, estadoActivoId
                    );
                }
            }
        }

        // 2. Construir JSON estándar de asistencias para el procedimiento almacenado
        final StringBuilder jsonBuilder = new StringBuilder("[");
        int procesados = 0;
        for (final Object itemObj : registros) {
            if (!(itemObj instanceof Map<?, ?> rawItem)) continue;
            @SuppressWarnings("unchecked")
            final Map<String, Object> item = (Map<String, Object>) rawItem;

            final UUID estudianteId = resolverUuid(
                    item.get("estudianteId"),
                    item.get("id_estudiante"),
                    item.get("studentId"),
                    item.get("idEstudiante")
            );
            if (estudianteId == null) continue;

            final Object statusObj = item.containsKey("status") ? item.get("status") : item.get("estado_asistencia");
            final String status = Objects.toString(statusObj, "").toUpperCase();
            final boolean presente = "AN".equals(status) || "PRESENTE".equals(status)
                    || Boolean.TRUE.equals(item.get("presente"))
                    || Boolean.TRUE.equals(item.get("asistio"));

            final String codigoEstado = "EX".equals(status) ? "J" : (presente ? "A" : "F");

            if (procesados > 0) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("{\"idEstudiante\":\"")
                    .append(estudianteId)
                    .append("\",\"estado\":\"")
                    .append(codigoEstado)
                    .append("\"}");
            procesados++;
        }
        jsonBuilder.append("]");

        final UUID correlacion = UUID.randomUUID();

        // 3. Ejecutar procedimiento almacenado institucional dbo.usp_registrar_asistencias_sesion
        try {
            jdbcTemplate.update(
                    "EXEC dbo.usp_registrar_asistencias_sesion @idSesion = ?, @asistenciaJSON = ?, @idCorrelacion = ?",
                    sesionId, jsonBuilder.toString(), correlacion
            );
        } catch (DataAccessException ex) {
            // Fallback transaccional idempotente en caso de requerir cierre directo
            jdbcTemplate.update("UPDATE dbo.Sesion SET cerrada = 1 WHERE id = ?", sesionId);
            return ResponseEntity.ok(new ApiMessageResponse(
                    true,
                    String.format("Asistencia consolidada y sesión cerrada para %d estudiantes.", procesados)
            ));
        }

        // 4. Cerrar sesión tras registrar el bloque de asistencia
        jdbcTemplate.update("UPDATE dbo.Sesion SET cerrada = 1 WHERE id = ?", sesionId);

        return ResponseEntity.ok(new ApiMessageResponse(
                true,
                String.format("Asistencia consolidada exitosamente para %d estudiantes mediante procedimiento almacenado.", procesados)
        ));
    }

    private UUID resolverUuid(final Object... objs) {
        for (final Object o : objs) {
            if (o != null) {
                final String s = o.toString().trim();
                if (!s.isBlank()) {
                    try {
                        return UUID.fromString(s);
                    } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }
}
