# SCSS architecture

The application uses SCSS through Angular's bundled Dart Sass compiler, Angular's default emulated component encapsulation and native CSS cascade layers. No new dependency or custom build plugin is required. SCSS organizes and compiles the styles; the browser still receives ordinary CSS.

## Entry point and ownership

```text
src/styles.scss                        Angular CLI entry; @use './styles/main'
src/styles/
  main.scss                            composition root and cascade-layer order
  _tokens.scss                         shared runtime CSS custom properties
  _reset.scss                          browser normalization, no theme decisions
  _base.scss                           typography, element and focus defaults
  _layout.scss                         reusable compositions and grids
  components/
    _buttons.scss                      buttons and their explicit variants
    _forms.scss                        controls, labels, validation and form surfaces
    _notices.scss                      operation feedback
    _badges.scss                       state badges
    _navigation.scss                   shared tab navigation
    _tables.scss                       tables scoped to .table-wrap
    _directory.scss                    shared category/shop directory pattern
  _utilities.scss                      small u-* helpers
  _accessibility.scss                  reduced-motion preference
  tools/
    _media.scss                        shared breakpoint map and media mixins; emits no CSS
src/app/**/name.component.html          referenced with templateUrl
src/app/**/name.component.scss          component-owned styles referenced with styleUrl
src/app/**/name.page.html               routed page template
src/app/**/name.page.scss               page-owned styles when needed
```

Every component template, including the small routing-test fixtures, is external HTML. The three small feedback/loading/empty components share a TypeScript module and each reference their own HTML file. Components without unique styling reuse shared patterns rather than receiving empty stylesheets. No inline `template`, decorator styles or HTML style attributes remain.

Angular's component generator defaults to `inlineTemplate: false`, `inlineStyle: false` and `style: scss` in `angular.json`. New components therefore follow the same file separation automatically. The formatter includes SCSS and HTML, with the Angular parser applied to every app template, including `.page.html` and test fixtures.

## Modules and cascade contract

The global layer order remains:

```scss
@layer tokens, reset, base, layout, components, utilities, accessibility;
```

`styles.scss` loads the composition root with `@use`. `main.scss` uses the built-in `sass:meta` module to place each CSS-emitting partial inside its original layer:

```scss
@use 'sass:meta';

@layer tokens {
  @include meta.load-css('tokens');
}
```

The underscore marks a Sass partial. Imports omit its underscore and extension. `meta.load-css` is used here because ordinary `@use` must be top-level, while these shared rules must be emitted inside a specific cascade layer. Shared patterns are loaded only by the composition root. Importing a global partial from a feature would duplicate CSS and obscure ownership.

The Fontsource package is loaded with `meta.load-css` inside `@layer base`, after the layer-order declaration. Angular bundles the font assets locally. Do not replace this with a native CSS `@import`: Sass hoists that import above the layer declaration, establishing `base` before `reset` and changing the cascade. Application SCSS uses `@use` and `meta.load-css` throughout.

Angular component styles intentionally remain unlayered and encapsulated, preserving their existing priority over normal layered defaults. No IDs, inline styles, `::ng-deep`, disabled encapsulation or `!important` are used to win normal conflicts. The reduced-motion accessibility override is the deliberate exception: it suppresses motion when requested without altering the theme or user-origin preferences.

For normal declarations, a later global layer takes priority before selector specificity. Keep a shared control's layout and appearance in its owning pattern: for example, the sorting control stays with generic controls in `_forms.scss` so its intended width is not overridden by a higher layer.

## Responsive rules and nesting

Repeated breakpoint values now live in the CSS-free `tools/_media.scss` module:

| Key        | CSS width | Use                                   |
| ---------- | --------- | ------------------------------------- |
| phone      | 480px     | single-column narrow layouts          |
| compact    | 600px     | compact cart/order layouts            |
| tablet     | 700px     | shared grids and compact spacing      |
| navigation | 800px     | navigation and detail layout collapse |
| admin      | 950px     | category administration split         |
| split      | 1000px    | general two-column layout collapse    |
| workspace  | 1050px    | merchant workspace and header spacing |
| wide       | 1300px    | four-column catalog                   |

