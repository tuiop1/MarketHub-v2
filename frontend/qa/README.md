# Visual verification evidence

`catalog-mobile-fixtures.jpg` shows the rebuilt catalog using isolated, read-only HTTP test fixtures. The temporary fixture gateway was on :8088 and the Angular test preview on :3002. These are illustrative test records, not production data or evidence of real backend integration. The application source has no mock-data switch.

Public catalog sorting/pagination, direct product refresh and category navigation were checked in the browser. Registration validation and mode switching, the mobile menu and keyboard skip-link focus were also checked. Actual CSS viewport widths inspected were 1440, 960, 390 and 409; no horizontal overflow was observed in those inspected pages. The capture is a browser screenshot, not generated artwork.

The normal frontend at :3000 continues to use the actual gateway :8080 and Keycloak :8090. The backend stack was unavailable during this task; see REBUILD_NOTES.md for the complete verification status.

`css-refactor-mobile.jpg` records the actual registration page after the CSS architecture refactor. It uses no fixture records. The refactor preserved all 132 sampled desktop computed-style values; mobile form/validation and catalog controls were checked at CSS width 390 without horizontal overflow.
