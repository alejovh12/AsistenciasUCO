# VAL-pendientes-john (backend) - VALIDATION

Fecha: 2026-11-17. Rama `fix/VAL-pendientes-john` sobre `origin/develop` 7d9c95cc.

## Resultado

| Comando | Resultado |
| :--- | :--- |
| `./mvnw.cmd -B verify` | **NOT_RUN (bloqueado)**: Maven no resuelve `co.edu.uco:messageucolab-spring-boot-starter:0.1.3` desde GitHub Packages (`401 Unauthorized`). Esa versión entró con INT-003 (`2f69c7d2`) y nunca se descargó en este equipo; el repositorio local solo tiene la 0.1.1, con la que el proyecto no compila. Hace falta la credencial de GitHub Packages del equipo en `~/.m2/settings.xml` (no se leyó ni se tocó). |
| Pruebas nuevas | Escritas, no ejecutadas: `DbFailureClassifierPeriodoAcademicoTest` (PER_008 en siete operaciones, PER_009 en el cierre) y el conteo de `MessageUcoLabCatalogoTest` (489). Una prueba no ejecutada no es PASS. |
| `docs/testing/MATRIZ_HU_PRUEBAS_BACKEND.md` | Actualizada a mano (la prueba del clasificador cita HU096 y HU098: 5 a 7 casos, totales 100 a 102) porque Python no está instalado; el CI (`matriz_hu_pruebas.py`) la verifica. |
| Fixture `catalogo-mensajes-usuario.psv` | Dos filas agregadas a mano con el formato de `build-message-catalog.py` (código remoto `PER008`/`PER009`, título recortado a 50 caracteres); la verifica `MessageUcoLabCatalogoTest`. |

## Pendiente antes de push

Correr `./mvnw.cmd -B verify` con la credencial de GitHub Packages disponible y anotar aquí el resultado (tests, ArchUnit y JaCoCo).
Sonar: la duplicación nueva es mínima (dos constantes de enum, dos casos de `switch`, cuatro entradas de conjuntos y dos pruebas).
