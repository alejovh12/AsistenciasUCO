# MEJORAS-SERGIO-2026-11-18 — Validación del backend

Fecha: 2026-11-18. Equipo local (Windows 10, 8 GB de RAM, JDK 25.0.5). Rama `feat/mejoras-sergio-2026-11-18`.

## Bloqueo de dependencias

`messageucolab-spring-boot-starter:0.1.3` (GitHub Packages) responde 401 en este equipo y no está en `~/.m2`. Solo está la
0.1.1. Con el `pom.xml` real, `mvnw -o` falla al resolver dependencias. **El build completo (`mvnw verify`) queda para el CI del
PR**, por decisión de John: lo que el CI pueda correr se corre allá.

## Ejecución local dirigida

Se usó un pom temporal, sin versionar y borrado al terminar, con el starter en 0.1.1. Ese pom excluye las 2 clases de INT-003 que
necesitan la 0.1.3 (`MessageUcoLabServiceIdentityTokenProvider` y `MessageUcoLabSincronizacionConfiguration`) y sus 2 pruebas.
No las cambia esta rama.

| Comando | Resultado |
|---|---|
| `mvnw -o -f <pom temporal> test -Dtest=<26 clases>` | **BUILD SUCCESS: 260 pruebas, 0 fallos, 0 errores**. Compilan todo `src/main` y `src/test` (salvo las 4 clases excluidas). |

Clases ejecutadas: todas las pruebas que cambia esta rama (directorio, vincular, actualizar, cambiar estado, ficha y resumen del
docente; validadores; controladores del coordinador y del decano; repositorios JPA; wiring; `RbacSecurityFilterChainTest`;
`MessageUcoLabCatalogoTest`; `JpaRepositoryBeanUniquenessTest`; `AltaRequestLengthParityTest`; `DbExceptionTranslatorDocenteProgramaTest`).
También las de los ítems 2 y 10: `AprobarSolicitudSesionUseCaseImplTest`, `CancelarSesionUseCaseImplTest` y `DocentePortalControllerTest`.

## Pendiente en el CI

- `mvnw verify` completo (unitarias, integración con `-Pintegration`, JaCoCo) con acceso a GitHub Packages.
