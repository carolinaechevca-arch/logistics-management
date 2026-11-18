# Calidad: SonarQube y OWASP ZAP (logistics-management)

Herramientas para el Caso práctico final: análisis estático (SonarQube + JaCoCo) y análisis dinámico (OWASP ZAP).
Se ejecutan con Docker desde PowerShell. El proyecto ya trae JaCoCo y el plugin `org.sonarqube`
en shipment-service; aquí se agregó el mismo plugin a notification-service.

> ZAP solo se aplica a **shipment-service**, el único servicio con API HTTP. notification-service solo consume
> mensajes de RabbitMQ y no expone puertos, así que se evalúa de forma estática con SonarQube.

## 1. SonarQube (análisis estático + cobertura)
Desde la carpeta `quality`:
```powershell
docker compose -f docker-compose.quality.yml up -d sonarqube
```
1. Espera 1-2 minutos y abre http://localhost:9000 (`admin` / `admin`; pedirá cambiar la clave).
2. **My Account → Security → Generate Tokens** (tipo *User*) y copia el token.
3. Con Docker Desktop abierto (las pruebas usan Testcontainers):
```powershell
.\scripts\sonar-scan.ps1 -Token <TOKEN>
```
4. Resultados en http://localhost:9000/projects: dos proyectos, **Logistics Shipment Service** y **Logistics Notification Service**.

**Capturas para el informe (por cada proyecto):** Summary (Quality Gate, ratings, Coverage, Duplications), Issues,
Security Hotspots y Reporting → Measures (Security, Reliability, Maintainability, Coverage, Duplications, Size, Complexity).
En SonarQube con modo MQR: Bugs = Reliability, Vulnerabilidades = Security, Code Smells = Maintainability.

El proyecto ya excluye de la cobertura las clases sin lógica (puertos, enums, DTOs, entidades) y exige 90 % de
cobertura en JaCoCo; el reporte de SonarQube refleja ese mismo criterio.

## 2. OWASP ZAP (análisis dinámico)
ZAP crea envíos de prueba. Para que **no se envíen correos reales por Gmail**, levanta el sistema con Mailpit.
Desde la **raíz del proyecto** (donde está `docker-compose.yml`):
```powershell
docker compose -f docker-compose.yml -f quality/docker-compose.mailpit.yml up -d --build
curl.exe http://localhost:8080/v3/api-docs
```
La segunda línea debe devolver un JSON con la definición OpenAPI. Luego, desde la carpeta `quality`:
```powershell
docker compose -f docker-compose.quality.yml --profile dast run --rm zap
```
Tarda varios minutos. Genera en `quality\zap\`: `zap-report.html` (captura/anexo), `zap-report.json` y `zap-report.md`.
Ábrelo con `start .\zap\zap-report.html`. Captura también la consola final (`FAIL-NEW`, `WARN-NEW`, `PASS`).

ZAP lee la definición OpenAPI que publica la propia aplicación (springdoc), así que conoce los 7 endpoints de
`/api/v1/shipments` sin configuración manual.

Si ZAP dice que no puede importar la definición, abre http://localhost:8080/v3/api-docs en el navegador y revisa
que el campo `servers` no apunte a otro host; avísame y se ajusta.

## 3. Qué analizar en el informe (no solo pegar capturas)
Observaciones del código y la configuración; confírmalas o refútalas con lo que muestre ZAP:

| OWASP Top 10 (2021) | Observación | Mitigación propuesta |
|---|---|---|
| A01 Control de acceso / A07 Autenticación | Ningún endpoint requiere autenticación (el proyecto no usa Spring Security) | OAuth2/JWT con Spring Security o un API gateway |
| A05 Configuración insegura | Credenciales por defecto: `postgres/postgres` en el compose y `guest/guest` en RabbitMQ; puertos 5432, 5672, 15672 y 8025 publicados en el host | Secretos por variables de entorno; no publicar puertos de administración y base de datos |
| A05 Configuración insegura | Swagger UI y `/v3/api-docs` públicos | Deshabilitar springdoc fuera de desarrollo |
| A02 Fallas criptográficas | API por HTTP sin TLS | TLS en el gateway |
| A04 Diseño inseguro | Sin límite de tasa: se pueden crear envíos y eventos sin restricción | Rate limiting |
| A03 Inyección | Validación Jakarta en el cuerpo y parámetros; JPA con consultas parametrizadas | Mantener y agregar longitudes máximas a los campos de texto |
| A09 Registro y monitoreo | Los logs no incluyen datos personales (solo ids, tracking y estado); no hay actuator ni métricas | Agregar monitoreo y alertas |

Buena práctica ya presente: idempotencia con `Idempotency-Key` (reintentos seguros), contenedores sin credenciales
de correo embebidas (`.env` fuera de Git) y manejo uniforme de errores 400/404/409.
Posible hallazgo de ZAP: el manejador `@ExceptionHandler(Exception.class)` devuelve 500 ante rutas inexistentes o
métodos no permitidos, cuando deberían ser 404 o 405.

## 4. Apagar
```powershell
cd ..
docker compose -f docker-compose.yml -f quality/docker-compose.mailpit.yml down -v
cd quality
docker compose -f docker-compose.quality.yml --profile dast down
```
Agrega `-v` a este último para borrar también los datos de SonarQube.
