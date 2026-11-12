# MarketHub design

User direction: avoid a blue-and-white marketplace. This rebuild uses plum and stone, with ochre in the brand mark. No binding competitor reference was supplied.

Tokens: ink #2e2735, muted #6c6271, primary plum #624175, accent ochre #e6b956, canvas stone #f6f4f5, surface #ffffff. White is reserved for content surfaces rather than the identity. Red/green/amber express state only.

Plus Jakarta Sans is self-hosted from the bundled font package. Strong rounded letterforms carry the shop identity; the same family remains readable in forms and tables. Headings are left aligned, with restrained negative tracking and a maximum 52px catalog headline.

Public surfaces use a generous top navigation, a concise marketplace introduction, then product-first grids. Management surfaces use the same navigation, a local tab bar and compact tables or forms. No advertising claims, invented stock, testimonials, ratings or photos.

Products are presented as text-only blocks: name, description, price, stock and actions. Product cards, details and cart rows have no images, placeholder illustrations, media captions or reserved image space. The backend has no product-image contract, and the user explicitly requested no visual substitutes. Controls retain the hand-authored SVG stroke family.

The earlier blue-and-white direction was discarded after user feedback. The design avoids the original cream/green visual world, pill-shaped uppercase metadata, decorative gradients and giant metrics. Its identity lives in type, plum color, ochre mark and clear spacing; workflows retain familiar controls.

Accessibility: 44px controls, visible focus, skip link, reduced-motion support, semantic headings, native modal confirmations with keyboard focus, inline validation and live feedback. Tables scroll inside their container on small screens. Catalog grids collapse from four to one column.

## Styling ownership

Design values live in `src/styles/tokens.css`. `src/styles/main.css` composes reset, base, layout, shared UI patterns, utilities and accessibility overrides through explicit cascade layers. Individual page/component styles are colocated and Angular-scoped. See [CSS architecture](src/styles/README.md). The palette and reference sizes are preserved by the CSS architecture refactor.
