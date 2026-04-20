export type ApiErrorPayload = {
  timestamp?: string;
  path?: string;
  traceId?: string | null;
  code?: string;
  message?: string;
  errors?: unknown[] | null;
};

export type AuthTokenResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
};

export type SignupRequest = {
  email: string;
  password: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type EmailExistsResponse = {
  email: string;
  exists: boolean;
};

export type UserDetailResponse = {
  userId: number;
  email: string;
  status: string;
  createdAt: string;
  updatedAt: string;
};

export type DeliveryPolicy = {
  minOrderAmount: number;
  deliveryTip: number;
};

export type CreateOwnerStoreRequest = {
  name: string;
  categoryIds?: number[];
  deliveryPolicy?: DeliveryPolicy;
  storeLogoUrl?: string | null;
};

export type UpdateOwnerStoreRequest = {
  name: string;
  categoryIds?: number[];
  deliveryPolicy?: DeliveryPolicy;
  storeLogoUrl?: string | null;
};

export type CreateStoreResponse = {
  storeId: number;
  ownerUserId: number;
  name: string;
  status: string;
  statusOverride: boolean;
  minOrderAmount: number;
  deliveryTip: number;
  storeLogoUrl: string | null;
  createdAt: string;
};

export type StoreDetailResponse = {
  storeId: number;
  ownerUserId: number;
  name: string;
  status: string;
  statusOverride: boolean;
  minOrderAmount: number;
  deliveryTip: number;
  storeLogoUrl: string | null;
  createdAt: string;
  updatedAt: string;
};

export type OwnerStoreSummaryResponse = {
  storeId: number;
  name: string;
  status: string;
  minOrderAmount: number;
  deliveryTip: number;
  storeLogoUrl: string | null;
  createdAt: string;
};

export type StoreOrderResponse = {
  orderId: number;
  storeId: number;
  userId: number | null;
  finalAmount: number | null;
  status: string;
  rejectReason: string | null;
  paidAt: string | null;
  decidedAt: string | null;
  createdAt: string;
};

export type StoreOrderDecisionResponse = {
  orderId: number;
  storeId: number;
  status: string;
  rejectReason: string | null;
  decidedAt: string | null;
};

export type CreateMenuGroupRequest = {
  name: string;
  displayOrder: number;
};

export type UpdateMenuGroupRequest = {
  name: string;
  displayOrder: number;
};

export type MenuGroupResponse = {
  id: number;
  name: string;
};

export type CreateMenuRequest = {
  menuGroupId?: number | null;
  name: string;
  description?: string | null;
  price: number;
  isAvailable?: boolean;
  imageUrl?: string | null;
  displayOrder?: number;
};

export type MenuResponse = {
  id: number;
  storeId: number;
  menuGroupId: number | null;
  name: string;
  description: string | null;
  price: number;
  isAvailable: boolean;
  isSoldOut: boolean;
  imageUrl: string | null;
};

export type UpdateMenuRequest = {
  menuGroupId?: number | null;
  name: string;
  description?: string | null;
  price: number;
  isAvailable: boolean;
  isSoldOut: boolean;
  imageUrl?: string | null;
};

export type StoreSearchItem = {
  storeId: number;
  name: string;
  status: string;
  deliveryPolicy: DeliveryPolicy;
  storeLogoUrl: string | null;
  matchedBy: string[];
};

export type StoreSearchPageResponse = {
  content: StoreSearchItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type StoreCategoryOption = {
  id: number;
  name: string;
};

export type StoreDetailQueryCategory = {
  id: number;
  name: string;
};

export type StoreDetailQueryOption = {
  name: string;
  extraPrice: number;
  isAvailable: boolean;
};

export type StoreDetailQueryOptionGroup = {
  name: string;
  isRequired: boolean;
  isMultiple: boolean;
  minSelectCount: number;
  maxSelectCount: number;
  options: StoreDetailQueryOption[];
};

export type StoreDetailQueryTag = {
  id: number;
  name: string;
};

export type StoreDetailQueryMenu = {
  id: number;
  name: string;
  description: string | null;
  price: number;
  isAvailable: boolean;
  isSoldOut: boolean;
  imageUrl: string | null;
  tags?: StoreDetailQueryTag[];
  optionGroups?: StoreDetailQueryOptionGroup[];
};

export type StoreDetailQueryMenuGroup = {
  id: number;
  name: string;
  menus: StoreDetailQueryMenu[];
};

export type StoreDetailQueryResponse = {
  storeId: number;
  ownerUserId: number;
  name: string;
  status: string;
  deliveryPolicy: DeliveryPolicy;
  images: {
    storeLogoUrl: string | null;
  };
  categories: StoreDetailQueryCategory[];
  operatingHours: {
    dayOfWeek: number;
    openTime: string | null;
    closeTime: string | null;
  }[];
  holidays: {
    date: string;
    reason: string | null;
  }[];
  menuGroups: StoreDetailQueryMenuGroup[];
};

export type CreateOrderItemRequest = {
  menuId: number;
  menuName: string;
  unitPrice: number;
  quantity: number;
};

export type CreateOrderRequest = {
  userId: number;
  storeId: number;
  deliveryAddress: string;
  usedPointAmount: number;
  items: CreateOrderItemRequest[];
};

export type CreateOrderResponse = {
  orderId: number;
  userId: number;
  storeId: number;
  status: string;
  deliveryAddress: string;
  totalAmount: number;
  usedPointAmount: number;
  finalAmount: number;
  createdAt: string;
};

export type OrderItemResponse = {
  menuId: number;
  menuName: string;
  unitPrice: number;
  quantity: number;
  lineAmount: number;
};

export type OrderDetailResponse = {
  orderId: number;
  userId: number;
  storeId: number;
  status: string;
  deliveryAddress: string;
  totalAmount: number;
  usedPointAmount: number;
  finalAmount: number;
  items: OrderItemResponse[];
  createdAt: string;
  updatedAt: string;
};

export type JwtPayload = {
  sub?: string;
  user_id?: number;
  email?: string;
  nickname?: string;
  role?: string;
  type?: string;
  iat?: number;
  exp?: number;
  iss?: string;
  jti?: string;
};

export type TokenBundle = AuthTokenResponse & {
  issuedAt: number;
};
