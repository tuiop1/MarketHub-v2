import { inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import {
  Observable,
  Subject,
  combineLatest,
  distinctUntilChanged,
  map,
  startWith,
  switchMap,
} from 'rxjs';
import { Page } from '../models/page';
import { loading, requestState } from './request-state';
export function pagedResource<T>(
  fetch: (page: number, sort: string, view: string) => Observable<Page<T>>,
  sorts: readonly string[],
  defaultSort = sorts[0]
) {
  const route = inject(ActivatedRoute);
  const reload = new Subject<void>();
  const query = route.queryParamMap.pipe(
    map((params) => {
      const candidate = Number(params.get('page') ?? 0);
      const page = Number.isSafeInteger(candidate) && candidate >= 0 ? candidate : 0;
      const requested = params.get('sort') ?? '';
      return {
        page,
        view: params.get('view') ?? '',
        sort: sorts.includes(requested) ? requested : defaultSort,
      };
    }),
    distinctUntilChanged((a, b) => a.page === b.page && a.sort === b.sort && a.view === b.view)
  );
  const state = toSignal(
    combineLatest([query, reload.pipe(startWith(undefined))]).pipe(
      switchMap(([q]) => requestState(fetch(q.page, q.sort, q.view)))
    ),
    { initialValue: loading<Page<T>>() }
  );
  return { state, reload: () => reload.next() };
}
