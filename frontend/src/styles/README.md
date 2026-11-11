# CSS architecture

The application uses plain CSS, Angular's default emulated component encapsulation and explicit native cascade layers. No Sass, CSS framework, JavaScript theme manager or additional build plugin is needed.

## Entry point and ownership

```text
src/styles.css                         Angular CLI entry; imports main.css only
src/styles/
  main.css                             declares layers and import order
  tokens.css                           shared design values, no element styling
  reset.css                            browser normalization, no theme decisions
  base.css                             document, typography, links and focus defaults
  layout.css                           reusable page compositions and grids
  components/
    buttons.css                        .button and its explicit variants
    forms.css                          controls, labels, validation, form surface, sorting
    notices.css                        reusable operation feedback
    badges.css                         state badges
    navigation.css                     reusable tab navigation
    tables.css                         tables scoped to .table-wrap
    directory.css                      public category/shop directory pattern
  utilities.css                        small, explicit u-* helpers
  accessibility.css                    reduced-motion preference
src/app/**/name.component.css           CSS owned by that Angular component
src/app/**/name.page.css                CSS owned by that routed page
```

`styles.css` is kept as the CLI entry to preserve the existing Angular configuration. `main.css` is the sole composition root: importing these globals again from a feature would duplicate styles and obscure their precedence.

Component CSS is loaded through `styleUrl`, alongside its TypeScript file. Templates can remain inline when small; CSS does not live in TypeScript or `style` attributes. Components without unique styling reuse shared patterns rather than receiving empty stylesheets. For example, the checkout, product card, order details and dialog have their own files; category and shop directories share the deliberately global directory pattern.

Loading, empty-state and pagination styles belong to their respective shared components, so they are no longer global selectors. The catalog's `.product-grid` and its breakpoints belong to `product-list.page.css`. App navigation, branding, footer and skip link belong to `app.component.css`.

## Cascade contract

The global order is:

```css
@layer tokens, reset, base, layout, components, utilities, accessibility;
```

`main.css` assigns each import to its layer. For normal declarations, a later global layer wins over an earlier one before selector specificity is considered. This is why the sorting control's layout and appearance are kept together in `forms.css`: putting its select width in a lower layout layer would let the generic control width override it.

Angular component styles intentionally stay unlayered and encapsulated. They override the normal layered defaults only inside their component. This makes a card's local typography or a page's special layout independent of the order in which lazy routes load. Do not add layers ad hoc inside components, switch off encapsulation, use `::ng-deep` or import global files into component stylesheets.

No IDs, inline styles or `!important` are used to win ordinary styling conflicts. The exception is the reduced-motion accessibility override, which must suppress component animations and transitions when the user requests it. It does not override theme/layout properties or user-origin important rules.

## Tokens and units

Use semantic colors: `--color-text`, `--color-text-muted`, `--color-action`, `--color-page`, `--color-surface`, and state-specific surface/text pairs. Raw CSS color literals are centralized in `tokens.css`. The existing palette, including placeholder tones, is retained; this refactor is not a redesign.

Typography has one font-family source, size tokens, font-weight tokens, line heights and tracking. Size names such as `--font-size-14` identify the original reference size at a browser default of 16px; their values are **rem**, not fixed px. The existing size range is recorded explicitly to avoid changing the visual hierarchy during an architectural refactor. A future typography redesign should update that scale intentionally rather than add arbitrary per-page sizes.

The root uses `font-size: 100%`, preserving the browser's font preference. Body text defaults to `var(--font-size-14)`. Text, spacing, radii and content constraints use rem. `em` remains useful for icons sized relative to their surrounding text; percentages, fractions, viewport units and `ch` retain their layout meaning. Thin borders/focus strokes stay in px.

The spacing scale uses a 4px reference unit: `--space-1` is 0.25rem, `--space-4` is 1rem, and fractional steps retain the original fine spacing. Repeated spacing should use these tokens. A unique product-art ratio, grid column proportion or component-specific width stays in its owning stylesheet; not every number needs a global token.

Breakpoints remain literal CSS-pixel media-query conditions and live beside the affected layout. CSS custom properties cannot substitute into ordinary media-query conditions, so a `--breakpoint-mobile` token would suggest functionality it does not provide. No preprocessor is introduced solely to share those values. Shared compositions own shared breakpoints; a page owns its specific collapse threshold.

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

References: [Angular component styling](https://angular.dev/guide/components/styling) and [MDN cascade layers](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/At-rules/@layer).
