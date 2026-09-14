# Mercado Pago — qué falta para que funcione correctamente

Estado a 2026-08-31, microservicio `ink-ms-subscriptions`.

## Resumen

El **código de la integración ya está** (Checkout Pro vía SDK `com.mercadopago:sdk-java:2.8.0`):

- `MercadoPagoGatewayClient` — crea la preferencia de pago y consulta el estado real del pago.
- `MercadoPagoWebhookController` + `PagoWebhookService` — reciben la notificación (IPN/webhook), **nunca confían en el payload** y siempre re-consultan el pago contra la API de MP.
- `PagoSuscripcionService` / `PagoEventoService` — crean el pago `PENDIENTE`, devuelven el `checkoutUrl` y activan la suscripción / inscripción cuando el pago queda `APROBADO`.

Lo que **NO funciona todavía** es todo lo que está fuera del código: no hay credenciales, el webhook no es accesible desde internet y el frontend no redirige al checkout ni maneja el retorno. Sin eso, el cobro no se completa de punta a punta.

Prioridad: **Parte A (bloqueante) → Parte C (frontend) → Parte D (probar) → Parte B (robustez)**.

---

## Parte A — Configuración que falta (BLOQUEANTE)

### A1. Cuenta y credenciales de Mercado Pago

1. Crear cuenta en <https://www.mercadopago.com.co> (Colombia, moneda COP).
2. Entrar al panel de desarrollador: <https://www.mercadopago.com.co/developers/panel/app> → **Crear aplicación** (tipo "Pagos online" / Checkout Pro).
3. En la aplicación, sección **Credenciales**, copiar:
   - **Credenciales de prueba (sandbox):** `Access Token` (empieza por `TEST-...`) y `Public Key` (`TEST-...`).
   - Las de **producción** se piden solo cuando el flujo de prueba ya funciona.
4. Crear **usuarios de prueba** (Sandbox → "Cuentas de prueba"): uno **vendedor** (cuyo Access Token se usa en el backend) y uno **comprador** (para pagar en el checkout). No se puede pagar una preferencia con la misma cuenta que la creó.

> El `Public Key` solo hace falta si el frontend integra el formulario de tarjeta embebido (Bricks). Si el frontend simplemente **redirige** al `checkoutUrl` que devuelve el backend, con el `Access Token` basta.

### A2. Archivo `.env` del microservicio

`spring-dotenv` ya es dependencia; lee un archivo `.env` en la raíz de `ink-ms-subscriptions/` (está en `.gitignore`, **no se commitea**).

Crear `ink-ms-subscriptions/.env`:

```dotenv
# --- Mercado Pago ---
MP_ACCESS_TOKEN=TEST-xxxxxxxxxxxxxxxx-xxxxxx-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx-xxxxxxxxx
MP_PUBLIC_KEY=TEST-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
MP_CURRENCY=COP

# URL PÚBLICA a la que Mercado Pago enviará el webhook (ver A3)
MP_NOTIFICATION_URL=https://TU-SUBDOMINIO.ngrok-free.app/api/pagos/mercadopago/webhook

# Rutas del frontend a las que MP devuelve al usuario tras pagar (ver A4)
MP_BACK_URL_SUCCESS=http://localhost:4200/pagos/exito
MP_BACK_URL_PENDING=http://localhost:4200/pagos/pendiente
MP_BACK_URL_FAILURE=http://localhost:4200/pagos/error

# --- Base de datos (A5) ---
DB_USERNAME=root
DB_PASSWORD=tu_password_mysql

# --- Correo para el comprobante PDF, RF69 (A5) ---
MAIL_USERNAME=tucorreo@gmail.com
MAIL_PASSWORD=clave_de_aplicacion_de_gmail

# --- JWT: debe ser EL MISMO valor que en ink-ms-auth ---
JWT_SECRET=inklusport2024superSecretKeyForJWTtokenGenerationWith512bitsAlgorithmHS512
AUTH_SERVICE_URL=http://localhost:3001
```

> En perfil `docker` las variables se pasan por `environment:` en el `docker-compose`, no por `.env`.

### A3. `notification_url` pública — el webhook

