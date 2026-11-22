# Prueba de carga k6 (shipment-service)

Perfiles: smoke (2 VU), load (10 VU), stress (hasta 100 VU).
Arrancar el sistema con Mailpit (no se envian correos reales):
  docker compose -f docker-compose.yml -f quality/docker-compose.mailpit.yml up -d --build
Ejecutar desde la carpeta quality:
  $env:K6_PROFILE = "load"
  docker compose -f docker-compose.k6.yml run --rm k6
Resultados en reports/k6 (summary-*.json y dashboard-*.html).
Umbrales: http_req_failed<1%, p95<1s y p99<3s (peticiones validas), checks>99%.
