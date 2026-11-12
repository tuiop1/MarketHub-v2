# API integration

Verified against local Java controllers, DTOs, services, security chains, gateway routes and realm import on 2026-11-11. The local implementation is authoritative, including existing uncommitted backend changes. No backend changes are part of this rebuild.

## Contract matrix

All paths below are relative to `/api/v1`. All list endpoints use Spring `Page<T>` with zero-based `page`, `size` and `sort=property,asc|desc`. The UI supplies explicit sizes and whitelisted sort values. UUIDs are path parameters. No search, category filter, merchant-product filter or order-by-ID endpoint exists.

| Method | Path                           | Access                                        | Request                                       | Success                                            |
| ------ | ------------------------------ | --------------------------------------------- | --------------------------------------------- | -------------------------------------------------- |
| GET    | /products                      | Public                                        | page,size,sort (name,priceCents,createdAt)    | 200 Page<Product>                                  |
| GET    | /products/{id}                 | Public                                        | UUID                                          | 200 Product                                        |
| GET    | /categories                    | Public                                        | page,size,sort (name)                         | 200 Page<Category>, active only                    |
| GET    | /categories/{id}               | Public                                        | UUID                                          | 200 active Category                                |
| GET    | /merchants                     | Public                                        | page,size,sort (shopName,createdAt)           | 200 Page<Merchant>, verified only                  |
| GET    | /merchants/{id}                | Public                                        | UUID                                          | 200 Merchant (service does not require verified)   |
| POST   | /auth/customers/register       | Public                                        | CustomerRegistration                          | 201 Customer                                       |
| POST   | /auth/merchants/register       | Public                                        | MerchantRegistration                          | 201 Merchant, PENDING                              |
| GET    | /customers/me                  | CUSTOMER                                      | bearer JWT                                    | 200 Customer                                       |
| GET    | /merchants/me                  | MERCHANT, MERCHANT_PENDING, MERCHANT_REJECTED | bearer JWT                                    | 200 Merchant                                       |
| GET    | /carts                         | CUSTOMER                                      | bearer JWT                                    | 200 Cart                                           |
| POST   | /carts/items                   | CUSTOMER                                      | {productId,quantity}                          | 200 CartItem, adds to existing quantity            |
| DELETE | /carts/items/{id}              | CUSTOMER                                      | cart-item UUID, not product UUID              | 200 empty                                          |
| DELETE | /carts/items                   | CUSTOMER                                      | none                                          | 204                                                |
| POST   | /orders/purchase               | CUSTOMER                                      | {items:[{productId,quantity}],paymentMethod}  | 201 Order                                          |
| POST   | /orders/my-cart/purchase       | CUSTOMER                                      | JSON string: `"CARD"`, `"GOOGLE_PAY"`, `"QR"` | 201 Order                                          |
| GET    | /orders/me                     | CUSTOMER                                      | page,size,sort (createdAt)                    | 200 Page<Order>, item snapshots included           |
| GET    | /merchant/products             | MERCHANT                                      | page,size,sort (createdAt,name)               | 200 Page<Product>, own listings including inactive |
| GET    | /merchant/products/{id}        | MERCHANT                                      | UUID, ownership checked                       | 200 Product                                        |
| POST   | /merchant/products             | MERCHANT + VERIFIED profile                   | ProductCreate                                 | 201 Product                                        |
| PUT    | /merchant/products/{id}        | MERCHANT + VERIFIED profile                   | ProductUpdate                                 | 200 Product                                        |
| DELETE | /merchant/products/{id}        | MERCHANT + VERIFIED profile                   | UUID                                          | 200 empty, soft deactivation                       |
| GET    | /admin/categories              | ADMIN                                         | page,size,sort (name)                         | 200 Page<Category>, includes inactive              |
| POST   | /admin/categories              | ADMIN                                         | {name,description}                            | 201 Category                                       |
| PUT    | /admin/categories/{id}         | ADMIN                                         | {name,description}                            | 200 Category                                       |
| PATCH  | /admin/categories/{id}/enable  | ADMIN                                         | empty object                                  | 200 Category                                       |
| PATCH  | /admin/categories/{id}/disable | ADMIN                                         | empty object                                  | 200 Category                                       |
| GET    | /admin/merchants/unverified    | ADMIN                                         | page,size,sort (createdAt)                    | 200 Page<Merchant>, pending only                   |
| PATCH  | /admin/merchants/{id}/verify   | ADMIN                                         | empty object                                  | 204, PENDING → VERIFIED                            |
| PATCH  | /admin/merchants/{id}/reject   | ADMIN                                         | empty object                                  | 204, PENDING → REJECTED                            |
| PATCH  | /admin/merchants/{id}/disable  | ADMIN                                         | empty object                                  | 204, VERIFIED → SUSPENDED                          |
| GET    | /admin/customers               | ADMIN                                         | page,size,sort (createdAt)                    | 200 Page<Customer>                                 |
| PATCH  | /admin/customers/{id}/enable   | ADMIN                                         | empty object                                  | 204                                                |
| PATCH  | /admin/customers/{id}/disable  | ADMIN                                         | empty object                                  | 204                                                |

