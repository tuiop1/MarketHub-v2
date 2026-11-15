import { HttpHeaders } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { AuthService } from '../auth/auth.service';

export interface DiagnosticEntry {
  readonly id: number;
  readonly method: string;
  readonly path: string;
  readonly state: 'pending' | 'complete' | 'cancelled';
  readonly status: number | null;
  readonly durationMs: number | null;
  readonly cachePolicy: 'Eligible (up to 30 min)' | 'Not configured';
  readonly requestId: string | null;
}

function cachePolicyFor(method: string, path: string): DiagnosticEntry['cachePolicy'] {
  // Mirrors catalog-service @Cacheable detail lookups and RedisConfig's 30-minute TTL.
  // This describes eligibility only; it does not inspect a response or Redis.
  const detailPath =
    /^\/api\/v1\/(products|categories)\/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$/;
  return method === 'GET' && detailPath.test(path) ? 'Eligible (up to 30 min)' : 'Not configured';
}

@Injectable({ providedIn: 'root' })
export class DiagnosticsService {
  private readonly auth = inject(AuthService);
  readonly available = computed(() => this.auth.hasRole('ADMIN'));
  private readonly enabledState = signal(false);
  private readonly entryState = signal<readonly DiagnosticEntry[]>([]);
  readonly enabled = computed(() => this.available() && this.enabledState());
  readonly entries = computed(() => (this.enabled() ? this.entryState() : []));
  private nextId = 0;

  constructor() {
    effect(() => {
      if (!this.available()) this.setEnabled(false);
    });
  }

  setEnabled(enabled: boolean): void {
    this.enabledState.set(this.available() && enabled);
    this.clear();
  }

  clear(): void {
    this.entryState.set([]);
  }

  start(method: string, url: string): number | null {
    if (!this.enabled()) return null;
    const id = ++this.nextId;
    const path = url.split(/[?#]/)[0];
    this.entryState.update((entries) =>
      [
        {
          id,
          method,
          path,
          state: 'pending' as const,
          status: null,
          durationMs: null,
          cachePolicy: cachePolicyFor(method, path),
          requestId: null,
        },
        ...entries,
      ].slice(0, 25)
    );
    return id;
  }

  finish(id: number, status: number, durationMs: number, headers: HttpHeaders): void {
    if (!this.enabled()) return;
    this.entryState.update((entries) =>
      entries.map((entry) =>
        entry.id === id
          ? {
              ...entry,
              state: 'complete',
              status,
              durationMs: Math.round(durationMs),
              requestId:
                (headers.get('X-Request-Id') ?? headers.get('X-Correlation-Id'))?.slice(0, 128) ??
                null,
            }
          : entry
      )
    );
  }

  cancel(id: number, durationMs: number): void {
    if (!this.enabled()) return;
    this.entryState.update((entries) =>
      entries.map((entry) =>
        entry.id === id && entry.state === 'pending'
          ? { ...entry, state: 'cancelled', durationMs: Math.round(durationMs) }
          : entry
      )
    );
  }
}
