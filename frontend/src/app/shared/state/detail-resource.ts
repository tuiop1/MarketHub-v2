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
import { loading, requestState } from './request-state';
export function detailResource<T>(fetch: (id: string) => Observable<T>) {
  const route = inject(ActivatedRoute);
  const reload = new Subject<void>();
  const id = route.paramMap.pipe(
    map((params) => params.get('id') ?? ''),
    distinctUntilChanged()
  );
  const state = toSignal(
    combineLatest([id, reload.pipe(startWith(undefined))]).pipe(
      switchMap(([value]) => requestState(fetch(value)))
    ),
    { initialValue: loading<T>() }
  );
  return { state, reload: () => reload.next() };
}
