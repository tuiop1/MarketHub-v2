import { OrderStatus } from './data-access/orders-api.service';
const labels: Readonly<Record<OrderStatus, string>> = {
  PENDING_RESERVATION: 'Preparing order',
  PENDING_PAYMENT: 'Processing payment',
  PAID: 'Paid',
  PAYMENT_FAILED: 'Payment failed',
  CANCELLED: 'Cancelled',
  COMPENSATION_FAILED: 'Needs attention',
};
export function orderStatusLabel(status: OrderStatus): string {
  return labels[status];
}