## DTOs and validation

Exact TypeScript definitions live with the feature APIs. IDs are UUID strings, timestamps ISO instants, dates `YYYY-MM-DD`, and money integer cents. Currency is not returned; the UI retains the original frontend's USD convention. No shipping, tax, currency conversion, image, or real payment-entry contract exists.

- Product: id, merchantId, categoryId, name, nullable description, priceCents, stockQuantity, active, createdAt, updatedAt. Create: name (nonblank, max 255), description (max 5000), priceCents (positive integer), stockQuantity (integer ≥0), categoryId. Update also requires active. Category must be active. Names are trimmed. Merchant ownership and VERIFIED status are enforced server-side.
- Category: id, name, nullable description, active, createdAt. Write name nonblank/max255, description max1000. Case-insensitive duplicate names return 409.
- Merchant: id, shopName, nullable description, email, status (`PENDING|VERIFIED|REJECTED|SUSPENDED`), createdAt, updatedAt, nullable verifiedAt.
- Customer: id, firstName, lastName, nullable birthDate/address, email, enabled, createdAt, updatedAt. Address: nullable country,city,street,postalCode,apartment.
- CustomerRegistration: firstName,lastName (nonblank/max100), email (nonblank/email/max255), password (8–128), optional past birthDate, optional address. Address limits: country/city/apartment100, street255, postalCode32. MerchantRegistration: same identity fields, shopName (nonblank/2–120), description (max2000). Email is unique across customer and merchant identities; shop names are case-insensitively unique. Registration creates a Keycloak identity, grants CUSTOMER or MERCHANT_PENDING, and publishes a Kafka notification.
- Cart: id,customerId,totalPriceCents,cartItems. Item: id,cartId,productId,productName,priceCents,quantity,totalPriceCents,productActive,stockQuantity. Add quantity is integer ≥1 and combined quantity cannot exceed current stock. There is no decrement/set-quantity endpoint: add one or remove the whole line.
- Purchase: 1–50 lines, each productId and integer quantity≥1; paymentMethod CARD/GOOGLE_PAY/QR. Order: id,status,totalPriceCents,items,createdAt. Snapshot item: id,productId,merchantId,productNameSnapshot,merchantNameSnapshot,priceSnapshotCents,quantity,totalPriceSnapshotCents. Status: PENDING_RESERVATION/PENDING_PAYMENT/PAID/PAYMENT_FAILED/CANCELLED/COMPENSATION_FAILED.

## Errors and special behavior

Common error: `{timestamp,status,code,message,path}`; validation adds `errors: Record<string,string>` with dotted nested field paths. UI translates known business codes and status codes rather than rendering raw exceptions. 400 validation, 401 invalid/expired session, 403 role restriction, 404 missing/inactive resource, 409 business conflict, 429 gateway limit, 500 unexpected failure, and 503 upstream outage are handled. Some upstream technical failures use 502. Gateway errors may lack the common body.

A never-created cart returns 404 `RESOURCE_NOT_FOUND`; the same code can represent a missing customer or unavailable product during cart enrichment. The UI treats only the explicit cart/customerId not-found message as empty; ambiguous 404s remain errors so unavailable items are not hidden. The cart read can fail as a whole when a contained product becomes unbuyable. Only removal/clear endpoints can resolve such a cart; the UI offers clear with confirmation even on load failure.

Cart checkout clears the cart only for PAID, and clearing can independently fail. A returned 201 is not proof of payment. CARD and GOOGLE_PAY succeed in the current simulated PaymentService; QR deliberately fails. No real money is charged. The UI states simulation explicitly and preserves failed carts. No automatic retries of purchases: the API provides no frontend idempotency key. After an ambiguous network/5xx failure, check history before retrying.

Public product lists check product/category activation but do not verify merchant status per row. Product details, cart and purchase verify seller eligibility; a listed item can therefore fail details or purchase. This discrepancy is documented, not patched in the backend.

Approval changes realm roles. Refreshing the token or signing in again is needed before the newly granted MERCHANT role is visible. Rejection does not enable management. Suspension disables the Keycloak user. No merchant re-enable endpoint, all-status admin merchant list, account edit, order cancellation, image upload or single-order lookup exists. Order details expand the snapshots in the paginated history.

## Network and authentication

Browser API traffic uses same-origin `/api/v1`, proxied to Gateway :8080. Gateway routes to account/catalog/cart/order; internal payments, reservations, Eureka, Redis, Kafka and notification delivery have no browser client. PostgreSQL stores service data, Redis supports cache/rate limiting, Kafka delivers asynchronous notifications, Eureka discovers services.

Local: app :3000, dev proxy → :8080, Keycloak :8090. Production: existing Caddy `/api/*` → gateway, `/auth/*` → Keycloak, all Angular paths → index.html. Production environment accepts the existing Docker PUBLIC_KEYCLOAK_URL substitution and defaults to origin/auth for a direct npm production build. Client markethub-client is public, standard flow enabled, redirect `${PUBLIC_APP_URL}/callback`; no client secret. keycloak-js handles authorization code, state, nonce and PKCE S256, token renewal and logout. Tokens remain in adapter memory.
