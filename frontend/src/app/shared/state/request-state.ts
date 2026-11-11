import { DestroyRef, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, catchError, firstValueFrom, map, of, startWith } from 'rxjs';
import { ApiFailure, apiFailure } from '../../core/http/api-error';
export interface LoadState<T> {
  readonly status: 'initial' | 'loading' | 'success' | 'error';
  readonly value: T | null;
  readonly error: ApiFailure | null;
}
export function loading<T>(): LoadState<T> {
  return { status: 'loading', value: null, error: null };
}
export function requestState<T>(request: Observable<T>): Observable<LoadState<T>> {
  return request.pipe(
    map((value) => ({ status: 'success' as const, value, error: null })),
    catchError((error: unknown) =>
      of({ status: 'error' as const, value: null, error: apiFailure(error) })
    ),
    startWith(loading<T>())
  );
}
/** Local mutation state, composed by pages. Never retries a mutation. */
export class ActionState {
  readonly busy = signal(false);
  readonly error = signal<ApiFailure | null>(null);
  readonly message = signal('');
  constructor(private readonly destroyRef: DestroyRef) {}
  async run<T>(request: Observable<T>, success: (value: T) => void | Promise<void>): Promise<void> {
    if (this.busy() || this.destroyRef.destroyed) return;
    this.busy.set(true);
    this.error.set(null);
    this.message.set('');
    try {
      const value = await firstValueFrom(request.pipe(takeUntilDestroyed(this.destroyRef)));
      if (!this.destroyRef.destroyed) await success(value);
    } catch (error: unknown) {
      if (!this.destroyRef.destroyed) this.error.set(apiFailure(error));
    } finally {
      this.busy.set(false);
    }
  }
}
