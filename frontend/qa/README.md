# Visual verification evidence

`catalog-mobile-fixtures.jpg` shows the rebuilt catalog using isolated, read-only HTTP test fixtures. The temporary fixture gateway was on :8088 and the Angular test preview on :3002. These are illustrative test records, not production data or evidence of real backend integration. The application source has no mock-data switch.

Public catalog sorting/pagination, direct product refresh and category navigation were checked in the browser. Registration validation and mode switching, the mobile menu and keyboard skip-link focus were also checked. Actual CSS viewport widths inspected were 1440, 960, 390 and 409; no horizontal overflow was observed in those inspected pages. The capture is a browser screenshot, not generated artwork.

The normal frontend at :3000 continues to use the actual gateway :8080 and Keycloak :8090. The backend stack was unavailable during this task; see REBUILD_NOTES.md for the complete verification status.

`css-refactor-mobile.jpg` records the actual registration page after the CSS architecture refactor. It uses no fixture records. The refactor preserved all 132 sampled desktop computed-style values; mobile form/validation and catalog controls were checked at CSS width 390 without horizontal overflow.

The catalog capture was refreshed for the text-only product layout. `product-details-text-only-fixtures.jpg` shows the corresponding desktop details and purchase blocks, using the same isolated fixture API. Neither screenshot contains product-image placeholders. These captures verify presentation; they do not claim live backend integration.

## Template and SCSS migration — 2026-11-14

`scss-migration-catalog-fixtures.jpg` shows the production catalog after template extraction and the Sass migration. `scss-migration-browser-report.json` records 24 before/after comparisons against a production build of the original source. Both builds used identical GET-only fixtures on temporary local servers; no fixture mode was added to the application.

The catalog was compared at widths 390, 800, 1280 and 1440. Product, category and shop details, directories, customer/merchant registration, login and the missing-page state were compared at 390 and 1280. Empty registration submission and changing the sort control were also compared. Each check compared rendered text, DOM element structure/classes/links, bounding boxes and 26 computed-style properties. All matched, with no horizontal overflow. The final catalog console had no errors.

A browser check caught a real Sass font-import hoisting issue: establishing the `base` layer ahead of the order declaration gave reset rules higher priority. Loading the font with `meta.load-css` after the explicit layer order fixed it. The final compiled global rule/declaration stream matches the original, with unchanged layer order.

These checks cover presentation and client validation. Gateway and Keycloak were stopped, so live authentication, authorized administration and checkout integration were not exercised.
