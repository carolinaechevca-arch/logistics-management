import http from 'k6/http';
import { check, group, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API = `${BASE_URL}/api/v1/shipments`;
const PROFILE = __ENV.K6_PROFILE || 'load';

const profiles = {
  smoke:  { stages: [{ duration: '20s', target: 2 }] },
  load:   { stages: [{ duration: '30s', target: 10 }, { duration: '1m', target: 10 }, { duration: '15s', target: 0 }] },
  stress: { stages: [{ duration: '30s', target: 20 }, { duration: '30s', target: 50 }, { duration: '30s', target: 100 }, { duration: '15s', target: 0 }] },
};

export const options = {
  stages: (profiles[PROFILE] || profiles.load).stages,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{kind:valid}': ['p(95)<1000', 'p(99)<3000'],
    checks: ['rate>0.99'],
  },
};

const JSON_H = { 'Content-Type': 'application/json' };
const ok = (...codes) => http.expectedStatuses(...codes);

function validBody(tag) {
  return JSON.stringify({
    customerName: `Cliente Prueba ${tag}`,
    customerEmail: `k6-${tag}@example.test`,
    origin: 'Medellin',
    destination: 'Bogota',
    description: 'Paquete de prueba de carga k6',
  });
}

export default function () {
  const tag = `${__VU}-${__ITER}`;
  const key = `k6-${tag}-${Date.now()}`;

  // ~10% de solicitudes invalidas (deben responder 400)
  if (Math.random() < 0.1) {
    group('invalid', () => {
      const bad = http.post(API, JSON.stringify({ customerName: '', customerEmail: 'no-es-correo', origin: '', destination: '', description: '' }),
        { headers: { ...JSON_H, 'Idempotency-Key': key }, tags: { kind: 'invalid' }, responseCallback: ok(400) });
      check(bad, { 'invalido -> 400': (r) => r.status === 400 });
      const noKey = http.post(API, validBody(tag), { headers: JSON_H, tags: { kind: 'invalid' }, responseCallback: ok(400) });
      check(noKey, { 'sin Idempotency-Key -> 400': (r) => r.status === 400 });
    });
    sleep(1);
    return;
  }

  group('flujo valido', () => {
    const body = validBody(tag);
    const created = http.post(API, body, { headers: { ...JSON_H, 'Idempotency-Key': key }, tags: { kind: 'valid' } });
    const okCreate = check(created, { 'crear -> 201': (r) => r.status === 201 });
    if (!okCreate) return;
    const id = created.json('id');

    // Idempotencia: mismo key y mismo cuerpo devuelve el envio existente
    const again = http.post(API, body, { headers: { ...JSON_H, 'Idempotency-Key': key }, tags: { kind: 'valid' } });
    check(again, { 'idempotente -> 200/201': (r) => r.status === 200 || r.status === 201 });

    check(http.get(`${API}/${id}`, { tags: { kind: 'valid' } }), { 'consultar -> 200': (r) => r.status === 200 });
    check(http.get(`${API}?page=0&size=10`, { tags: { kind: 'valid' } }), { 'listar -> 200': (r) => r.status === 200 });

    const r = Math.random();
    if (r < 0.6) {
      check(http.patch(`${API}/${id}/dispatch`, null, { tags: { kind: 'valid' } }), { 'despachar -> 200': (x) => x.status === 200 });
      if (r < 0.3) {
        check(http.patch(`${API}/${id}/in-transit`, null, { tags: { kind: 'valid' } }), { 'en transito -> 200': (x) => x.status === 200 });
        check(http.patch(`${API}/${id}/deliver`, null, { tags: { kind: 'valid' } }), { 'entregar -> 200': (x) => x.status === 200 });
      }
    }
  });
  sleep(1);
}
