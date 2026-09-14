package co.edu.uco.asistenciasuco.application.secondaryports.security;

import java.util.Optional;
import java.util.UUID;

public interface InstitutionalScopePort {

    Optional<UUID> findUsuarioIdById(UUID usuarioId);

    Optional<UUID> findUsuarioIdByEmail(String email);

    Optional<UUID> findDocenteIdByUsuario(UUID usuarioId);

    Optional<UUID> findEstudianteIdByUsuario(UUID usuarioId);

    Optional<UUID> findProgramaIdByCoordinadorUsuario(UUID usuarioId);

    Optional<UUID> findCoordinadorIdByUsuario(UUID usuarioId);

    Optional<UUID> findFacultadIdByDecanoUsuario(UUID usuarioId);

    Optional<UUID> findDecanoIdByUsuario(UUID usuarioId);

    boolean canDocenteAccessGrupo(UUID usuarioId, UUID grupoId);

    boolean canEstudianteAccessGrupo(UUID usuarioId, UUID grupoId);

    boolean canCoordinadorAccessPrograma(UUID usuarioId, UUID programaId);

    boolean canDecanoAccessFacultad(UUID usuarioId, UUID facultadId);
}
