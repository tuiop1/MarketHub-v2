export type MerchantStatus = 'PENDING' | 'VERIFIED' | 'REJECTED' | 'SUSPENDED';
export interface Merchant {
  readonly id: string;
  readonly shopName: string;
  readonly description: string | null;
  readonly email: string;
  readonly status: MerchantStatus;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly verifiedAt: string | null;
}
export interface Address {
  readonly country: string | null;
  readonly city: string | null;
  readonly street: string | null;
  readonly postalCode: string | null;
  readonly apartment: string | null;
}
export interface Customer {
  readonly id: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly birthDate: string | null;
  readonly email: string;
  readonly address: Address | null;
  readonly enabled: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}
export interface IdentityRegistration {
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string;
  readonly password: string;
}
export interface CustomerRegistration extends IdentityRegistration {
  readonly birthDate: string | null;
  readonly address: Address | null;
}
export interface MerchantRegistration extends IdentityRegistration {
  readonly shopName: string;
  readonly description: string;
}
