# Logistics Management

Sistema de laboratorio para registrar envíos, controlar sus transiciones de estado y notificar al cliente de forma asíncrona. El repositorio agrupa dos aplicaciones Spring Boot independientes; no es un proyecto Gradle multiproyecto y no contiene configuración Gradle en la raíz.

## Arquitectura

- `shipment-service`: API REST, reglas de negocio, persistencia PostgreSQL y publicación de eventos.
- `notification-service`: consumo RabbitMQ, deduplicación en memoria y envío SMTP a Gmail o Mailpit.
- PostgreSQL: base `logistics`, esquema `shipment`.
- RabbitMQ: transporte de eventos, reintentos y dead letters.
- Mailpit: servidor SMTP de laboratorio e interfaz para revisar mensajes.

Ambos servicios usan arquitectura hexagonal con `domain`, `application` e `infrastructure`. El dominio no depende de Spring, HTTP, JPA ni RabbitMQ. Los casos de uso son POJOs registrados como beans en `UseCaseConfig`; los adapters dependen de puertos y toda inyección se realiza por constructor.

## Negocio y API

Un envío siempre nace en `CREATED`. Las únicas transiciones admitidas son:

```text
CREATED -> DISPATCHED -> IN_TRANSIT -> DELIVERED
   |
   +-> CANCELLED
```

`DELIVERED` y `CANCELLED` son finales. `DELETE` es una cancelación lógica y nunca elimina la fila.

| Método | Ruta | Resultado |
|---|---|---|
| POST | `/api/v1/shipments` | Crea un envío idempotente y responde 201 |
| GET | `/api/v1/shipments/{id}` | Consulta por UUID |
| GET | `/api/v1/shipments?page=0&size=10&status=CREATED` | Lista y filtra de forma paginada |
| PATCH | `/api/v1/shipments/{id}/dispatch` | `CREATED` a `DISPATCHED` |
| PATCH | `/api/v1/shipments/{id}/in-transit` | `DISPATCHED` a `IN_TRANSIT` |
| PATCH | `/api/v1/shipments/{id}/deliver` | `IN_TRANSIT` a `DELIVERED` |
| DELETE | `/api/v1/shipments/{id}` | `CREATED` a `CANCELLED` |

Swagger está disponible en [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html). Las validaciones responden 400, los recursos inexistentes 404, los conflictos de estado 409 y los fallos inesperados 500 con un cuerpo de error uniforme.

Ejemplo de creación:

```bash
curl -i -X POST http://localhost:8080/api/v1/shipments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: pedido-carolina-001' \
  -d '{"customerName":"Carolina","customerEmail":"carolina@email.com","origin":"Medellín","destination":"Bogotá","description":"Portátil"}'
```

El backend genera el UUID, un `trackingNumber` único con prefijo `SHP-`, el estado y las fechas. Esos campos no forman parte del DTO de entrada.

`Idempotency-Key` es obligatorio en la creación y admite hasta 128 caracteres. Repetir el mismo `POST` con la misma clave y payload devuelve el envío original sin insertar otra fila ni publicar otro evento. Reutilizar la clave con un payload diferente responde `409 Conflict`. Para crear otro envío se debe usar una clave nueva, por ejemplo un UUID generado por el cliente.

## Eventos y RabbitMQ

Las operaciones publican `SHIPMENT_CREATED`, `SHIPMENT_DISPATCHED`, `SHIPMENT_IN_TRANSIT`, `SHIPMENT_DELIVERED` y `SHIPMENT_CANCELLED` respectivamente. Cada evento incluye `eventId`, datos del envío y `occurredAt`.

| Recurso | Nombre |
|---|---|
| Topic exchange | `shipment.exchange` |
| Routing key | `shipment.notification` |
| Cola principal | `shipment.notification.queue` |
| Dead letter exchange | `shipment.dlx` |
| Dead letter routing key | `shipment.notification.dlq` |
| Dead letter queue | `shipment.notification.dlq` |

