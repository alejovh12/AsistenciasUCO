---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-08
---

# SEC-002 — parches de dependencias runtime en Spring Boot 4.0.x

## Motivo y contrato de alcance

Origen: PR #16 Trivy Deep Security Scan [run 37869776047](https://github.com/alejovh12/AsistenciasUCO/actions/runs/37869776047), SHA `275d828fcca735feba20686f9a950e89cf74d362`. Trivy repository y image ambos COMPLETARON y sus reportes SARIF están en artifacts `11588914959` y `11590120454`. El workflow usa `exit-code:0` para inventario; PASSED no equivale a CVE=0. Baseline imagen: 125 registros (10 CRITICAL, 49 HIGH, 60 MEDIUM, 6 LOW), 104 ruleIds distintos; algunos CVE en varios componentes. POM Maven propio: Spring Boot parent `4.0.6`.

Esta microfase propone **parches mínimos** en `pom.xml`, SIN cambiar código Java, SQL, Angular, Dockerfile, OpenAPI ni contratos. No se afirma ausencia de CVE hasta inspeccionar imagen nueva generada con el POM modificado y validar Java25, ArchUnit, JaCoCo, CodeQL/Dependency Review, MinIO/SQL/ClamAV IT si aplican.

## Baseline CRITICAL imagen (identificadores observados en SARIF)

| Componente instalado | CVE críticos | Versión corregida observada en scan |
|---|---|---|
| `org.apache.tomcat.embed:tomcat-embed-core:11.0.21` | CVE-2026-41293, -43512, -43515, -65182, -65905, -68525 | Tomcat 11.0.25+ |
| `org.springframework:spring-webmvc:7.0.7` | CVE-2026-47884, -47890 | Spring Framework 7.0.9+ |
| `io.netty:netty-handler:4.2.12.Final` | CVE-2026-75595 | Netty 4.2.17.Final+ |
| `org.bouncycastle:bcprov-jdk18on:1.84` | CVE-2026-8763 | BC 1.85+ |

**Registros detectados no implican explotación**; evaluar reachability y advisories proveedor para cada CVE.

## Cambio 1 permitido: BOM y contenedor (este PR)

- `spring-boot-starter-parent 4.0.6 → 4.0.8`: release estable agosto 2026, preserva rama minor y compatibilidad Java25. Dependency appendix de Boot 4.0.8 administra Framework `7.0.9` y Netty `4.2.17.Final`.
- `tomcat.version 11.0.26`: el BOM Boot 4.0.8 administra `11.0.24`, debajo de fixed `11.0.25`. Override único de la **familia** Tomcat (no cada módulo) hasta próxima publicación de BOM que lo incluya. Tomcat 11.0.26 publicó en septiembre 2026.
- No cambiar `jackson-*`, `bcprov` ni librerías transitivas arbitrariamente en este lote; pendientes explicitados abajo.

## Plan de validación / DoR

DoR: APPROVED_SCOPE_POM_ONLY por solicitud de corrección de vulnerabilidades, release documentadas. Tests existentes son la regresión de comportamiento; este lote no introduce funcionalidad ni requiere nuevos RED unit. **NO validado aún con Maven** desde la herramienta que escribió este POM, cuyo entorno sólo tiene JDK21; Java25 se certificará en GitHub Actions.

1. Ejecutar `./mvnw -B -ntp clean verify` con JDK25 (Surefire + ArchUnit + JaCoCo LINE >=80%, BRANCH >=70%, OpenAPI).
2. Ejecutar `./mvnw -B -ntp dependency:tree` y guardar versiones efectivas del runtime: Spring Framework, Tomcat, Netty, Jackson, BouncyCastle.
3. Ejecutar `./mvnw -B -ntp clean verify -Pintegration` con SQL Server/MinIO/ClamAV si ambiente disponible; no convertir skip en PASS.
4. Construir `docker build` del SHA y rescaneo Trivy filesystem/image; comparar CVE y fixedVersion, separar OS y Java; no usar el reporte del PR16 como post-fix.
5. Aprobar merge sólo si checks remotos verdes, diff de dependencyTree esperado y sin regresiones; PR no se fusiona automáticamente.

