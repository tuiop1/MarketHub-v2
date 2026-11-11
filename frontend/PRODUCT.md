# MarketHub

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Customers browse and buy products. Merchants register for approval and manage their own listings. Administrators review merchants, categories and customer access. The project owner also uses this code to learn Angular and explain engineering decisions in interviews.

## Product Purpose

A functional marketplace integrated with the existing Spring microservices, suitable for continued development and understandable to an experienced Angular engineer.

## Operating Context

Angular 20 standalone application. Local app :3000, gateway :8080, Keycloak :8090. Production Caddy serves routes and proxies /api and /auth.

## Capabilities and Constraints

The user's rebuild brief and API_INTEGRATION.md define scope. Backend and infrastructure must remain untouched. No invented endpoints, images, mock production data, real payment claims or hardcoded accounts. All currently supported workflows must remain accessible. Practical architecture, strict typing, tests and documentation are required.

## Brand Commitments

MarketHub name. Professional, polished, modern marketplace with restrained effects, clear hierarchy and accessible feedback. The user rejected the initial blue-and-white direction. Specific competitor references remain open. The chosen plum-and-stone direction is documented in DESIGN.md.

## Evidence on Hand

The complete local Java source and original Angular client. No product image contract or supplied visual assets. The user requires text-only product blocks: no product images, image placeholders, illustrations or reserved media areas. Payments are simulated. No testimonials or marketplace performance claims are supplied.

## Product Principles

- Backend contracts determine behavior.
- Features own their state and UI.
- Make errors recoverable and outcomes honest.
- Favor readable code and few dependencies.

## Accessibility & Inclusion

Responsive desktop/tablet/mobile layouts, semantic controls, visible focus, labeled forms, accessible confirmations and WCAG 2.2 AA where practical, as requested in the brief.
