# Mercado Pago — puesta en marcha (sandbox)

Guía corta para dejar el cobro funcionando de punta a punta en local.
Para el detalle de qué hace cada pieza y qué falta a nivel de robustez, ver
[`MERCADOPAGO-PENDIENTE.md`](./MERCADOPAGO-PENDIENTE.md).

## 1. Credenciales de prueba

1. Crear cuenta en <https://www.mercadopago.com.co> (Colombia, COP).
2. Panel de desarrollador → **Crear aplicación** (Checkout Pro / "Pagos online").
3. Sección **Credenciales de prueba**: copiar `Access Token` (`TEST-…`).
4. Sección **Cuentas de prueba**: crear un usuario **vendedor** y uno **comprador**
   (no se puede pagar una preferencia con la misma cuenta que la creó).
5. Sección **Webhooks**: registrar la URL pública (paso 3) y copiar la
   **Clave secreta** → `MP_WEBHOOK_SECRET`.

## 2. Archivo `.env`

```bash
cp .env.example .env
```

Rellenar como mínimo `MP_ACCESS_TOKEN`, `DB_USERNAME`/`DB_PASSWORD`, `JWT_SECRET`
(el **mismo** que `ink-ms-auth`). `MP_WEBHOOK_SECRET` es opcional en sandbox: si
se deja vacío, el webhook se acepta sin validar la firma (nunca en producción).

> **Este proyecto usa Spring Boot 4.1.1** (a diferencia de `ink-ms-suscripciones`,
> que usaba 3.3.5). `spring-dotenv` **no carga el `.env` automáticamente** bajo
> Boot 4.1.1 (confirmado empíricamente: sin este paso la app conecta a MySQL como
> `root` sin contraseña, ignorando el `.env`). Hay que exportar las variables al
> entorno antes de arrancar:
> ```bash
> set -a; source .env; set +a
> ./mvnw spring-boot:run
> ```

## 3. Túnel público para el webhook

Mercado Pago corre en sus servidores; `localhost` no le llega. Levantar un túnel
a `:3005` y usar esa URL:

```bash
ngrok http 3005
# -> MP_NOTIFICATION_URL=https://xxxx.ngrok-free.app/api/pagos/mercadopago/webhook
```

Reiniciar el microservicio después de cambiar `.env` (el `notification_url` se
fija al **crear la preferencia**, no se puede cambiar sobre una ya creada).

## 4. Arrancar

```bash
# MySQL en :3306 e ink-ms-auth en :3001 ya corriendo
mvn spring-boot:run           # SIN el perfil docker
```

En el log **no** debe aparecer `mercadopago.access-token no esta configurado`.

## 5. Probar

1. Login como organizador en `ink-ms-auth` → copiar el JWT.
2. Crear la suscripción a un plan de pago:
   ```bash
   curl -X POST http://localhost:8080/api/suscripciones \
     -H "Authorization: Bearer <JWT>" -H "Content-Type: application/json" \
     -d '{"planId": 2}'
   ```
   Respuesta: JSON con `checkoutUrl` (`https://www.mercadopago.com.co/checkout/…`).
3. Abrir `checkoutUrl`, pagar con el **usuario comprador** y una **tarjeta de prueba**:

   | Resultado  | Tarjeta                          | CVV | Venc. | Titular |
   |------------|----------------------------------|-----|-------|---------|
   | Aprobada   | Mastercard `5031 7557 3453 0604` | 123 | 11/30 | `APRO`  |
   | Rechazada  | la misma                         | 123 | 11/30 | `OTHE`  |

   Docs: <https://www.mercadopago.com.co/developers/es/docs/checkout-pro/additional-content/your-integrations/test/cards>
4. MP redirige a `http://localhost:4200/pagos/exito?...&external_reference=PS-…&payment_id=…`.
   La página hace polling a
   `GET /api/pagos/{referencia}/estado?paymentId={payment_id}` hasta que el estado
   deja de ser `PENDIENTE` (no depende solo de que llegue el webhook).
5. Verificar en el log: `POST /api/pagos/mercadopago/webhook` → firma OK →
   `consultarPago` → `APROBADO`; y en BD `suscripcion.estado = ACTIVA`,
   fila en `comprobante_pago`, PDF en `./comprobantes/`.

## Checklist rápido

- [ ] `.env` con `MP_ACCESS_TOKEN` (`TEST-…`) y `JWT_SECRET` igual que auth
- [ ] Usuarios de prueba vendedor + comprador
- [ ] `ngrok http 3005` y `MP_NOTIFICATION_URL` con `/api/pagos/mercadopago/webhook`
- [ ] (Prod) `MP_WEBHOOK_SECRET` configurado
- [ ] MySQL + `ink-ms-auth` levantados
- [ ] Frontend: rutas `/pagos/exito|pendiente|error` (módulo `payments`)
