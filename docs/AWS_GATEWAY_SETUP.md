# API Gateway AWS Academy — Semana 6

Base URL: `https://c60n9nxi6c.execute-api.us-east-1.amazonaws.com/DEV`

Archivos OpenAPI listos para import:

| Archivo | Formato | Uso |
|---------|---------|-----|
| [api-gateway-OAS-DEV-proxy-only.json](api-gateway-OAS-DEV-proxy-only.json) | JSON | **Recomendado** — 13 rutas + JWT `azureidaas` + proxy EC2 |
| [api-gateway-OAS-DEV.json](api-gateway-OAS-DEV.json) | JSON | Referencia / alias (mismo contenido securitizado) |
| [api-gateway-OAS-DEV.yaml](api-gateway-OAS-DEV.yaml) | YAML | Equivalente YAML (referencia) |

---

## Import Merge (recomendado)

Usa este flujo para aplicar todas las rutas y el authorizer sobre la API existente **sin cambiar la URL** de Postman.

### Pasos en consola AWS

1. **API Gateway** → **HTTP APIs** → seleccionar API `cdy2204-1` (id `c60n9nxi6c`).
2. Menú **Develop** → **Import**.
3. Seleccionar [api-gateway-OAS-DEV-proxy-only.json](api-gateway-OAS-DEV-proxy-only.json).
4. Modo de import: **Merge** (actualiza rutas del OAS; no elimina rutas huérfanas solas).
5. Confirmar import.

### Post-import (obligatorio)

1. **Routes** → eliminar manualmente la ruta obsoleta `GET /api/pedidos/{pedido_id_1}` si sigue existiendo (duplicado del export anterior).
2. **Authorization** → pestaña *Asociar autorizadores a rutas* → verificar marca azul **JWT Auth** en las **13** operaciones (evidencia profesor).
3. **Authorizers** → verificar `azureidaas` (issuer y audience abajo).
4. **Integrations** → spot-check:
   - `GET /s3/{bucket}/objects` → `http://52.45.88.121:8080/s3/{bucket}/objects` (no `cdy2204-1` hardcodeado).
   - `PUT /api/pedidos/{pedido_id}` y `POST /s3/{bucket}/move` presentes.
5. **Deploy** → stage **DEV** (sin deploy los cambios no aplican en la URL invoke).
6. Confirmar invoke URL: `https://c60n9nxi6c.execute-api.us-east-1.amazonaws.com/DEV`.

### Si cambia la IP de EC2

Editar las URIs de integración en el OAS (`52.45.88.121`) y repetir Import Merge, o actualizar integraciones manualmente en consola.

---

## Error: "Unable to deploy API because no valid routes exist in this API"

Significa que la API **no tiene rutas con integración válida** (lista vacía o rutas rotas). Suele pasar si:

- Se borraron todas las rutas al limpiar `{pedido_id_1}`.
- Un Import falló o dejó integraciones huérfanas.
- Se intentó Deploy **antes** de reimportar el OAS.

### Recuperación (paso a paso)

1. **API Gateway** → HTTP APIs → `c60n9nxi6c` → menú **Routes**.
2. Si la lista está **vacía** o casi vacía:
   - **Develop** → **Import**.
   - Archivo: [api-gateway-OAS-DEV-proxy-only.json](api-gateway-OAS-DEV-proxy-only.json).
   - Modo: **Merge**. Si sigue vacío, repetir con **Overwrite** (restaura las 13 operaciones del OAS).
3. Tras import, en **Routes** debes ver **7 rutas** con métodos, por ejemplo:
   - `/api/pedidos` (GET, POST)
   - `/api/pedidos/{pedido_id}` (GET, PUT, DELETE)
   - `/api/pedidos/{pedido_id}/generar-guia` (POST)
   - `/s3/{bucket}/objects`, `/consulta`, `/object`, `/move`
4. **Integrations** → cada ruta debe apuntar a `http://52.45.88.121:8080/...`.
5. **Authorizers** → `azureidaas` con issuer y audience corregidos (sección JWT arriba).
6. **Deploy** → stage **DEV** (solo cuando Routes tenga contenido).

**No borres todas las rutas.** Solo elimina la obsoleta `GET /api/pedidos/{pedido_id_1}` si aparece duplicada.

---

## JWT Authorizer `azureidaas`

| Campo | Valor |
|-------|-------|
| Tipo | JWT |
| Identity source | `$request.header.Authorization` |
| **Issuer (debe coincidir con `iss` del token — ver 0.0 C)** | `https://empresatransportistaefs.b2clogin.com/tfp/972f25cf-cd03-4c70-84ea-285778b48398/b2c_1_cdy2204-1/v2.0/` |
| Audience | `49f4ab51-5e0e-4139-9cf4-15f566581b07` y/o scope API `https://EmpresaTransportistaEFS.onmicrosoft.com/49f4ab51-5e0e-4139-9cf4-15f566581b07/cdy2204-1` |

**Issuer real del token (verificado en 0.0 C):** B2C emite el `iss` en forma **tfp** con la política en minúscula (`b2c_1_cdy2204-1`). Ese mismo valor debe estar **idéntico** en el Authorizer AWS y en el `issuer-uri` de Spring (`application.properties`).

