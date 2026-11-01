---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-01
---

# PLAN — F1: horario semanal del grupo (backend)

## Decisión

John (2026-10-30): el **coordinador define el horario** del grupo: varias franjas, sin traslapes entre ellas. La BD publica `usp_guardar_horarios_grupo` (rama `feature/F1-horario-grupo` del repo de BD).

## Cambios

1. **Contrato HTTP:** se mantienen las mismas rutas, sin cambios en `access-catalog.json` ni en el PDP.
   - `POST /api/v1/grupos` y `PUT /api/v1/grupos/{id}` aceptan `franjas: [{dia, horaInicio, horaFin}]`.
   - `dia` admite el código (LU…DO) o el nombre, con o sin tildes. Las horas van en `HH:mm`.
   - El formato anterior `dias` + `horaInicio`/`horaFin` sigue aceptándose y aplica las mismas horas a cada día.
   - Sin campos de horario, el horario no cambia.
   - Antes, cualquier campo de horario respondía **501**. Ese 501 se retira.
2. **Aplicación:**
   - `grupo.common` (`FranjaHorarioDTO`, `FranjaHorarioDomain` y `FranjaHorarioMapper`);
   - `CrearGrupoDTO/Domain` y `ActualizarGrupoDTO/Domain` llevan `franjas` y conservan sus constructores anteriores;
   - `generarSesionesAutomaticas` sigue respondiendo 501 hasta F2 (precarga).
3. **Persistencia (`GrupoJpaRepository`):**
   - `crearGrupo` y `actualizarGrupo` ejecutan `usp_guardar_horarios_grupo` **dentro de la misma transacción**: si el horario falla, el grupo tampoco queda;
   - `consultarGrupos` agrega a cada grupo sus franjas, leídas de la entidad nueva `UvHorarioEntity` (`dbo.uv_horario`, en lotes de 1000 ids) y ordenadas de lunes a domingo.
4. **Errores:** `DbFailureClassifier` traduce `HOR_003` y `HOR_004` a `VALIDATION_ERROR` (400). `HOR_002` ya respondía 409.

## Pruebas

- `GrupoHttpMapperContractTest`: normalización de franjas, formato anterior, rechazo de horario incompleto o inválido y "sin cambio".
- `GrupoControllerWriteEndpointsTest`: `POST` con `franjas` llega al caso de uso; horas sin días responden 400.
- `GrupoJpaCommandPatternTest`: el SP de horario se ejecuta después de crear y en la misma transacción, con el JSON esperado; sin franjas no se invoca.
- `JpaQueryAdapterContractTest`: el listado trae las franjas en orden.
- `FranjaHorarioMapperTest` y `DbExceptionTranslatorTest` (`HOR_003`, `HOR_004`).

## Validación

Ver `VALIDATION.md`.
