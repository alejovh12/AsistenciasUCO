---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# TEST_PLAN — HU008: oferta de grupos del estudiante

## Fuentes

- [PLAN.md](PLAN.md), con las decisiones J1-J5 de John.
- [CONTRACT.md](CONTRACT.md).
- `SecurityConfig` (`/api/v1/estudiante/**`).
- `infra/security-baseline/access-catalog.json`.

## Behavioral Matrix

| ID | Requirement | Scenario | Precondition | Action | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|---|
| O-01 | J1, J3 | Oferta del estudiante | E1 en P1; P1 tiene ESP vigentes (G1, G5), ESP próxima (G2), ESP finalizada (G3) y BAS vigente (G7); P2 tiene INS vigente (G4) y ESP vigente (G6) | EXEC | filas, orden, componente | 5 filas (G1, G5, G7, G2, G4) por asignatura, inicio y código; G4 INS y G7 BAS; excluye G3 y G6 | ofrecer ESP de otro programa, perder las transversales u ofrecer períodos finalizados | DB real |
| O-02 | J1 | Cupos, horario y relación | G1: capacidad 2, 1 activa y 1 inactiva de E1; G5: E1 matriculado; G2: solicitud P de E1 | EXEC | columnas | G1: cupos 1, horario "Lunes 08:00-10:00; Martes 10:00-12:00", docente; G5: matriculado y 0 cupos; G2: PROXIMO con solicitud P | contar matrículas inactivas o perder la solicitud abierta | DB real |
| O-03 | J1 | Búsqueda y paginación | — | EXEC con búsqueda y página 2 de tamaño 1; con comodines | filas | 1 fila (G2); G5 con total 5; comodines escapados devuelven 0 | LIKE sin escape | DB real |
| O-05 | J5 | Parámetro en 0 | `OFERTA.INCLUIR_TRANSVERSALES = 0` | EXEC | filas | solo las 3 ESP de su programa | parámetro ignorado | DB real |
| O-04 | Ámbito | Docente como ejecutor; ejecutor nulo | — | EXEC | filas o error | 0 filas; `VAL_001` | oferta visible a otros roles | DB real |
| RI-01 | J4 | Aceptar en ESP (HU053) | grupo semilla con componente ESP | solicitar y aceptar | matrícula y `EstudiantePrograma` | matrícula A y fila del programa | regresión de HU053 | DB real |
| RI-02 / RI-03 | J4 | Aceptar en INS o BAS | el mismo grupo con componente INS o BAS | solicitar y aceptar | matrícula y `EstudiantePrograma` | matrícula A **sin** fila de programa | inscribir en el programa por una transversal | DB real |
| S-01..S-12 | HU053-055 | Suite existente | — | — | — | sigue en verde (regresión) | — | DB real |
| OG-01..08 | J1, J2 | Caso de uso | puertos mock | execute | dominio | búsqueda recortada; valores por defecto 1/10; búsqueda vacía como nula; P/E traducidos a PENDIENTE/EN_ESPERA; ESP/BAS/INS traducidos a tipo y `transversal`; 403 sin estudiante; 400 por página, tamaño o búsqueda; constructores; interactor | límites o traducciones mal | Unit |
| OR-01..06 | Persistencia | Repositorio | EM mock | consultarOferta | SQL, parámetros, proyección | `EXEC` con parámetros nombrados y correlación; 21 columnas mapeadas; vacío; ejecutor nulo sin DB; `DatabaseOperationException`; fechas sin java.sql; nulos seguros | columna desplazada | Unit |
| OC-01..05 | HTTP | Controlador | puerto mock | GET | status y JSON | lista y total; `tipoAsignatura` y `transversal`; búsqueda y paginación propagadas; 400; 403; constructores | — | MockMvc |
| CAT | Seguridad | Catálogo vs Spring vs `SecurityConfig` | fila nueva | `AccessCatalogConsistencyTest` | — | consistente | endpoint sin regla | Unit |
| OF-01..07, OS-01..02, MK-08 | UI | Pantalla, servicio y mock | HttpTestingController | interacción | DOM y HTTP | cupos, horario, Vigente/Próximo, etiquetas "Transversal · Institucional/Básica"; sin botón si está matriculado o tiene solicitud abierta; solicitar envía `{grupoId}`, avisa y recarga; sin doble envío; búsqueda con debounce que vuelve a la página 1; error visible; servicio sin toast global; el mock refleja la solicitud abierta | botón duplicado o etiqueta errada | Frontend unit |

## Integration Requirement

- **REAL_PROVIDER_REQUIRED:** sí para DB; no para backend ni frontend.
- **ENVIRONMENT:** SQL Server 2022 desechable (réplica del job DB Quality Gate).
- **WHY:** el ámbito, la clasificación, el parámetro y la regla de matrícula viven en la DB.

## Negative / Boundary Coverage

- Ejecutor nulo o no estudiante.
- ESP de otro programa.
- Período finalizado.
- Matrícula inactiva.
- Cupo en 0.
- Comodines en la búsqueda.
- Parámetro en 0.
- Página 0 y tamaños 0 o 101.
- Búsqueda de 51 caracteres.
- Componente desconocido o nulo.
- Error HTTP.
- Doble clic.

## Pruebas RED requeridas

- **DB:** `test_usp_consultar_oferta_grupos_estudiante.sql` (fallo esperado: SP inexistente) y `test_hu008_regla_inscripcion_programa.sql` (RI-02/03 fallan sin el JOIN: se crea la fila del programa).
- **Backend:** `ConsultarOfertaGruposUseCaseImplTest`, `OfertaGruposEstudianteJpaRepositoryTest` y `EstudianteOfertaAcademicaControllerTest` (fallo esperado: clases inexistentes).
- **Frontend:** `student-group-offer.component.spec.ts` y `oferta-academica.service.spec.ts`.
