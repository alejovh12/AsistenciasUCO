// VAL-oleada3 (REA-01 / REA-06 / F12-08): prueba de carga k6 de las lecturas principales de AsistenciasUCO.
//
// Perfil realista por rol (mezcla de usuarios concurrentes): estudiante 50 %, docente 25 %, coordinador 25 %.
// Cada iteración recorre las pantallas de inicio del rol con una pausa de "lectura" (think time) entre peticiones.
// Las peticiones van con Bearer real (PKCE del lab), pasan por Spring Security -> PEP -> PDP -> SQL Server.
//
// Variables (k6 -e):
//   BASE_URL      URL del backend o del balanceador (por defecto http://host.docker.internal:8080)
//   TOKENS_FILE   JSON {ROL: token} generado por scripts/load/get_tokens.py (por defecto /work/tokens.json)
//   VUS           usuarios virtuales en meseta (por defecto 20)
//   DURATION      duración de la meseta (por defecto 2m)
//   RAMP          duración de la rampa de subida (por defecto 20s)
//   THINK_MS      pausa media entre peticiones en ms (por defecto 1000)
//   MODE          normal | dependencia-caida
//                 normal: objetivos REA-01 (p95 y tasa de error).
//                 dependencia-caida: el PEP está detenido; se espera 503 ACCESS_DECISION_UNAVAILABLE (fallo cerrado y
//                 rápido), nunca 200 ni 5xx distintos de 503.
//   SUMMARY_FILE  ruta del resumen JSON (por defecto /work/out/summary.json)
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const TOKENS = JSON.parse(open(__ENV.TOKENS_FILE || '/work/tokens.json'));
const VUS = parseInt(__ENV.VUS || '20', 10);
const DURATION = __ENV.DURATION || '2m';
const RAMP = __ENV.RAMP || '20s';
const THINK_MS = parseInt(__ENV.THINK_MS || '1000', 10);
const MODE = __ENV.MODE || 'normal';

// Objetivos REA-01 (ver docs/work-items/VAL-oleada3-carga/PLAN.md). Medidos en el cliente, extremo a extremo.
const OBJETIVOS = {
  // 1) Lecturas propias de estudiante y docente (pantallas de inicio, listados paginados).
  lecturaPropia: 'p(95)<800',
  // 2) Agregados de coordinación (indicadores, consolidado, alertas): consultas sobre vistas agregadas.
  agregado: 'p(95)<1500',
  // 3) Disponibilidad bajo carga: menos del 1 % de errores.
  errores: 'rate<0.01',
};

const ESCENARIOS = {
  ESTUDIANTE: [
    ['/api/v1/auth/me', 'lecturaPropia'],
    ['/api/v1/estudiante/horarios', 'lecturaPropia'],
    ['/api/v1/estudiante/materias', 'lecturaPropia'],
    ['/api/v1/estudiante/alertas', 'lecturaPropia'],
    ['/api/v1/estudiante/reclamos?page=0&size=20', 'lecturaPropia'],
  ],
  DOCENTE: [
    ['/api/v1/auth/me', 'lecturaPropia'],
    ['/api/v1/docente/horarios', 'lecturaPropia'],
    ['/api/v1/docente/asignaturas', 'lecturaPropia'],
    ['/api/v1/docente/reclamos?page=0&size=20', 'lecturaPropia'],
  ],
  COORDINADOR: [
    ['/api/v1/auth/me', 'lecturaPropia'],
    ['/api/v1/coordinador/indicadores', 'agregado'],
    ['/api/v1/coordinador/grupos?page=0&size=20', 'agregado'],
    ['/api/v1/coordinador/consolidado-asistencia', 'agregado'],
    ['/api/v1/coordinador/alertas-tempranas', 'agregado'],
  ],
};

const esperado503 = new Rate('fallo_cerrado_503');
// Distribución de códigos de respuesta (0 = timeout o error de red del cliente).
const ESTADOS = ['200', '401', '403', '404', '429', '500', '502', '503', '504', '0'];
const porEstado = Object.fromEntries(ESTADOS.map((e) => [e, new Counter(`estado_${e}`)]));
const otroEstado = new Counter('estado_otro');

