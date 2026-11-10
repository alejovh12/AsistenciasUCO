---
status: active
type: work-item
scope: database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-10
---

# PLAN — HU018 y HU019: plan de estudios por semestres y horas de cada materia del estudiante

## Origen

Decisión de John del 2026-11-10: cerrar las dos HU del estudiante que seguían abiertas en el HU-CATALOG.

- **HU018, "Consultar estructura curricular de semestres"** estaba `[PENDIENTE]`: no existía en la interfaz.
- **HU019, "Consultar créditos y plan de estudios"** estaba `[PARCIAL]`: la tarjeta de la materia ya mostraba los créditos (CULM-001 D.9) y faltaba la barra.

La aplicación no maneja notas ni asignaturas aprobadas, así que una barra de "créditos aprobados" no se puede calcular. John definió la barra por **horas** de cada materia: las horas a las que el estudiante asistió, las horas dictadas hasta hoy y las horas programadas del grupo, con los créditos de la materia al lado.

## Clase de cambio y alcance

- **Clase:** funcionalidad nueva de solo lectura. Un endpoint nuevo y dos campos aditivos en uno existente.
- **Rutas permitidas:** `application/features/estudiante/consultarplanestudio/**`, `application/features/estudiante/consultarmaterias/**`, los puertos y proyecciones nuevos en `application/secondaryports/academic/**`, `PlanEstudioEstudianteJpaRepository`, `HorasProgramadasEstudianteJpaRepository`, `EstudiantePlanEstudioController`, `EstudianteWiringConfiguration`, `infra/security-baseline/access-catalog.json` y sus pruebas.
- **Rutas prohibidas:** `SecurityConfig` (la regla `/api/v1/estudiante/**` ya exige el rol ESTUDIANTE), el contrato OpenAPI del golden path y los objetos de BD existentes.
- **Variable principal:** que el estudiante vea su plan por semestres y, en cada materia, cuánto ha asistido frente a lo dictado y a lo programado, sin ver datos de otro estudiante.
- **No alcance:** asignaturas aprobadas, notas, prerrequisitos (HU025 está descartada) y edición del plan. Tampoco se elige un plan por cohorte: el estudiante se vincula a un programa, no a un plan.
- **DoR:** READY. La regla de cada dato está definida abajo y la BD tiene las vistas necesarias.

## Reglas

| Dato | Regla | Dónde se aplica |
|---|---|---|
| Planes que ve el estudiante | Los planes ACTIVOS (`uv_plan_estudio.estaActivoPlanEstudio = 1`) de los programas en que está inscrito (`EstudiantePrograma`) | SP |
| Asignaturas del plan | Vínculo activo (`AsignaturaPlanEstudio.estaActivo = 1`) con una asignatura activa | SP |
| Semestre sin asignaturas | Se muestra, vacío: la estructura se ve completa | SP (una fila sin asignatura) y caso de uso |
| Cursando | Matrícula inscrita (`MATRICULA/ESTADO_INSCRITA`) en un grupo de la asignatura: la misma regla de "Mis materias" | SP |
| Total de semestres | El semestre más alto del plan, la regla MAX que ya usan el coordinador y el decano | Dominio |
| Créditos y asignaturas del plan | Suma y conteo de las asignaturas que el estudiante ve | Dominio |
| Horas programadas | Sesiones del grupo que no están canceladas, ya dictadas o por dictar (`uv_cumplimiento_sesion_grupo`), con `ufn_horas_sesion` | SP |
| Horas dictadas y asistidas | Sin cambio: las del resumen por horas de CULM-001 B.2. Las dictadas son un subconjunto de las programadas | SP existente |

Privacidad: los dos SP resuelven al estudiante desde el usuario ejecutor. Ningún parámetro permite pedir datos de otro estudiante, y quien no es estudiante recibe 403 en el backend.

## Contrato

### `GET /api/v1/estudiante/plan-estudio` (nuevo, rol ESTUDIANTE)

Sin parámetros. Responde `ApiListResponse<PlanEstudioEstudianteDTO>`; `total` es el número de planes. Un estudiante sin programa o sin plan activo recibe 200 con la lista vacía.

```json
{
  "exitoso": true,
  "total": 1,
  "datos": [
    {
      "idPlanEstudio": "…", "inp": 2024, "idPrograma": "…", "nombrePrograma": "Ingenieria de Sistemas",
      "totalSemestres": 8, "totalAsignaturas": 1, "totalCreditos": 3,
      "asignaturasCursando": 1, "creditosCursando": 3,
      "semestres": [
        {
          "idSemestrePlanEstudio": "…", "numeroSemestre": 8, "totalAsignaturas": 1, "totalCreditos": 3,
          "asignaturas": [
            {
              "idAsignatura": "…", "codigo": "ARQ-402", "nombre": "Arquitectura de Software", "creditos": 3,
              "tipoAsignatura": "ESPECIFICA", "codigoCaracter": "OBL", "nombreCaracter": "Obligatoria",
              "cursando": true, "idGrupoCursando": "…", "nombreGrupoCursando": "Grupo 001 - Arq Software"
            }
          ]
        }
      ]
    }
  ]
}
```

- `tipoAsignatura`: `ESPECIFICA`, `BASICA` o `INSTITUCIONAL`, igual que en la oferta de grupos (HU008).
- `idGrupoCursando` y `nombreGrupoCursando` son `null` cuando `cursando` es `false`.
- Errores: 403 si el usuario autenticado no es un estudiante. Los errores de la BD se traducen con `DbCodeTranslator`.

### `GET /api/v1/estudiante/materias` (cambio aditivo)

Cada materia agrega:

- `horasProgramadas` (número con 2 decimales o `null`): horas de todas las sesiones no canceladas del grupo.
- `sesionesProgramadas` (entero o `null`): cuántas sesiones son.

`null` significa que no se conoce la carga horaria; nunca se sustituye por cero. Los demás campos (`indicadores`, `creditos`) no cambian.

### Seguridad y PDP

- `access-catalog.json` agrega `GET /api/v1/estudiante/plan-estudio` para ESTUDIANTE. `SecurityConfig` no cambia.
- El PDP necesita esa ruta: `catalog`, `pdp` y `verify`, sin `--prune`.

## Base de datos

Dos SP nuevos de solo lectura y sin objetos modificados: `usp_consultar_plan_estudio_estudiante` y `usp_consultar_horas_programadas_estudiante`, con la migración `33_hu018_hu019_plan_horas_estudiante`. El detalle está en el work item homónimo del repositorio de BD. Orden de despliegue: primero la migración y después el backend (DEC-060).

## Riesgos y rollback

- `GET /estudiante/materias` hace una consulta más por petición. Es una consulta por estudiante, sin paginación, igual que el resumen.
- Si el backend se despliega sin la migración 33, `GET /estudiante/materias` falla. Por eso la migración va primero.
- Rollback: revertir el commit del backend y ejecutar el `down` de la migración 33, que solo elimina los dos SP.