Mercado Pago corre en sus servidores y hace `POST` al `notification_url`. Con el valor por defecto (`http://localhost:3005/...` o `http://suscripciones-service:3005/...`) **nunca llegará** la notificación y la suscripción se quedará en `SUSPENDIDA` / el pago en `PENDIENTE` para siempre.

Opciones:

- **Desarrollo:** túnel a `localhost:3005`
  ```bash
  ngrok http 3005
  # o: cloudflared tunnel --url http://localhost:3005
  ```
  Copiar la URL `https://xxxx.ngrok-free.app` y ponerla en `MP_NOTIFICATION_URL` con el path `/api/pagos/mercadopago/webhook`. Reiniciar el microservicio (el `notification_url` se fija al **crear la preferencia**, no se puede cambiar sobre una preferencia ya creada).
- **Producción / despliegue:** dominio público con HTTPS que resuelva al gateway o directamente al microservicio.

Verificación: el endpoint `POST /api/pagos/mercadopago/webhook` es público (no requiere JWT — ya configurado en `SecurityConfig` y `JwtAuthenticationFilter`) y **siempre responde `200 OK`** aunque falle el procesamiento (para que MP no reintente en bucle). Los errores quedan en el log.

Opcional: registrar la misma URL en el panel de MP (Webhooks / Notificaciones) para las notificaciones que no van atadas a una preferencia.

### A4. `back_urls` y rutas en el frontend

Tras pagar, MP redirige el navegador del usuario a una de las tres `back_urls`. Hoy apuntan a `http://localhost:4200/pagos/{exito|pendiente|error}` pero **esas rutas no existen en el Angular** (`Fronted-Inklusport`). Hay que crearlas (ver Parte C).

Con `auto_return="approved"` (ya configurado en el backend), en un pago aprobado MP redirige **solo**, sin que el usuario tenga que pulsar "volver al sitio".

### A5. Dependencias del flujo de pago: base de datos y correo

El pago aprobado dispara: activar suscripción → generar **comprobante PDF** (RF69) → **enviar correo** con el PDF adjunto.

- **MySQL** corriendo en `localhost:3306`. La URL ya incluye `createDatabaseIfNotExist=true`, así que crea el schema `inklusport_subscriptions` solo. Setear `DB_USERNAME` / `DB_PASSWORD`.
- **Correo (RF69):** setear `MAIL_USERNAME` / `MAIL_PASSWORD`. Para Gmail hay que generar una **clave de aplicación** (no la contraseña normal). Si se dejan vacíos, el pago igual se procesa y el comprobante se guarda; solo se omite el envío del correo con un `WARN` en el log.
- **Carpeta de comprobantes:** `./comprobantes` en local (o `/app/comprobantes` en docker). Se crea sola; asegurar permisos de escritura.

---

## Parte B — Código del backend

### Ya corregido en esta rama

| Tema | Qué se hizo |
|------|-------------|
| Un fallo al generar el PDF revertía el pago aprobado | `generarPdf` ya no relanza; comprobante+correo van en `try/catch` dentro de `confirmarPago`. El cobro confirmado nunca se revierte. |
| Notificaciones duplicadas / concurrentes de MP | `@Version` (bloqueo optimista) en `PagoSuscripcion` y `PagoEvento` → solo una notificación activa; la otra falla y se ignora. |
| `mapEstado` incompleto | `refunded` y `charged_back` → `RECHAZADO`. |
| UX del retorno | `auto_return="approved"` en la preferencia. |
| Filtro JWT se ejecutaba 2 veces por request | `FilterRegistrationBean(...).setEnabled(false)`. |
| Envío de correo sin `MAIL_USERNAME` | Guard: se omite con `WARN` en vez de fallar. |
| El schema de BD debía existir a mano | `createDatabaseIfNotExist=true` en la URL JDBC. |

### Pendiente (no bloquea la demo, sí recomendable antes de producción)

1. **Validar la firma del webhook (`x-signature` / `x-request-id`).**
   Hoy no se valida. Está mitigado porque nunca se confía en el payload y se re-consulta el pago con el token propio, pero validar la firma HMAC (secret del panel de MP) evita procesamiento de notificaciones falsas. Ubicación: `MercadoPagoWebhookController` / `PagoWebhookService`.

