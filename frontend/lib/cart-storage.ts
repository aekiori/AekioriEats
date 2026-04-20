export type CartOption = {
  groupName: string;
  optionName: string;
  extraPrice: number;
};

export type CartItem = {
  cartItemId: string;
  storeId: number;
  storeName: string;
  menuId: number;
  menuName: string;
  basePrice: number;
  unitPrice: number;
  quantity: number;
  imageUrl: string | null;
  options: CartOption[];
};

export type CartSnapshot = {
  storeId: number;
  storeName: string;
  items: CartItem[];
  updatedAt: number;
};

export type AddCartItemInput = Omit<CartItem, "cartItemId">;

const CART_STORAGE_KEY = "aekiori.delivery.cart";

function isBrowser(): boolean {
  return typeof window !== "undefined";
}

export function getCart(): CartSnapshot | null {
  if (!isBrowser()) return null;

  const raw = window.localStorage.getItem(CART_STORAGE_KEY);
  if (!raw) return null;

  try {
    const parsed = JSON.parse(raw) as CartSnapshot;
    if (!parsed.storeId || !Array.isArray(parsed.items)) {
      window.localStorage.removeItem(CART_STORAGE_KEY);
      return null;
    }
    return parsed;
  } catch {
    window.localStorage.removeItem(CART_STORAGE_KEY);
    return null;
  }
}

export function saveCart(cart: CartSnapshot): CartSnapshot {
  if (isBrowser()) {
    window.localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cart));
  }
  window.dispatchEvent(new Event("aekiori-cart-updated"));
  return cart;
}

export function clearCart(): void {
  if (isBrowser()) {
    window.localStorage.removeItem(CART_STORAGE_KEY);
  }
  window.dispatchEvent(new Event("aekiori-cart-updated"));
}

export function addCartItem(input: AddCartItemInput): CartSnapshot {
  const current = getCart();
  const nextItem: CartItem = {
    ...input,
    cartItemId: createCartItemId(input)
  };

  if (!current || current.storeId !== input.storeId) {
    return saveCart({
      storeId: input.storeId,
      storeName: input.storeName,
      items: [nextItem],
      updatedAt: Date.now()
    });
  }

  const existingIndex = current.items.findIndex((item) => item.cartItemId === nextItem.cartItemId);
  const items = [...current.items];
  if (existingIndex >= 0) {
    const existing = items[existingIndex];
    items[existingIndex] = {
      ...existing,
      quantity: Math.min(20, existing.quantity + input.quantity)
    };
  } else {
    items.push(nextItem);
  }

  return saveCart({
    ...current,
    storeName: input.storeName,
    items,
    updatedAt: Date.now()
  });
}

export function updateCartItemQuantity(cartItemId: string, quantity: number): CartSnapshot | null {
  const current = getCart();
  if (!current) return null;

  const nextQuantity = Math.max(0, Math.min(20, quantity));
  const items = nextQuantity === 0
    ? current.items.filter((item) => item.cartItemId !== cartItemId)
    : current.items.map((item) => (item.cartItemId === cartItemId ? { ...item, quantity: nextQuantity } : item));

  if (items.length === 0) {
    clearCart();
    return null;
  }

  return saveCart({ ...current, items, updatedAt: Date.now() });
}

export function getCartTotals(cart: CartSnapshot | null): { itemCount: number; totalAmount: number } {
  if (!cart) return { itemCount: 0, totalAmount: 0 };
  return cart.items.reduce(
    (acc, item) => ({
      itemCount: acc.itemCount + item.quantity,
      totalAmount: acc.totalAmount + item.unitPrice * item.quantity
    }),
    { itemCount: 0, totalAmount: 0 }
  );
}

function createCartItemId(input: AddCartItemInput): string {
  const optionKey = input.options
    .map((option) => `${option.groupName}:${option.optionName}:${option.extraPrice}`)
    .sort()
    .join("|");
  return `${input.storeId}:${input.menuId}:${input.unitPrice}:${optionKey}`;
}