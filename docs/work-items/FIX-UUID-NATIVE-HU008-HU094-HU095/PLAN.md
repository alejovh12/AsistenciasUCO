---
status: active
type: work-item
scope: backend
owner: fullstack-team
last-reviewed: 2026-10-28
---

# PLAN — FIX: UNIQUEIDENTIFIER como texto en las consultas nativas de HU008 y HU094-HU095

## Causa

- **Comportamiento del driver:** en una consulta nativa (`EXEC` de un SP leído como `Object[]`), el driver de SQL Server devuelve las columnas `UNIQUEIDENTIFIER` como `String`, no como `UUID`. Por eso un cast directo `(UUID) fila[i]` lanza `ClassCastException` y el endpoint responde **500** contra la base real.
- **Antecedente:** Alejo lo detectó en HU051 y HU053-HU055 (`BUG-HU-02` en [INTEGRACION-DEVELOP-20261027](../INTEGRACION-DEVELOP-20261027/README.md)). Lo corrigió con `NativeRowValues.uuid`, que acepta `UUID` o su texto.
- **Mismo patrón aquí:** el cast directo también estaba en los repositorios de HU094-HU095 y HU008, ya integrados en `develop`.

| Repositorio | Endpoints afectados |
|---|---|
| `DocenteProgramaReporteJpaRepository` (HU094-HU095) | `GET /api/v1/coordinador/docentes/{idDocente}/ficha` y `GET /api/v1/coordinador/docentes/resumen` |
| `OfertaGruposEstudianteJpaRepository` (HU008) | `GET /api/v1/estudiante/oferta-academica` |

**Lección aprendida:** las pruebas unitarias simulaban el `EntityManager` devolviendo objetos `UUID`, así que no reproducían lo que entrega el driver real. Toda prueba nueva de una consulta nativa debe incluir al menos un caso con el `UNIQUEIDENTIFIER` como **texto**, idealmente en mayúsculas o con espacios.

## Cambio

- Se reemplazan los 8 casts `(UUID) f[i]` por `NativeRowValues.uuid(f[i])`: 4 en cada repositorio.
- No cambia ningún contrato HTTP ni de la DB.
- **Pruebas nuevas:**
  - `HR07_RR04` en `DocenteProgramaReporteJpaRepositoryTest`: historial y consolidado con ids como texto.
  - `OR07` en `OfertaGruposEstudianteJpaRepositoryTest`: grupo y solicitud abierta con ids como texto.
- HU006 (`PerfilUsuarioJpaRepository`) recibe el mismo arreglo en su propia rama `feature/HU006-perfil-usuario`, con la prueba `PR06`.

## Rollback

Revertir el commit. No hay cambios de datos ni de contratos.
