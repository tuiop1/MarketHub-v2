import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, firstValueFrom, Observable, of, throwError } from 'rxjs';
import {
  ApiService,
  Cart,
  Category,
  Customer,
  Merchant,
  MerchantAction,
  Order,
  Page,
  PaymentMethod,
  Product,
  ProductInput
} from './api.service';
import { AuthService } from './auth.service';

type View = 'shop' | 'merchants' | 'register' | 'merchant' | 'admin' | 'cart' | 'orders' | 'account';
type AdminSection = 'categories' | 'pending-merchants' | 'verified-merchants';

const CUSTOMER_PRESETS = [
  {
    firstName: 'Lena', lastName: 'Fischer', birthDate: '1994-05-16',
    country: 'Germany', city: 'Berlin', street: 'Lindenstrasse 18', postalCode: '10969', apartment: '4B'
  },
  {
    firstName: 'Noah', lastName: 'Bennett', birthDate: '1991-09-08',
    country: 'United Kingdom', city: 'Bristol', street: 'Oakfield Road 27', postalCode: 'BS8 2AT', apartment: '12'
  },
  {
    firstName: 'Sofia', lastName: 'Marin', birthDate: '1996-02-21',
    country: 'Spain', city: 'Valencia', street: 'Carrer de la Pau 42', postalCode: '46003', apartment: '3A'
  },
  {
    firstName: 'Milan', lastName: 'Petrovic', birthDate: '1989-11-30',
    country: 'Serbia', city: 'Novi Sad', street: 'Dunavska 11', postalCode: '21000', apartment: '7'
  }
] as const;

const MERCHANT_PRESETS = [
  {
    firstName: 'Mia', lastName: 'Wilson', shopName: 'Good Shop',
    description: 'Friendly everyday essentials selected for useful, uncomplicated living.'
  },
  {
    firstName: 'Oliver', lastName: 'King', shopName: 'North & Pine',
    description: 'Thoughtful home goods, desk accessories, and practical gifts.'
  },
  {
    firstName: 'Amira', lastName: 'Haddad', shopName: 'Bright Basket',
    description: 'Colorful household favorites and reliable products for busy families.'
  },
  {
    firstName: 'Lucas', lastName: 'Moreau', shopName: 'Common Goods',
    description: 'Simple products with honest prices, chosen for daily use.'
  }
] as const;

const CATEGORY_PRESETS = [
  { name: 'Household', description: 'Practical products for everyday life around the home.' },
  { name: 'Books & Stationery', description: 'Books, notebooks, writing tools, and desk essentials.' },
  { name: 'Electronics', description: 'Useful personal electronics and reliable accessories.' },
  { name: 'Sports & Outdoors', description: 'Everyday equipment for movement, training, and time outside.' }
] as const;

const PRODUCT_PRESETS = [
  {
    name: 'Ceramic Storage Jar', description: 'A durable kitchen jar with a simple airtight bamboo lid.',
    priceCents: 1899, stockQuantity: 24
  },
  {
    name: 'Compact Desk Lamp', description: 'A warm LED desk lamp with three brightness settings.',
    priceCents: 3499, stockQuantity: 15
  },
  {
    name: 'Recycled Notebook Set', description: 'Three soft-cover notebooks made with recycled paper.',
    priceCents: 1299, stockQuantity: 40
  },
  {
    name: 'Everyday Yoga Mat', description: 'A comfortable non-slip mat for stretching and home workouts.',
    priceCents: 2799, stockQuantity: 18
  }
] as const;

const randomPresetIndex = (length: number): number => {
  const value = new Uint32Array(1);
  crypto.getRandomValues(value);
  return value[0] % length;
};