function stages(fraccion) {
  const objetivo = Math.max(1, Math.round(VUS * fraccion));
  return [
    { duration: RAMP, target: objetivo },
    { duration: DURATION, target: objetivo },
    { duration: '5s', target: 0 },
  ];
}

function escenario(rol, fraccion) {
  return {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: stages(fraccion),
    gracefulRampDown: '5s',
    exec: 'recorrido',
    env: { ROL: rol },
    tags: { rol },
  };
}

const thresholds = MODE === 'dependencia-caida'
  ? {
      // Fallo cerrado: toda petición autenticada responde 503 (nunca 200) y rápido (breaker abierto o connect-timeout 2 s).
      fallo_cerrado_503: ['rate>0.99'],
      http_req_duration: ['p(95)<2500'],
    }
  : {
      http_req_failed: [OBJETIVOS.errores],
      'http_req_duration{clase:lecturaPropia}': [OBJETIVOS.lecturaPropia],
      'http_req_duration{clase:agregado}': [OBJETIVOS.agregado],
      checks: ['rate>0.99'],
    };

// Submétricas por endpoint (umbral informativo 'max>=0', siempre pasa) para que el resumen traiga el p95 de cada ruta.
for (const rutas of Object.values(ESCENARIOS)) {
  for (const [ruta] of rutas) {
    thresholds[`http_req_duration{name:${ruta.split('?')[0]}}`] = ['max>=0'];
  }
}

export const options = {
  scenarios: {
    estudiantes: escenario('ESTUDIANTE', 0.5),
    docentes: escenario('DOCENTE', 0.25),
    coordinadores: escenario('COORDINADOR', 0.25),
  },
  thresholds,
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  userAgent: 'asistencias-uco-carga/1.0',
  noConnectionReuse: false,
};

// En modo dependencia-caida 503 es la respuesta correcta (no cuenta como http_req_failed).
if (MODE === 'dependencia-caida') {
  http.setResponseCallback(http.expectedStatuses(503));
}

export function recorrido() {
  const rol = __ENV.ROL;
  const token = TOKENS[rol];
  for (const [ruta, clase] of ESCENARIOS[rol]) {
    const nombre = ruta.split('?')[0];
    const res = http.get(`${BASE_URL}${ruta}`, {
      headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
      tags: { name: nombre, clase },
      timeout: '30s',
    });
    (porEstado[String(res.status)] || otroEstado).add(1);
    if (MODE === 'dependencia-caida') {
      esperado503.add(res.status === 503);
      check(res, { 'PEP caído -> 503 (fallo cerrado)': (r) => r.status === 503 });
    } else {
      check(res, { [`${nombre} 200`]: (r) => r.status === 200 });
    }
    sleep((THINK_MS / 1000) * (0.5 + Math.random()));
  }
}

export function handleSummary(data) {
  const out = __ENV.SUMMARY_FILE || '/work/out/summary.json';
  const m = data.metrics;
  const linea = (k) => (m[k] ? JSON.stringify(m[k].values) : 'n/a');
  const texto = [
    `modo=${MODE} base=${BASE_URL} vus=${VUS} duracion=${DURATION}`,
    `http_reqs=${linea('http_reqs')}`,
    `http_req_failed=${linea('http_req_failed')}`,
    `http_req_duration=${linea('http_req_duration')}`,
    `lecturaPropia=${linea('http_req_duration{clase:lecturaPropia}')}`,
    `agregado=${linea('http_req_duration{clase:agregado}')}`,
    `checks=${linea('checks')}`,
    `estados=${JSON.stringify(Object.fromEntries(Object.entries(m).filter(([k]) => k.startsWith('estado_')).map(([k, v]) => [k, v.values.count])))}`,
    `umbrales=${JSON.stringify(Object.fromEntries(Object.entries(m).filter(([, v]) => v.thresholds).map(([k, v]) => [k, v.thresholds])))}`,
    '',
  ].join('\n');
  return { [out]: JSON.stringify(data, null, 2), stdout: texto };
}
