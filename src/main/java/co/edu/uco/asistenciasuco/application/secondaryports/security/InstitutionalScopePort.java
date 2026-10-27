package co.edu.uco.asistenciasuco.application.secondaryports.security;

import java.util.Optional;
import java.util.UUID;

public interface InstitutionalScopePort {

    Optional<UUID> findUsuarioIdById(UUID usuarioId);

    Optional<UUID> findUsuarioIdByEmail(String email);

    /** Identidad vigente; se usa antes de vincular una identidad externa al usuario institucional. */
    boolean isUsuarioActivo(UUID usuarioId);

    Optional<UUID> findDocenteIdByUsuario(UUID usuarioId);

    Optional<UUID> findEstudianteIdByUsuario(UUID usuarioId);

    Optional<UUID> findProgramaIdByCoordinadorUsuario(UUID usuarioId);

    Optional<UUID> findCoordinadorIdByUsuario(UUID usuarioId);

    Optional<UUID> findFacultadIdByDecanoUsuario(UUID usuarioId);

    Optional<UUID> findDecanoIdByUsuario(UUID usuarioId);

    boolean canDocenteAccessGrupo(UUID usuarioId, UUID grupoId);

    boolean canEstudianteAccessGrupo(UUID usuarioId, UUID grupoId);

    /** El usuario es administrador activo. */
    boolean isAdministradorActivo(UUID usuarioId);

    /** Roles institucionales activos de la persona según la base institucional (fuente de verdad). */
    java.util.Set<co.edu.uco.asistenciasuco.application.security.InstitutionalRole> findRolesActivosByUsuario(UUID usuarioId);

    /**
     * El actor puede gestionar la cuenta de acceso del destino: administrador (cualquiera), decano (coordinadores,
     * docentes y estudiantes de su facultad) o coordinador (docentes y estudiantes de su programa).
     */
    boolean canGestionarCuenta(UUID actorUsuarioId, UUID destinoUsuarioId);

    /** El usuario es coordinador activo del programa al que pertenece el grupo (vista {@code uv_grupo_programa}). */
    boolean canCoordinadorAccessGrupo(UUID usuarioId, UUID grupoId);

    /** El usuario es decano activo de la facultad a la que pertenece el programa del grupo. */
    boolean canDecanoAccessGrupo(UUID usuarioId, UUID grupoId);

    boolean canCoordinadorAccessPrograma(UUID usuarioId, UUID programaId);

    boolean canDecanoAccessFacultad(UUID usuarioId, UUID facultadId);

    /** El usuario es decano activo de la facultad a la que pertenece el programa. */
    boolean canDecanoAccessPrograma(UUID usuarioId, UUID programaId);
}
