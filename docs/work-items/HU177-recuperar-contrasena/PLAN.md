# HU177 — recuperar contraseña: retiro del flujo propio

## Decisión

John, 2026-11-11: recuperar la contraseña **no es de esta aplicación**. Es del servicio de seguridad de terceros (Security Baseline / Keycloak), y la aplicación debe apoyarse en él. HU177 queda **pendiente de terceros**.

Esta decisión reemplaza la del mismo día que aprobó un flujo propio (enlace de un solo uso por correo). Ese flujo se implementó, pasó sus pruebas y estuvo unas horas en `develop` (PR #132). Se retira completo con un revert de ese merge.

## Qué se retira del backend

| Pieza | Estado |
|---|---|
| Rutas públicas `POST /api/v1/public/recuperacion-clave/solicitud` y `/confirmacion`, su controlador y sus `request` | Retiradas |
| Casos de uso `features/usuario/recuperarclave/**`, `RecuperacionClaveRepositoryPort` y `RecuperacionClaveJpaRepository` | Retirados |
| `IdentityProviderPort.establecerClave` y su implementación en `KeycloakIdentityProviderAdapter` y `ProfileSyncingIdentityProvider` | Retirados |
| `Notificacion.TIPO_RECUPERACION_CLAVE`, su plantilla por defecto y el tipo de prueba del administrador | Retirados |
| Entradas en `SecurityPublicEndpoints`, `SecurityConfig`, la lista `exempt` de `access-catalog.json` y `application.yml` (`app.recuperacion-clave.*`) | Retiradas |
| Generalizaciones hechas para ese flujo en `VerificacionCorreoMarcadorProvider`, `TokenVerificacionCorreoJpaRepository` y `VerificacionCorreoRateLimitFilter` | Revertidas: esas clases vuelven exactamente a como estaban |
| Las 38 pruebas del flujo; `JpaRepositoryBeanUniquenessTest` vuelve a 70 repositorios | Retiradas |

El backend queda como estaba antes de HU177. No hay ninguna función de credenciales propia: la aplicación no recibe, valida ni cambia contraseñas fuera del alta de cuentas que ya existía.

## Qué queda para HU177

- En esta aplicación: nada en el backend.
- En el servicio de seguridad de terceros: activar la recuperación de contraseña y su correo en Keycloak. Cuando exista, el frontend conecta el botón del login con ella.

## Dependencias

- Base de datos: migración `36_hu177_retiro_recuperacion_clave_up.sql`, que limpia los ambientes donde se aplicó la 35. Este backend no usa ninguno de esos objetos, así que el orden entre los dos despliegues no importa.
- PDP: nada que cambiar. Las dos rutas eran públicas y nunca se aprovisionaron.

## Regla que deja esta decisión

Antes de construir cualquier función de identidad o credenciales, confirmar si la cubre el componente de seguridad de terceros. Si la cubre, la aplicación solo enlaza o delega.

## Validación

| Comando | Resultado |
|---|---|
| `.\mvnw.cmd -B -o clean verify` (JDK 25), 2026-11-11 | `Tests run: 4334, Failures: 0, Errors: 0, Skipped: 0`; `All coverage checks have been met`; `BUILD SUCCESS`. Es el mismo total que tenía `develop` antes de HU177 |
| Comparación con `develop` anterior a HU177 | El código fuente, las pruebas y la configuración quedan idénticos; solo cambian tres documentos |