**Identificar el origen del 401:** si el body es `{"message":"Unauthorized"}` lo rechaza el **Gateway** (issuer/aud del Authorizer); si la respuesta trae header `WWW-Authenticate: Bearer error="invalid_token" ... decode the Jwt` con body vacío, lo rechaza **Spring** (issuer-uri/jwk-set-uri en EC2).

### Arreglo inmediato en consola AWS (sin esperar reimport)

1. **API Gateway** → **Authorizers** → `azureidaas` → **Edit**.
2. **Issuer** = copiar el `iss` exacto de **0.0 C** (consola Postman), forma **tfp**:
   `https://empresatransportistaefs.b2clogin.com/tfp/972f25cf-cd03-4c70-84ea-285778b48398/b2c_1_cdy2204-1/v2.0/`
3. **Audience** = ambos valores (client_id + scope API).
4. **Save** → **Deploy** DEV.
5. Postman **0.1**: Manage Tokens → borrar → Get New Access Token → Use Token → Send.

O reimportar [api-gateway-OAS-DEV-proxy-only.json](api-gateway-OAS-DEV-proxy-only.json) (Merge) — ya trae el issuer `tfp` alineado con B2C.

### Flujo principal (evaluacion — JWT Auth en Gateway)

1. **Develop** → **Import** → [api-gateway-OAS-DEV-proxy-only.json](api-gateway-OAS-DEV-proxy-only.json) → **Merge**.
2. **Authorization** → confirmar **JWT Auth** en las 13 rutas.
3. **Deploy** DEV.
4. Postman **0.1** → **0.0 B** → debe dar **200** si issuer/audience coinciden con el token B2C.

### Fallback (solo si Gateway rechaza tokens B2C)

Si **0.0 A** EC2 da 200 pero **0.0 B** Gateway da 401 tras ajustar issuer/audience:

1. Crear copia del OAS **sin** bloques `security` ni `components` (version historica sin JWT en Gateway).
2. **Import** → **Overwrite** con esa copia (Spring valida JWT en EC2; no habra marca azul JWT Auth).
3. Documentar la limitacion B2C + issuer en el Word de la actividad.

### Si Postman devuelve `{"message":"Unauthorized"}` (rutas OK, token enviado)

1. **Reimportar environment** `Semana6.postman_environment.json` (corregido).
2. **0.1** → Get New Access Token → **Use Token** → Send (token nuevo, no el de claves).
3. **0.0 C** → en consola Postman ver `iss` y `aud` del token.
4. En AWS Authorizer `azureidaas` verificar **Audience** (ambos valores):
   - `49f4ab51-5e0e-4139-9cf4-15f566581b07`
   - `https://EmpresaTransportistaEFS.onmicrosoft.com/49f4ab51-5e0e-4139-9cf4-15f566581b07/cdy2204-1`
5. **Issuer** en AWS = el mismo `iss` que muestra **0.0 C** (forma `tfp` con `b2c_1_cdy2204-1` en minúscula).
6. **Deploy** DEV.
7. Comparar **0.0 A** (EC2) vs **0.0 B** (Gateway):
   - EC2 200 + Gateway 401 → problema en Authorizer AWS (aud/issuer).
   - Ambos 401 → token OAuth inválido o expirado.

**Azure B2C (opcional):** User flow → Token compatibility → activar emisión de issuer compatible con OIDC si tu tenant lo permite.

### Si el Import falla: "Invalid issuer" / "Unable to create Authorizer"

El issuer del OAS debe coincidir con el claim `iss` del Access Token (forma `tfp`). Reimporta [api-gateway-OAS-DEV-proxy-only.json](api-gateway-OAS-DEV-proxy-only.json) o edita `azureidaas` manualmente en consola.

---

## Rutas completas (OAS corregido)

| Metodo | Ruta Gateway | Backend EC2 |
|--------|--------------|-------------|
| GET, POST | `/api/pedidos` | `:8080/api/pedidos` |
| GET, PUT, DELETE | `/api/pedidos/{pedido_id}` | `:8080/api/pedidos/{pedido_id}` |
| POST | `/api/pedidos/{pedido_id}/generar-guia` | `:8080/api/pedidos/{pedido_id}/generar-guia` |
| GET | `/s3/{bucket}/objects` | `:8080/s3/{bucket}/objects` |
| GET | `/s3/{bucket}/consulta` | `:8080/s3/{bucket}/consulta` |
| GET, POST, PUT, DELETE | `/s3/{bucket}/object` | `:8080/s3/{bucket}/object` |
| POST | `/s3/{bucket}/move` | `:8080/s3/{bucket}/move` |

**Total: 13 operaciones** (7 path patterns). Query strings (`fecha`, `transportista`, `key`, etc.) se reenvían por el proxy HTTP; no requieren rutas extra.

---

## Validacion Postman vs OAS

Coleccion: `postman/Pruebas-Semana6.postman_collection.json`. Todas las requests con `{{api_gateway_url}}` tienen ruta equivalente en el OAS:

