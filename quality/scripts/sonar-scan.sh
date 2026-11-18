#!/usr/bin/env bash
set -uo pipefail
export SONAR_TOKEN="${1:?Falta el token de SonarQube}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
for s in shipment-service notification-service; do
  echo "== $s =="
  (cd "$ROOT/$s" && ./gradlew clean test jacocoTestReport --no-daemon) || echo "Fallaron pruebas en $s; se continúa."
  (cd "$ROOT/$s" && ./gradlew sonar -x test -x jacocoTestReport --no-daemon) || { echo "Falló el análisis en $s"; exit 1; }
done
echo "Resultados: http://localhost:9000/projects"
