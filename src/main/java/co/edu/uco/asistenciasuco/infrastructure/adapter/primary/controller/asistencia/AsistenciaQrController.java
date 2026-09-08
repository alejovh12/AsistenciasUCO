package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.UserScopeService;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controlador REST para toma de asistencia dinámica por código QR (HU020 - HU025).
 */
@RestController
@RequestMapping("/api/v1")
public class AsistenciaQrController {

    private static final long QR_TTL_SECONDS = 60;

    public record QrTokenEntry(UUID sesionId, UUID grupoId, Instant expiresAt, String codigoAcceso) {}

    // Caché en memoria de tokens QR efímeros y códigos de acceso alfanuméricos
    private static final Map<String, QrTokenEntry> ACTIVE_QR_TOKENS = new ConcurrentHashMap<>();
    private static final Map<String, QrTokenEntry> ACTIVE_ACCESS_CODES = new ConcurrentHashMap<>();

    private final JdbcTemplate jdbcTemplate;
    private final UserScopeService userScopeService;

    public AsistenciaQrController(final JdbcTemplate jdbcTemplate, final UserScopeService userScopeService) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate es obligatorio.");
        this.userScopeService = Objects.requireNonNull(userScopeService, "UserScopeService es obligatorio.");
    }

    /**
     * El docente genera o refresca el token dinámico de asistencia para la sesión.
     */
    @GetMapping("/sesiones/{sesionId}/qr-token")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> generarQrToken(
            @PathVariable final UUID sesionId
    ) {
        final UUID grupoId = jdbcTemplate.query(
                "SELECT grupo FROM dbo.Sesion WHERE id = ? AND cerrada = 0",
                rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                sesionId
        );

        if (grupoId == null) {
            throw new ResourceNotFoundException("Sesión no encontrada o ya se encuentra cerrada.");
        }

        // Limpiar tokens expirados
        final Instant now = Instant.now();
        ACTIVE_QR_TOKENS.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        ACTIVE_ACCESS_CODES.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));

        // Generar nuevo token efímero y PIN corto de 6 dígitos para ingreso manual
        final String token = "UCO-QR-" + UUID.randomUUID();
        final String codigoAcceso = String.format("%06d", java.util.concurrent.ThreadLocalRandom.current().nextInt(100000, 999999));
        final Instant expiresAt = now.plusSeconds(QR_TTL_SECONDS);

        final QrTokenEntry entry = new QrTokenEntry(sesionId, grupoId, expiresAt, codigoAcceso);
        ACTIVE_QR_TOKENS.put(token, entry);
        ACTIVE_ACCESS_CODES.put(codigoAcceso, entry);

        final Map<String, Object> data = Map.of(
                "sesionId", sesionId.toString(),
                "grupoId", grupoId.toString(),
                "token", token,
                "codigoAcceso", codigoAcceso,
                "expiraEnSegundos", QR_TTL_SECONDS,
                "expiraEn", expiresAt.toString()
        );

        return ResponseEntity.ok(new ApiDataResponse<>(true, data));
    }

    /**
     * El estudiante escanea el código QR o digita el código de acceso para marcar su asistencia automáticamente.
     */
    @PostMapping("/estudiante/asistencia-qr")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> registrarAsistenciaPorQr(
            @RequestBody final Map<String, String> payload
    ) {
        final String token = payload.get("token");
        final String codigoAcceso = payload.get("codigoAcceso") != null ? payload.get("codigoAcceso") : payload.get("code");

        QrTokenEntry entry = null;
        if (token != null && !token.isBlank()) {
            entry = ACTIVE_QR_TOKENS.get(token.trim());
        } else if (codigoAcceso != null && !codigoAcceso.isBlank()) {
            entry = ACTIVE_ACCESS_CODES.get(codigoAcceso.trim().toUpperCase());
        }

        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            throw new ConflictException("El código QR o PIN de acceso ha expirado o no es válido. Solicite al docente que lo actualice.");
        }

        final UUID estudianteId = resolveCurrentEstudianteId();

        // Validar que el estudiante esté matriculado en el grupo
        final UUID estudianteGrupoId = jdbcTemplate.query(
                "SELECT id FROM dbo.EstudianteGrupo WHERE estudiante = ? AND grupo = ?",
                rs -> rs.next() ? UUID.fromString(rs.getString(1)) : null,
                estudianteId,
                entry.grupoId()
        );

        if (estudianteGrupoId == null) {
            throw new ForbiddenException("No está matriculado en el grupo correspondiente a esta sesión.");
        }

        // Validar si el estudiante ya registró su asistencia para esta sesión
        final Integer countAsistenciaPrevia = jdbcTemplate.query(
                "SELECT COUNT(1) FROM dbo.Asistencia WHERE estudianteGrupo = ? AND sesion = ?",
                rs -> rs.next() ? rs.getInt(1) : 0,
                estudianteGrupoId,
                entry.sesionId()
        );

        final boolean yaRegistrado = (countAsistenciaPrevia != null && countAsistenciaPrevia > 0);

        if (!yaRegistrado) {
            jdbcTemplate.update(
                    "INSERT INTO dbo.Asistencia (id, estudianteGrupo, sesion) VALUES (NEWID(), ?, ?)",
                    estudianteGrupoId, entry.sesionId()
            );
        }

        final String mensaje = yaRegistrado
                ? "Ya habías registrado tu asistencia previamente para esta sesión."
                : "¡Asistencia registrada exitosamente!";

        final Map<String, Object> responseData = Map.of(
                "sesionId", entry.sesionId().toString(),
                "estudianteId", estudianteId.toString(),
                "estado", "PRESENTE",
                "yaRegistrado", yaRegistrado,
                "mensaje", mensaje,
                "fechaHora", LocalDateTime.now().toString()
        );

        return ResponseEntity.ok(new ApiDataResponse<>(true, responseData));
    }

    private UUID resolveCurrentEstudianteId() {
        final UUID usuarioId = userScopeService.getAuthenticatedUserId()
                .orElse(UUID.fromString("E1F2A3B4-0000-0000-0000-000000000004"));

        return userScopeService.findEstudianteIdByUsuario(usuarioId)
                .orElse(UUID.fromString("F1A2B3C4-0000-0000-0000-000000000004"));
    }
}