Un procesamiento exitoso retorna normalmente al listener: RabbitMQ hace ACK y retira el mensaje. Un error se reintenta sin loops manuales con intervalos de 1 y 2 segundos: intento inicial más dos reintentos, tres intentos totales. Tras el tercero, `RejectAndDontRequeueRecoverer` rechaza sin requeue; los argumentos `x-dead-letter-exchange` y `x-dead-letter-routing-key` de la cola lo envían a la DLQ.

JSON inválido, tipos desconocidos y eventos sin `eventType`, `shipmentId` o `customerEmail` fallan de manera controlada y, al agotarse los intentos, llegan a la DLQ sin detener permanentemente al consumidor.

La deduplicación usa `eventId` mediante `ConcurrentHashMap.newKeySet()`. El primer mensaje envía el correo y registra el identificador; el siguiente con el mismo ID se reconoce sin reenviar y deja el log `duplicate event ignored`. Es una solución deliberadamente en memoria para el laboratorio: se pierde al reiniciar y no coordina varias instancias.

RabbitMQ Management está en [http://localhost:15672](http://localhost:15672), con usuario y contraseña `guest`.

## Correo con Gmail y Mailpit

El consumidor genera un asunto distinto para cada evento. El correo de creación incluye nombre, tracking, origen y destino. Docker Compose configura `smtp.gmail.com:587` con autenticación y STARTTLS. Antes de levantar el servicio, completar el archivo `.env` con el correo Gmail remitente y una contraseña de aplicación de Google:

```dotenv
GMAIL_USERNAME=tu-cuenta@gmail.com
GMAIL_APP_PASSWORD=abcdefghijklmnop
```

No utilizar la contraseña normal de Gmail ni subir `.env` a Git. El destinatario real se obtiene de `customerEmail` al crear el envío. Mailpit permanece en Compose para pruebas locales; para usarlo nuevamente hay que configurar `MAIL_HOST=mailpit`, `MAIL_PORT=1025` y desactivar autenticación y STARTTLS.

## Ejecución

Para levantar todo desde la raíz:

```bash
docker compose up --build
```

Para detener y conservar el volumen de PostgreSQL:

```bash
docker compose down
```

Cada proyecto también se construye con su propio wrapper:

```bash
cd shipment-service
./gradlew clean test
./gradlew clean build

cd ../notification-service
./gradlew clean test
./gradlew clean build
```

## Demostraciones

### Consumidor no disponible

Detener solo el consumidor, crear un envío y revisar el mensaje pendiente:

```bash
docker compose stop notification-service
curl -X POST http://localhost:8080/api/v1/shipments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: prueba-consumidor-detenido-001' \
  -d '{"customerName":"Carolina","customerEmail":"carolina@email.com","origin":"Medellín","destination":"Bogotá"}'
```

En RabbitMQ Management, abrir `Queues and Streams` y comprobar que `shipment.notification.queue` tiene un mensaje `Ready`. Después:

```bash
docker compose start notification-service
```

El mensaje pasa a cero y el correo llega al destinatario configurado en `customerEmail`. Si se usa la configuración local de Mailpit, aparece en su interfaz.

### Tres intentos y DLQ

La condición visible de laboratorio es exactamente `customerEmail == "fail@email.com"`:

```bash
curl -X POST http://localhost:8080/api/v1/shipments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: prueba-dlq-001' \
  -d '{"customerName":"Falla","customerEmail":"fail@email.com","origin":"Medellín","destination":"Bogotá"}'
docker compose logs -f notification-service
```

Los logs muestran tres recepciones/fallos. Después, `shipment.notification.queue` queda sin ese mensaje y `shipment.notification.dlq` incrementa su contador. No se envía correo ni existe requeue infinito.

## Pruebas

Las pruebas cubren creación y publicación, rechazo del email inválido antes de invocar el caso de uso, transición `CREATED -> DELIVERED` inválida sin persistencia/evento, deduplicación, fallo de laboratorio y payload incompleto. Las integraciones Testcontainers levantan PostgreSQL y RabbitMQ efímeros, comprueban persistencia/publicación y verifican retry + DLQ sin instalaciones locales de esos servidores.

Docker Desktop suele ser detectado automáticamente. Si se usa Colima y Testcontainers no encuentra el daemon, ejecutar con el socket del contexto activo, por ejemplo:

```bash
DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')" \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
./gradlew clean test
```