A component imports only the helper, using a relative path:

```scss
@use '../../../styles/tools/media' as media;

.product-details {
  display: grid;
  grid-template-columns: 1.4fr 0.7fr;
}

@include media.down('navigation') {
  .product-details {
    grid-template-columns: 1fr;
  }
}
```

`media.up('wide')` emits an inclusive min-width query; `media.down('phone')` emits an inclusive max-width query. These preserve the pre-migration pixel thresholds. An unknown key causes a compile-time error. The module produces no selectors, so it is safe to use in encapsulated component styles without copying global CSS.

Use shallow nesting for a root's own modifiers, such as `&.primary` or `&.selected`. Keep selector scope and specificity stable; a grouped nested selector must use `&` for each root it represents. Avoid nesting just to make SCSS look different, deep DOM chains and `@extend` coupling unrelated components.

## Tokens and units

Use semantic colors: `--color-text`, `--color-text-muted`, `--color-action`, `--color-page`, `--color-surface`, and state-specific surface/text pairs. Raw CSS color literals are centralized in `_tokens.scss`. The shared palette is retained. Product views use text-only blocks; obsolete product-art color tokens have been removed.

Typography has one font-family source, size tokens, font-weight tokens, line heights and tracking. Size names such as `--font-size-14` identify the original reference size at a browser default of 16px; their values are **rem**, not fixed px. The existing size range is recorded explicitly to avoid changing the visual hierarchy during an architectural refactor. A future typography redesign should update that scale intentionally rather than add arbitrary per-page sizes.

The root uses `font-size: 100%`, preserving the browser's font preference. Body text defaults to `var(--font-size-14)`. Text, spacing, radii and content constraints use rem. `em` remains useful for icons sized relative to their surrounding text; percentages, fractions, viewport units and `ch` retain their layout meaning. Thin borders/focus strokes stay in px.

The spacing scale uses a 4px reference unit: `--space-1` is 0.25rem, `--space-4` is 1rem, and fractional steps retain the original fine spacing. Repeated spacing should use these tokens. A unique grid column proportion or component-specific width stays in its owning stylesheet; not every number needs a global token.

Breakpoint values remain CSS-pixel values in the Sass map. CSS custom properties still cannot substitute into ordinary media-query conditions; Sass now supplies those compile-time values through the shared helper. Responsive rules stay with their owning component or shared composition.

## Naming and selector scope

- Shared visual contracts have explicit roots: `.button.primary`, `.notice.error`, `.badge.good`, `.tab-nav` and `.table-wrap`.
- Tiny helpers use the `u-` prefix: `.u-text-muted`, `.u-text-small`, `.u-money`, `.u-span-full`, `.u-visually-hidden`. They carry no feature state or workflow behavior.
- Local selectors describe the component's content, such as `.shop-description`, `.registration-actions`, `.order-line` and `.category-pagination`. Angular encapsulation supplies their scope; a second naming framework is unnecessary.
- Table element rules are scoped beneath `.table-wrap`. A newly introduced table outside this contract does not inherit an administrative table skin by accident.
- Global input/label defaults are an intentional application-wide form contract. Feature-specific control arrangements belong locally.
- Prefer short selectors and a root/modifier pair. Do not reach across child component boundaries. Pass inputs or expose a deliberate CSS custom property if a real styling extension is required.

## Adding or changing styling

1. Change a token when the design value changes consistently across the application.
2. Change a shared pattern when its visual behavior is shared by multiple owners.
3. Change a colocated stylesheet for a page/component-specific adjustment.
4. Use a utility only for one of the documented single-purpose concerns; do not create a utility for every margin value.
5. Keep responsive rules with their owner, and verify focus, disabled, loading, error and empty states.

Run `npm run format:check`, `npm run build` and the existing component tests. Browser checks are still required for cascade/encapsulation issues because DOM unit tests do not prove visual equivalence.

References: [Sass modules](https://sass-lang.com/documentation/at-rules/use/), [sass:meta.load-css](https://sass-lang.com/documentation/modules/meta/#load-css), [Angular component styling](https://angular.dev/guide/components/styling) and [MDN cascade layers](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/At-rules/@layer).
