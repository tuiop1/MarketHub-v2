import { HttpParams } from '@angular/common/http';
export interface Page<T> {
  readonly content: readonly T[];
  readonly totalElements: number;
  readonly totalPages: number;
  readonly number: number;
  readonly size: number;
  readonly first: boolean;
  readonly last: boolean;
}
export function pageParams(page: number, size: number, sort: string): HttpParams {
  return new HttpParams().set('page', Math.max(0, page)).set('size', size).set('sort', sort);
}
