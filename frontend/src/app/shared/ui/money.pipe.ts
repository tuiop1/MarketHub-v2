import { Pipe, PipeTransform } from '@angular/core';
const formatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(cents: number | null | undefined): string {
    return cents == null ? 'Unavailable' : formatter.format(cents / 100);
  }
}
