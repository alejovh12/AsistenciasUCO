---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CONTRACT — HU006: perfil propio

## 1. HTTP

`GET /api/v1/usuarios/perfil`

- **Acceso:** usuario autenticado (`AUTHENTICATED`, los 5 roles). Sin parámetros.
- **200:** `ApiDataResponse<PerfilUsuarioDTO>`

```json
{
  "exitoso": true,
  "datos": {
    "idUsuario": "uuid", "codigoTipoIdentificacion": "CC", "nombreTipoIdentificacion": "Cédula de ciudadanía",
    "numeroIdentificacion": 1017000001, "primerNombre": "Ana", "segundoNombre": null,
    "primerApellido": "Pérez", "segundoApellido": null, "nombreCompleto": "Ana Pérez",
    "correo": "ana@uco.edu.co", "correoConfirmado": true, "estaActivo": true,
    "vinculos": [
      { "rol": "ESTUDIANTE|DOCENTE|COORDINADOR|DECANO|ADMINISTRADOR", "idEntidad": "uuid",
        "nombreEntidad": "Ingeniería de Sistemas", "detalle": "Facultad de Ingeniería", "estaActivo": true }
    ]
  }
}
```

| Situación | HTTP | Origen |
|---|---|---|
| Sin autenticación | 401 | Seguridad |
| Usuario institucional inexistente | 404 | Application (`ResourceNotFoundException`) |

`PUT /api/v1/usuarios/perfil` sigue respondiendo 501 (TD-049 parcial).

## 2. Base de datos

### `dbo.usp_consultar_perfil_usuario` (READ_QUERY_RESULTSET)

- **Parámetros:** `@idCorrelacion`, `@idUsuarioEjecutor`. Si el ejecutor es nulo, lanza `THROW` con `VAL_001`.
- **Columnas**, en este orden:
  - `idUsuario`, `codigoTipoIdentificacion`, `nombreTipoIdentificacion`, `numeroIdentificacion`;
  - `primerNombre`, `segundoNombre`, `primerApellido`, `segundoApellido`, `nombreCompleto`;
  - `correo`, `correoConfirmado`, `estaActivoUsuario`;
  - `rolVinculo`, `idEntidadVinculo`, `nombreEntidadVinculo`, `detalleVinculo`, `estaActivoVinculo`.
- **Vínculos por rol:**

  | Rol | Entidad | Detalle | Activo |
  |---|---|---|---|
  | ESTUDIANTE | programa (`EstudiantePrograma`) | facultad | `Programa.estado` |
  | DOCENTE | programa (`DocentePrograma`) | tipo de vinculación | `DocentePrograma.estado` |
  | COORDINADOR | programa (`Programa.coordinador`) | facultad | `Programa.estado` |
  | DECANO | facultad (`Facultad.decano`) | institución | `Facultad.estado` |
  | ADMINISTRADOR | institución (`Administrador.institucion`) | NULL | `Institucion.estado` |

- **Orden:** rol en ese mismo orden, luego nombre de la entidad.
- **Sin vínculos:** 1 fila con las columnas de vínculo en NULL.
- **Usuario inexistente:** 0 filas.

## 3. Puertos de Application

`PerfilUsuarioQueryPort.consultarPerfil(usuarioEjecutor)` devuelve `Optional<PerfilUsuarioProjection>`, con la lista de `VinculoUsuarioProjection`.

## 4. Compatibilidad

- El GET pasa de 501 a 200.
- Los campos que el frontend ya leía con `AuthService#fetchProfileFromBackend` (`primerNombre`, `segundoNombre`, `primerApellido`, `segundoApellido`, `numeroIdentificacion`, `correo`) se devuelven con esos mismos nombres.
