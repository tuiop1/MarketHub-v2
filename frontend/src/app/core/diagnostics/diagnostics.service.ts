import { HttpHeaders } from '@angular/common/http';
import { inject, Injectable, InjectionToken, signal } from '@angular/core';
import { environment } from '../../../environments/environment';

export const DIAGNOSTICS_AVAILABLE = new InjectionToken<boolean>('Diagnostics available', {
  providedIn: 'root',
  factory: () => environment.diagnostics,
});

export interface DiagnosticEntry {
  readonly id: number;
  readonly method: string;
  readonly path: string;
  readonly state: 'pending' | 'complete' | 'cancelled';
  readonly status: number | null;
  readonly durationMs: number | null;
  readonly cache: 'Hit' | 'Miss' | 'Revalidated' | 'Not reported';
  readonly requestId: string | null;
}

function cacheResult(status: number, headers: HttpHeaders): DiagnosticEntry['cache'] {
  if (status === 304) return 'Revalidated';
  const cache = headers.get('X-Cache') ?? '';
  if (/\bhit\b/i.test(cache)) return 'Hit';
  if (/\bmiss\b/i.test(cache)) return 'Miss';
  return 'Not reported';
}

@Injectable({ providedIn: 'root' })
export class DiagnosticsService {
  readonly available = inject(DIAGNOSTICS_AVAILABLE);
  private readonly enabledState = signal(false);
  private readonly entryState = signal<readonly DiagnosticEntry[]>([]);
  readonly enabled = this.enabledState.asReadonly();
  readonly entries = this.entryState.asReadonly();
  private nextId = 0;

  setEnabled(enabled: boolean): void {
    this.enabledState.set(this.available && enabled);
    this.clear();
  }

  clear(): void {
    this.entryState.set([]);
  }

  start(method: string, url: string): number | null {
    if (!this.available || !this.enabled()) return null;
    const id = ++this.nextId;
    this.entryState.update((entries) =>
      [
        {
          id,
          method,
          path: url.split(/[?#]/)[0],
          state: 'pending' as const,
          status: null,
          durationMs: null,
          cache: 'Not reported' as const,
          requestId: null,
        },
        ...entries,
      ].slice(0, 25)
    );
    return id;
  }

  finish(id: number, status: number, durationMs: number, headers: HttpHeaders): void {
    this.entryState.update((entries) =>
      entries.map((entry) =>
        entry.id === id
          ? {
              ...entry,
              state: 'complete',
              status,
              durationMs: Math.round(durationMs),
              cache: cacheResult(status, headers),
              requestId:
                (headers.get('X-Request-Id') ?? headers.get('X-Correlation-Id'))?.slice(0, 128) ??
                null,
            }
          : entry
      )
    );
  }

  cancel(id: number, durationMs: number): void {
    this.entryState.update((entries) =>
      entries.map((entry) =>
        entry.id === id && entry.state === 'pending'
          ? { ...entry, state: 'cancelled', durationMs: Math.round(durationMs) }
          : entry
      )
    );
  }
}