## Pendientes explícitos posteriores

- `org.bouncycastle:bcprov-jdk18on 1.84`: salto a 1.85+ requiere verificar `dependency:tree`, quién la incluye (MinIO/SDK/otros), compatibilidad e integración TLS/certificados. **CVE-2026-8763 NO cerrado por este PR**.
- Jackson 2.x/3.x: existen varios HIGH con fixed versions diversas; evitar override por módulo que cree classpath mixto, estudiar BOM alineados o Boot update adicional.
- Tomcat/Netty/Spring: comprobar en Trivy nuevos CVEs o false positives; no inferir remediación completa desde POM.
- P1/P2 mantenibilidad pertenece a MAINT-001, en PR separado para no mezclar refactor con actualización de dependencias.

## Adenda correctiva — CVE restantes después del primer parche (HEAD 6d2d4b3)

Se inspeccionaron SARIF de [run 37872449097](https://github.com/alejovh12/AsistenciasUCO/actions/runs/37872449097) (Docker image artifact 11589969593, filesystem artifact 11591205030). Comparado con imagen previa: **125 → 24 registros**; **CRITICAL 10 → 1**; HIGH **49 → 12**; MEDIUM 11. Conteos de hallazgos en imagen (no explotabilidad). Nuevo baseline detectado:
- `org.bouncycastle:bcprov-jdk18on:1.84`: CVE-2026-8763 CRITICAL y CVE-2026-13506 HIGH (fixed 1.85). Usar un solo BOM `org.bouncycastle:bc-jdk18on-bom:1.85.2`.
- Jackson 2.x `2.21.5`: 5 HIGH, 2 MEDIUM en jackson-core/databind; fix `2.21.7`.
- Jackson 3.x `3.1.5`: 5 HIGH, 2 MEDIUM; fix `3.1.7`.
- `mssql-jdbc:13.2.1` (sin sufijo): CVE-2025-59250 HIGH en escáner, fixed `13.2.1.jre11` según SARIF; comprobar equivalencia de versión y advisory antes de cualquier override.
- `OpenTelemetry:1.55.0` MEDIUM (dos paquetes), `commons-compress:1.25.0` MEDIUM (dos CVE), `poi-ooxml:5.2.5` MEDIUM; pendientes en siguiente lote con origen transitivo/versión validada.
- Imagen Alpine: `libpng 1.6.58-r1` MEDIUM, `zlib 1.3.2-r0` MEDIUM; requieren parche de image base o paquetes, con nuevo Docker build.
- Trivy FS misconfig `DS-0002` HIGH: Dockerfile en stage final sin USER. Se añade `USER 10001:10001` tras COPY; no cambiar usuario del stage build. `DS-0026` LOW HEALTHCHECK pendiente de decisión (Kubernetes/Compose también puede definir healthcheck).

### Implementación del segundo lote
- Properties de Spring Boot parent: `jackson-2-bom.version=2.21.7`, `jackson-bom.version=3.1.7`. Actualiza las familias completas y evita versionado individual de módulos.
- Import de `org.bouncycastle:bc-jdk18on-bom:1.85.2` bajo dependencyManagement del módulo, sin nueva dependencia runtime directa.
- `USER 10001:10001` en imagen productiva, con UID/GID numéricos estables. No prometer compatibilidad con escrituras en `/app` hasta validar que la aplicación no intenta persistir allí.
- No se cambia código Java, esquema DB, frontend, rutas HTTP ni umbrales de Trivy.

### Gates exigidos
Maven JDK25 clean verify, ArchUnit, CI Sonar, Security y **nuevo Trivy image/fs** del SHA publicado. Revisar que efectivamente aparecen Jackson `2.21.7/3.1.7` y Bouncy Castle `1.85.2` en dependency:tree y Trivy. Comprobar `docker inspect --format='{{.Config.User}}'` y startup con UID sin privilegios. Las pruebas integradas SQL/MinIO/ClamAV quedan pendientes hasta ambiente real. No declarar DONE hasta nuevo escaneo y prueba de arranque.