2. **Job de reconciliación / confirmación por `back_url`.**
   Si el webhook se pierde una vez, el pago queda `PENDIENTE` sin reintento. Añadir uno de:
   - `@Scheduled` que cada X minutos busque `PagoSuscripcion` / `PagoEvento` en `PENDIENTE` con antigüedad > N min y llame a `paymentGatewayClient.consultarPago(...)` para resolverlos; **o**
   - un endpoint `GET /api/pagos/{referencia}/estado` que el frontend (página `/pagos/exito`) llame para forzar la consulta contra MP al volver del checkout.

3. **Poder cancelar una suscripción `SUSPENDIDA` que nunca se pagó.**
   `POST /api/suscripciones` crea la suscripción en `SUSPENDIDA` antes de pagar. Si el usuario abandona el checkout, queda `SUSPENDIDA` y `crearSolicitud` rechaza cualquier reintento ("ya existe una suscripcion en estado SUSPENDIDA"). Opciones: que `crearSolicitud` reutilice la `SUSPENDIDA` sin historial, o un `@Scheduled` que expire las `SUSPENDIDA` con pago `PENDIENTE` viejo, o un `DELETE /api/suscripciones/{id}` para el propio organizador.

4. **Evitar pagos `PENDIENTE` duplicados.**
   `inscribirse` (eventos) y `renovar` (suscripciones) crean un `Pago*` nuevo + una preferencia MP en cada llamada. Reutilizar el `PENDIENTE` existente (guardando `checkoutUrl` / `preferenceId` en la entidad) o marcar los anteriores como caducados.

5. **Migración del campo `version`.**
   Si ya hay filas en `pago_suscripcion` / `pago_evento`, `ddl-auto=update` añade `version` como `NULL` y el primer update de esas filas fallará. Ejecutar una sola vez:
   ```sql
   UPDATE pago_suscripcion SET version = 0 WHERE version IS NULL;
   UPDATE pago_evento      SET version = 0 WHERE version IS NULL;
   ```
   (Con datos nuevos no hace falta.)

6. **Perfil `docker` desactiva toda la seguridad** (`anyRequest().permitAll()`, sin filtro JWT). No es específico de MP y afecta a varios microservicios, pero el webhook y el resto de endpoints quedan sin autenticación en ese perfil. Decisión de arquitectura pendiente.

---

## Parte C — Frontend (`Fronted-Inklusport`)

Hoy **no hay nada** de pagos en el Angular. Falta:

1. **Servicio de pagos** que llame al backend:
   - `POST /api/suscripciones` → recibe `{ pagoId, monto, estado, referenciaTransaccion, checkoutUrl }`.
   - `POST /api/suscripciones/{id}/renovar`.
   - `POST /api/pagos/eventos/{eventoId}/inscripcion`.
2. **Redirección al checkout:** al recibir la respuesta, si `checkoutUrl != null` →
   `window.location.href = resp.checkoutUrl;`
   (si `checkoutUrl == null` y `estado == APROBADO`, era plan gratuito: ya está activo, no hay pago).
3. **Rutas de retorno** (coinciden con `MP_BACK_URL_*`):
   - `/pagos/exito` — MP añade query params `?payment_id=...&status=approved&external_reference=PS-...&merchant_order_id=...`. La página debe mostrar "pago exitoso" y, como el webhook puede tardar unos segundos, **hacer polling** a `GET /api/suscripciones/actual` (o al endpoint de estado del punto B2) hasta ver la suscripción `ACTIVA`.
   - `/pagos/pendiente` — pago en revisión (efectivo, PSE en proceso...). Mostrar "estamos confirmando tu pago".
   - `/pagos/error` — pago rechazado. Ofrecer reintentar.
4. **Enviar el JWT** (`Authorization: Bearer ...`) en las llamadas de creación/renovación (el webhook NO lleva JWT, pero esas sí).
5. Si se usa el formulario de tarjeta embebido (Bricks) en vez de redirección, además: cargar el SDK JS de MP con el `MP_PUBLIC_KEY`.

---

## Parte D — Probar de punta a punta (sandbox)

