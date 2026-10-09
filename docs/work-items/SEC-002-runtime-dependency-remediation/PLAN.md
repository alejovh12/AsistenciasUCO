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