const emptyPage = <T>(): Page<T> => ({
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 10,
  first: true,
  last: true
});

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  readonly view = signal<View>('shop');
  readonly adminSection = signal<AdminSection>('categories');
  readonly activeRequests = signal(0);
  readonly busy = computed(() => this.activeRequests() > 0);
  readonly result = signal('Ready.');

  readonly categories = signal<Category[]>([]);
  readonly productPage = signal<Page<Product>>(emptyPage());
  readonly merchantDirectoryPage = signal<Page<Merchant>>(emptyPage());
  readonly merchantProductPage = signal<Page<Product>>(emptyPage());
  readonly adminCategoryPage = signal<Page<Category>>(emptyPage());
  readonly pendingMerchantPage = signal<Page<Merchant>>(emptyPage());
  readonly verifiedMerchantPage = signal<Page<Merchant>>(emptyPage());
  readonly orderPage = signal<Page<Order>>(emptyPage());

  readonly selectedCategory = signal<Category | null>(null);
  readonly selectedProduct = signal<Product | null>(null);
  readonly selectedMerchant = signal<Merchant | null>(null);
  readonly expandedOrderId = signal<string | null>(null);
  readonly selectedQuickOrderIds = signal<string[]>([]);
  readonly cart = signal<Cart | null>(null);
  readonly customerProfile = signal<Customer | null>(null);
  readonly merchantProfile = signal<Merchant | null>(null);

  readonly productPageSize = 12;
  readonly adminPageSize = 10;
  readonly orderPageSize = 10;

  productSort = 'name,asc';
  merchantSort = 'shopName,asc';
  paymentMethod: PaymentMethod = 'CARD';
  productQuantities: Record<string, number> = {};

  private customerPresetIndex = randomPresetIndex(CUSTOMER_PRESETS.length);
  private merchantPresetIndex = randomPresetIndex(MERCHANT_PRESETS.length);
  private categoryPresetIndex = randomPresetIndex(CATEGORY_PRESETS.length);
  private productPresetIndex = randomPresetIndex(PRODUCT_PRESETS.length);

  categoryForm: { name: string; description: string } = this.newCategoryForm();
  editingCategoryId: string | null = null;

  productForm: ProductInput & { active: boolean } = this.newProductForm();
  editingProductId: string | null = null;

  customerForm = this.newCustomerForm();
  merchantForm = this.newMerchantForm();

  constructor(readonly auth: AuthService, readonly api: ApiService) {}

  async ngOnInit(): Promise<void> {
    const params = new URLSearchParams(location.search);
    const code = params.get('code');
    const state = params.get('state');
    const error = params.get('error');

    if (error) {
      this.auth.clearLoginAttempt();
      this.result.set(`Login failed: ${params.get('error_description') ?? error}`);
    }
    if (code) {
      await this.perform('Complete Keycloak login', this.auth.completeLogin(code, state));
    } else if (location.pathname === '/callback') {
      history.replaceState({}, '', '/');
    }

    await this.loadCategories();
    await this.loadProducts(0);
  }

  show(view: View): void {
    this.view.set(view);
    this.selectedCategory.set(null);
    this.selectedProduct.set(null);
    this.selectedMerchant.set(null);

    if (view === 'shop') void this.loadProducts(this.productPage().number);
    if (view === 'merchants') void this.loadMerchantDirectory(0);
    if (view === 'merchant') void this.loadMerchantProducts(0);
    if (view === 'admin') void this.loadAdminSection(this.adminSection(), 0);
    if (view === 'cart') void this.loadCart();
    if (view === 'orders') void this.loadOrders(0);
    if (view === 'account') void this.loadMe();
  }

  async loadCategories(): Promise<void> {
    await this.call('Load categories', this.api.categories(0, 100), page => {
      this.categories.set(page.content);
      if (!this.productForm.categoryId && page.content[0]) {
        this.productForm.categoryId = page.content[0].id;
      }
    }, false);
  }

  async loadProducts(page: number): Promise<void> {
    await this.call(
      'Load products',
      this.api.products(page, this.productPageSize, this.productSort),
      value => {
        this.productPage.set(value);
        const visibleIds = new Set(value.content.map(product => product.id));
        this.selectedQuickOrderIds.update(ids => ids.filter(id => visibleIds.has(id)));
        value.content.forEach(product => {
          if (!this.productQuantities[product.id]) this.productQuantities[product.id] = 1;
        });
      },
      false
    );
  }

  async openProduct(productId: string): Promise<void> {
    this.selectedCategory.set(null);
    await this.call('Load product details', this.api.product(productId), product => {
      this.selectedProduct.set(product);
      if (!this.productQuantities[product.id]) this.productQuantities[product.id] = 1;
    }, false);
  }

  async openCategory(categoryId: string): Promise<void> {
    this.selectedProduct.set(null);
    await this.call('Load category details', this.api.category(categoryId), category => {
      this.selectedCategory.set(category);
    }, false);
  }

  closeCategory(): void {
    this.selectedCategory.set(null);
  }

  closeProduct(): void {
    this.selectedProduct.set(null);
  }

  async loadMerchantDirectory(page: number): Promise<void> {
    await this.call(
      'Load merchants',
      this.api.merchants(page, this.productPageSize, this.merchantSort),
      value => this.merchantDirectoryPage.set(value),
      false
    );
  }

  async openMerchant(id: string): Promise<void> {
    await this.call('Load merchant details', this.api.merchant(id), value => {
      this.selectedMerchant.set(value);
    }, false);
  }

  async registerCustomer(): Promise<void> {
    const form = this.customerForm;
    await this.call('Register customer', this.api.registerCustomer({
      firstName: form.firstName,
      lastName: form.lastName,
      email: form.email,
      password: form.password,
      birthDate: form.birthDate,
      address: {
        country: form.country,
        city: form.city,
        street: form.street,
        postalCode: form.postalCode,
        apartment: form.apartment
      }
    }), () => {
      this.result.set('Customer registration succeeded. You can now log in.');
      this.useAnotherCustomerExample();
    });
  }

  async registerMerchant(): Promise<void> {
    await this.call('Register merchant', this.api.registerMerchant(this.merchantForm), () => {
      this.result.set('Merchant registration succeeded. An administrator must verify the account before merchant access is available.');
      this.useAnotherMerchantExample();
    });
  }

  useAnotherCustomerExample(): void {
    this.customerPresetIndex = (this.customerPresetIndex + 1) % CUSTOMER_PRESETS.length;
    this.customerForm = this.newCustomerForm();
  }

  useAnotherMerchantExample(): void {
    this.merchantPresetIndex = (this.merchantPresetIndex + 1) % MERCHANT_PRESETS.length;
    this.merchantForm = this.newMerchantForm();
  }

  useAnotherCategoryExample(): void {
    this.editingCategoryId = null;
    this.categoryPresetIndex = (this.categoryPresetIndex + 1) % CATEGORY_PRESETS.length;
    this.categoryForm = this.newCategoryForm();
  }

  useAnotherProductExample(): void {
    this.editingProductId = null;
    this.productPresetIndex = (this.productPresetIndex + 1) % PRODUCT_PRESETS.length;
    this.productForm = this.newProductForm();
  }

  async loadMe(): Promise<void> {
    this.customerProfile.set(null);
    this.merchantProfile.set(null);

    if (this.auth.hasRole('CUSTOMER')) {
      await this.call('Load customer profile', this.api.customerMe(), value => this.customerProfile.set(value), false);
    } else if (this.auth.roles().some(role => role.startsWith('MERCHANT'))) {
      await this.call('Load merchant profile', this.api.merchantMe(), value => this.merchantProfile.set(value), false);
    }
  }

  selectAdminSection(section: AdminSection): void {
    this.adminSection.set(section);
    void this.loadAdminSection(section, 0);
  }

  async loadAdminSection(section: AdminSection, page: number): Promise<void> {
    if (section === 'categories') await this.loadAdminCategories(page);
    if (section === 'pending-merchants') await this.loadPendingMerchants(page);
    if (section === 'verified-merchants') await this.loadVerifiedMerchants(page);
  }

  async loadAdminCategories(page: number): Promise<void> {
    await this.call(
      'Load categories',
      this.api.adminCategories(page, this.adminPageSize),
      value => this.adminCategoryPage.set(value),
      false
    );
  }

  editCategory(category: Category): void {
    this.editingCategoryId = category.id;
    this.categoryForm = { name: category.name, description: category.description ?? '' };
  }

  cancelCategoryEdit(): void {
    this.editingCategoryId = null;
    this.useAnotherCategoryExample();
  }

  async saveCategory(): Promise<void> {
    const request = { ...this.categoryForm };
    const operation = this.editingCategoryId
      ? this.api.updateCategory({ id: this.editingCategoryId, ...request })
      : this.api.createCategory(request);

    await this.call(this.editingCategoryId ? 'Update category' : 'Create category', operation, async () => {
      this.cancelCategoryEdit();
      await this.loadCategories();
      await this.loadAdminCategories(this.adminCategoryPage().number);
      await this.loadProducts(this.productPage().number);
    });
  }

  async toggleCategory(category: Category): Promise<void> {
    await this.call(
      `${category.active ? 'Disable' : 'Enable'} category`,
      this.api.setCategoryEnabled(category.id, !category.active),
      async () => {
        await this.loadCategories();
        await this.loadAdminCategories(this.adminCategoryPage().number);
        await this.loadProducts(0);
      }
    );
  }

  async loadPendingMerchants(page: number): Promise<void> {
    await this.call(
      'Load pending merchants',
      this.api.unverifiedMerchants(page, this.adminPageSize),
      value => this.pendingMerchantPage.set(value),
      false
    );
  }

  async loadVerifiedMerchants(page: number): Promise<void> {
    await this.call(
      'Load verified merchants',
      this.api.merchants(page, this.adminPageSize),
      value => this.verifiedMerchantPage.set(value),
      false
    );
  }

  async merchantAction(id: string, action: MerchantAction): Promise<void> {
    await this.call(`${action} merchant`, this.api.merchantAction(id, action), async () => {
      await this.loadPendingMerchants(this.pendingMerchantPage().number);
      await this.loadVerifiedMerchants(this.verifiedMerchantPage().number);
    });
  }

  async loadMerchantProducts(page: number): Promise<void> {
    await this.call(
      'Load merchant products',
      this.api.merchantProducts(page, this.adminPageSize),
      value => this.merchantProductPage.set(value),
      false
    );
  }

  editProduct(product: Product): void {
    this.editingProductId = product.id;
    this.productForm = {
      name: product.name,
      description: product.description ?? '',
      priceCents: product.priceCents,
      stockQuantity: product.stockQuantity,
      categoryId: product.categoryId,
      active: product.active
    };
  }

  cancelProductEdit(): void {
    this.editingProductId = null;
    this.useAnotherProductExample();
  }

  async saveProduct(): Promise<void> {
    const operation = this.editingProductId
      ? this.api.updateProduct({ id: this.editingProductId, ...this.productForm })
      : this.api.createProduct(this.productForm);

    await this.call(this.editingProductId ? 'Update product' : 'Create product', operation, async () => {
      this.cancelProductEdit();
      await this.loadMerchantProducts(this.merchantProductPage().number);
      await this.loadProducts(this.productPage().number);
    });
  }

  async toggleProduct(product: Product): Promise<void> {
    await this.call(
      `${product.active ? 'Deactivate' : 'Activate'} product`,
      this.api.updateProduct({ ...product, active: !product.active }),
      async () => {
        await this.loadMerchantProducts(this.merchantProductPage().number);
        await this.loadProducts(this.productPage().number);
      }
    );
  }

  async deleteProduct(id: string): Promise<void> {
    if (!confirm('Deactivate this product?')) return;
    await this.call('Deactivate product', this.api.deleteProduct(id), async () => {
      await this.loadMerchantProducts(this.merchantProductPage().number);
      await this.loadProducts(this.productPage().number);
    });
  }

  quantity(product: Product): number {
    const value = Number(this.productQuantities[product.id] ?? 1);
    return Math.max(1, Math.min(product.stockQuantity, Number.isFinite(value) ? value : 1));
  }

  async addToCart(product: Product): Promise<void> {
    await this.call(
      'Add product to cart',
      this.api.addToCart(product.id, this.quantity(product)),
      () => this.loadCart()
    );
  }

  async buyNow(product: Product): Promise<void> {
    await this.call(
      'Purchase product',
      this.api.purchase([{ productId: product.id, quantity: this.quantity(product) }], this.paymentMethod),
      order => {
        this.result.set(`Order ${order.id} completed with status ${order.status}.`);
      }
    );
  }

  toggleQuickOrder(product: Product): void {
    const ids = this.selectedQuickOrderIds();
    this.selectedQuickOrderIds.set(
      ids.includes(product.id) ? ids.filter(id => id !== product.id) : [...ids, product.id]
    );
  }

  quickOrderSelected(productId: string): boolean {
    return this.selectedQuickOrderIds().includes(productId);
  }

  async purchaseQuickOrder(): Promise<void> {
    const productsById = new Map(this.productPage().content.map(product => [product.id, product]));
    const items = this.selectedQuickOrderIds()
      .map(id => productsById.get(id))
      .filter((product): product is Product => !!product)
      .map(product => ({ productId: product.id, quantity: this.quantity(product) }));

    if (!items.length) return;
    await this.call('Purchase selected products', this.api.purchase(items, this.paymentMethod), order => {
      this.selectedQuickOrderIds.set([]);
      this.result.set(`Order ${order.id} completed with status ${order.status}.`);
    });
  }

  async loadCart(): Promise<void> {
    const request = this.api.cart().pipe(catchError(error => {
      if (error instanceof HttpErrorResponse && error.status === 404) return of(null);
      return throwError(() => error);
    }));
    await this.call('Load cart', request, value => this.cart.set(value), false);
  }

  async addOneToCart(item: Cart['cartItems'][number]): Promise<void> {
    await this.call('Increase cart quantity', this.api.addToCart(item.productId, 1), () => this.loadCart());
  }

  async removeCartItem(id: string): Promise<void> {
    await this.call('Remove cart item', this.api.removeCartItem(id), () => this.loadCart());
  }

  async clearCart(): Promise<void> {
    if (!confirm('Remove every item from the cart?')) return;
    await this.call('Clear cart', this.api.clearCart(), () => this.loadCart());
  }

  async checkout(): Promise<void> {
    await this.call('Purchase cart', this.api.purchaseCart(this.paymentMethod), async order => {
      this.result.set(`Order ${order.id} completed with status ${order.status}.`);
      await this.loadCart();
      await this.loadOrders(0);
    });
  }

  async loadOrders(page: number): Promise<void> {
    await this.call(
      'Load orders',
      this.api.orders(page, this.orderPageSize),
      value => this.orderPage.set(value),
      false
    );
  }

  toggleOrderDetails(orderId: string): void {
    this.expandedOrderId.set(this.expandedOrderId() === orderId ? null : orderId);
  }

  money(cents: number | null | undefined): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format((cents ?? 0) / 100);
  }

  categoryName(categoryId: string): string {
    return this.categories().find(category => category.id === categoryId)?.name ?? 'Unknown category';
  }

  private newCustomerForm() {
    return {
      ...CUSTOMER_PRESETS[this.customerPresetIndex],
      email: '',
      password: ''
    };
  }

  private newMerchantForm() {
    return {
      ...MERCHANT_PRESETS[this.merchantPresetIndex],
      email: '',
      password: ''
    };
  }

  private newCategoryForm(): { name: string; description: string } {
    return { ...CATEGORY_PRESETS[this.categoryPresetIndex] };
  }

  private newProductForm(): ProductInput & { active: boolean } {
    const preset = PRODUCT_PRESETS[this.productPresetIndex];
    return {
      ...preset,
      categoryId: this.categories()[0]?.id ?? '',
      active: true
    };
  }

  private async call<T>(
    label: string,
    request: Observable<T>,
    success?: (value: T) => void | Promise<void>,
    show = true
  ): Promise<T | undefined> {
    return this.perform(label, firstValueFrom(request), success, show);
  }

  private async perform<T>(
    label: string,
    promise: Promise<T>,
    success?: (value: T) => void | Promise<void>,
    show = true
  ): Promise<T | undefined> {
    this.activeRequests.update(value => value + 1);
    try {
      const value = await promise;
      if (show) this.result.set(`${label} succeeded.`);
      await success?.(value);
      return value;
    } catch (error) {
      const response = error as HttpErrorResponse;
      this.result.set(
        `${label} failed (${response.status || 'client error'})\n${this.pretty(response.error ?? response.message ?? error)}`
      );
      return undefined;
    } finally {
      this.activeRequests.update(value => Math.max(0, value - 1));
    }
  }

  private pretty(value: unknown): string {
    if (value === null || value === undefined || value === '') return 'No response body';
    return typeof value === 'string' ? value : JSON.stringify(value, null, 2);
  }
}