1. Levantar MySQL y `ink-ms-auth` (`localhost:3001`).
2. Crear `ink-ms-subscriptions/.env` con las credenciales `TEST-...` (A2).
3. `ngrok http 3005` y poner la URL en `MP_NOTIFICATION_URL` (A3).
4. Arrancar el microservicio **sin** el perfil docker:
   ```bash
   cd ink-ms-subscriptions
   mvn spring-boot:run
   ```
   En el log **no** debe aparecer `mercadopago.access-token no esta configurado`.
5. Obtener un JWT de un usuario organizador (login en `ink-ms-auth`).
6. Crear la suscripción a un plan de pago:
   ```bash
   curl -X POST http://localhost:3005/api/suscripciones \
     -H "Authorization: Bearer <JWT>" -H "Content-Type: application/json" \
     -d '{"planId": 2}'
   ```
   Respuesta esperada: JSON con `checkoutUrl` (`https://www.mercadopago.com.co/checkout/...`).
7. Abrir `checkoutUrl` en el navegador y pagar con una **tarjeta de prueba** y el **usuario comprador** de sandbox:
   - Aprobada: `APRO` — Mastercard `5031 7557 3453 0604`, CVV `123`, venc. `11/30`, titular `APRO`.
   - Rechazada: titular `OTHE`.
   - Docs: <https://www.mercadopago.com.co/developers/es/docs/checkout-pro/additional-content/your-integrations/test/cards>
8. Verificar:
   - En el log del microservicio: `POST /api/pagos/mercadopago/webhook` recibido → `consultarPago` → estado `APROBADO`.
   - En BD: `pago_suscripcion.estado = 'APROBADO'`, `suscripcion.estado = 'ACTIVA'`, fila en `historial_suscripcion`, fila en `comprobante_pago`, PDF en `./comprobantes/`.
   - Correo con el PDF adjunto (si `MAIL_*` está configurado).
   - `GET /api/suscripciones/actual` devuelve la suscripción `ACTIVA`.
9. Repetir para inscripción a evento pago: `POST /api/pagos/eventos/{eventoId}/inscripcion` (el evento debe estar configurado como pago vía `POST /api/eventos-pago/configuracion`).

---

## Checklist

**Configuración (bloqueante)**
- [ ] Cuenta MP Colombia + aplicación creada en el panel
- [ ] `Access Token` y `Public Key` de **prueba** copiados
- [ ] Usuarios de prueba (vendedor + comprador) creados
- [ ] `ink-ms-subscriptions/.env` con `MP_ACCESS_TOKEN`, `MP_CURRENCY=COP`, `DB_*`, `MAIL_*`, `JWT_SECRET` (igual que auth)
- [ ] Túnel público (`ngrok`) y `MP_NOTIFICATION_URL` apuntando a `/api/pagos/mercadopago/webhook`
- [ ] `MP_BACK_URL_*` apuntando a rutas reales del frontend
- [ ] MySQL levantado; `MAIL_USERNAME/PASSWORD` (clave de aplicación) configurados

**Frontend**
- [ ] Servicio Angular que llama a `POST /api/suscripciones` / `renovar` / `inscripcion`
- [ ] Redirección `window.location.href = checkoutUrl`
- [ ] Rutas `/pagos/exito`, `/pagos/pendiente`, `/pagos/error`
- [ ] Polling de estado en `/pagos/exito` mientras el webhook confirma

**Backend (robustez, antes de producción)**
- [ ] Validación de firma `x-signature` del webhook
- [ ] Job de reconciliación de pagos `PENDIENTE` o endpoint de consulta de estado
- [ ] Cancelar/expirar suscripción `SUSPENDIDA` no pagada
- [ ] Evitar `Pago*` `PENDIENTE` duplicados
- [ ] `UPDATE ... SET version = 0 WHERE version IS NULL` (si hay datos previos)
- [ ] Decidir seguridad del perfil `docker`

**Prueba end-to-end**
- [ ] Pago aprobado → suscripción `ACTIVA` + comprobante + correo
- [ ] Pago rechazado → suscripción no se activa
- [ ] Inscripción a evento pago aprobada → comprobante + correo
- [ ] Notificación duplicada de MP → no dobla activación (gracias a `@Version`)