| Carpeta Postman | Metodo | Ruta Postman | En OAS |
|-----------------|--------|--------------|--------|
| 0.0 B | GET | `/api/pedidos` | Si |
| 1.1, 1.3 | POST | `/api/pedidos` | Si |
| 1.2, 1.4 | POST | `/api/pedidos/{pedido_id}/generar-guia` | Si |
| 1.5 | GET | `/api/pedidos` | Si |
| 1.6 | GET | `/api/pedidos/{pedido_id_1}` → `{pedido_id}` | Si |
| 1.7 | PUT | `/api/pedidos/{pedido_id_1}` → `{pedido_id}` | Si |
| 1.8 | DELETE | `/api/pedidos/{pedido_id_2}` → `{pedido_id}` | Si |
| 1 cloud.1 | GET | `/s3/{bucket}/objects` | Si |
| 1 cloud.2–3 | GET | `/s3/{bucket}/consulta` | Si |
| 1 cloud.4 | GET | `/s3/{bucket}/object` | Si |
| 1 cloud.5 | POST | `/s3/{bucket}/object` | Si |
| 1 cloud.6 | PUT | `/s3/{bucket}/object` | Si |
| 1 cloud.7 | POST | `/s3/{bucket}/move` | Si |
| 1 cloud.8 | DELETE | `/s3/{bucket}/object` | Si |
| 2 (LECTOR) | GET | `/s3/{bucket}/object` | Si |
| 2 | POST | `/api/pedidos` (403 esperado) | Si |
| 2 | DELETE | `/s3/{bucket}/object` (403 esperado) | Si |
| 3 (401) | GET | `/api/pedidos`, `/s3/.../objects`, `/s3/.../object` sin token | Si |

Variables Postman `pedido_id_1` / `pedido_id_2` se resuelven en path `{pedido_id}` del Gateway.

---

## Correcciones respecto al export original

| Problema en export viejo | Estado en OAS nuevo |
|--------------------------|---------------------|
| Falta `PUT /api/pedidos/{pedido_id}` | Incluido |
| Falta `POST /s3/{bucket}/move` | Incluido |
| `GET /s3/{bucket}/objects` con bucket fijo `cdy2204-1` | Corregido a `{bucket}` |
| Ruta duplicada `{pedido_id_1}` | Unificada en `{pedido_id}` (eliminar `{pedido_id_1}` manualmente tras merge) |

---

## Flujo de autenticacion

1. Postman obtiene **Access Token** de Azure AD B2C (OAuth Authorization Code + PKCE).
2. Postman envia `Authorization: Bearer {access_token}` al API Gateway.
3. Gateway valida JWT localmente (issuer, audience, firma).
4. Si OK, reenvia request a EC2:8080 con el mismo header.
5. Spring Security valida JWT de nuevo y aplica roles (`GESTOR_GUIAS` / `LECTOR_GUIAS`).

---

## Validacion Postman (despues del import)

Environment: `postman/Semana6.postman_environment.json` (nombre **Semana 6**).

| Paso | Request | Resultado esperado |
|------|---------|-------------------|
| 1 | **0.1** OAuth GESTOR → Use Token | Token en Authorization |
| 2 | **0.0 C** | iss/aud/rol correctos |
| 3 | **0.0 B** GET `/api/pedidos` via Gateway | 200 |
| 4 | **Carpeta 1** completa | Incluye PUT pedido y POST move |
| 5 | **0.2** LECTOR + **Carpeta 2** | GET object 200; POST pedidos 403 |
| 6 | **Carpeta 3** sin token | 401 Gateway |

Diagnostico rapido:

- `{"message":"Unauthorized"}` → JWT Authorizer Gateway (token, iss, aud).
- 401 con body Spring → issuer Spring distinto al token.
- 404 en Carpeta 1 → ruta no importada o stage DEV no desplegado.

---

## Evidencia para documento Word

1. Captura Import Merge con [api-gateway-OAS-DEV-proxy-only.json](api-gateway-OAS-DEV-proxy-only.json).
2. Captura **Authorization** → JWT Auth azul en las 13 rutas.
3. Captura Authorizer `azureidaas` con issuer/audience.
4. Captura rutas del stage DEV (13 operaciones).
5. Postman: **0.0 C** + **0.0 B** (200) + **Carpeta 1** completa.

---

## Checklist post-import (verificacion manual en AWS)

Ejecutar en consola AWS tras Import Merge + Deploy DEV:

| # | Donde | Que verificar |
|---|-------|---------------|
| 1 | **Authorization** | Marca azul **JWT Auth** en GET/POST/PUT/DELETE de las 13 operaciones |
| 2 | **Authorizers** | `azureidaas` con issuer = `iss` del token (forma `tfp`, `b2c_1` minúscula) y ambos audience |
| 3 | **Deploy** | Stage **DEV** desplegado (fecha reciente) |
| 4 | Postman **0.0 B** | GET `/api/pedidos` via Gateway → **200** |
| 5 | Postman **Carpeta 3** | Sin token → **401** |
| 6 | Postman **Carpeta 1–2** | Flujo GESTOR/LECTOR sin cambios en URLs ni OAuth |
