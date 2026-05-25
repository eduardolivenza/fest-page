export type ProductCategory = 'BED' | 'MATTRESS' | 'PILLOW' | 'ACCESSORY';

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'DELIVERED' | 'CANCELLED';

export interface ProductImage {
  id: string;
  url: string;
  alt: string | null;
}

export interface ProductSize {
  id: string;
  width: number;
  length: number;
  price: number;
  productId: string;
}

export interface Product {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  price: number;
  category: ProductCategory;
  images: ProductImage[];
  sizes: ProductSize[];
  featured: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface Customer {
  id: string;
  name: string;
  email: string;
  phone: string | null;
  createdAt: Date;
}

export interface OrderItem {
  id: string;
  productId: string;
  product: Pick<Product, 'id' | 'name' | 'price'>;
  quantity: number;
  unitPrice: number;
}

export interface Order {
  id: string;
  customer: Customer;
  items: OrderItem[];
  status: OrderStatus;
  totalAmount: number;
  notes: string | null;
  createdAt: Date;
  updatedAt: Date;
}

export interface ContactRequest {
  id: string;
  name: string;
  email: string;
  phone: string | null;
  message: string;
  read: boolean;
  createdAt: Date;
}

export interface AdminUser {
  id: string;
  email: string;
  name: string;
  createdAt: Date;
}

// API request/response shapes
export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface ApiError {
  message: string;
  code?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: AdminUser;
}

export interface CreateProductRequest {
  name: string;
  slug: string;
  description?: string;
  price: number;
  category: ProductCategory;
  featured?: boolean;
}

export interface CreateContactRequest {
  name: string;
  email: string;
  phone?: string;
  message: string;
}

export interface CreateOrderRequest {
  customerName: string;
  customerEmail: string;
  customerPhone?: string;
  items: Array<{ productId: string; quantity: number }>;
  notes?: string;
}
