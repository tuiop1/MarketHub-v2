export interface Category {
  readonly id: string;
  readonly name: string;
  readonly description: string | null;
  readonly active: boolean;
  readonly createdAt: string;
}
export interface Product {
  readonly id: string;
  readonly merchantId: string;
  readonly categoryId: string;
  readonly name: string;
  readonly description: string | null;
  readonly priceCents: number;
  readonly stockQuantity: number;
  readonly active: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}
export interface ProductCreate {
  readonly name: string;
  readonly description: string;
  readonly priceCents: number;
  readonly stockQuantity: number;
  readonly categoryId: string;
}
export interface ProductUpdate extends ProductCreate {
  readonly active: boolean;
}
