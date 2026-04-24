"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";
import { SessionPanel } from "@/components/SessionPanel";
import { decodeJwtPayload, getTokenBundle } from "@/lib/auth-storage";
import { ensureDevSession } from "@/lib/dev-session";
import { apiRequest, HttpError } from "@/lib/http";
import {
  CreateOrderItemRequest,
  CreateOrderRequest,
  CreateOrderResponse,
  OrderDetailResponse,
  StoreSearchPageResponse
} from "@/lib/types";

type AuthContext = {
  accessToken: string;
  userId: number;
};

type DraftOrderItem = {
  menuId: string;
  menuName: string;
  unitPrice: string;
  quantity: string;
};

const EMPTY_ITEM: DraftOrderItem = {
  menuId: "",
  menuName: "",
  unitPrice: "",
  quantity: "1"
};

export default function CreateOrderPage() {
  const [storeId, setStoreId] = useState("");
  const [deliveryAddress, setDeliveryAddress] = useState("");
  const [usedPointAmount, setUsedPointAmount] = useState("0");
  const [idempotencyKey, setIdempotencyKey] = useState(generateIdempotencyKey);
  const [items, setItems] = useState<DraftOrderItem[]>([{ ...EMPTY_ITEM }]);
  const [orderIdInput, setOrderIdInput] = useState("");

  const [searchQuery, setSearchQuery] = useState("chicken");
  const [storeSearchResult, setStoreSearchResult] = useState<StoreSearchPageResponse | null>(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [createdOrder, setCreatedOrder] = useState<CreateOrderResponse | null>(null);
  const [loadedOrder, setLoadedOrder] = useState<OrderDetailResponse | null>(null);

  function updateItem(index: number, next: Partial<DraftOrderItem>) {
    setItems((prev) => prev.map((item, idx) => (idx === index ? { ...item, ...next } : item)));
  }

  function addItem() {
    setItems((prev) => [...prev, { ...EMPTY_ITEM }]);
  }

  function removeItem(index: number) {
    setItems((prev) => (prev.length <= 1 ? prev : prev.filter((_, idx) => idx !== index)));
  }

  async function handleCreateOrder(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setSuccess("");
    setCreatedOrder(null);
    setLoadedOrder(null);

    try {
      const auth = await resolveAuthContext();
      const request: CreateOrderRequest = {
        userId: auth.userId,
        storeId: parsePositiveInt(storeId, "Store ID"),
        deliveryAddress: deliveryAddress.trim(),
        usedPointAmount: parseNonNegativeInt(usedPointAmount, "Used point amount"),
        items: items.map((item, index) => toOrderItemRequest(item, index))
      };

      const response = await apiRequest<CreateOrderResponse>("/api/v1/orders", {
        method: "POST",
        token: auth.accessToken,
        headers: {
          "X-Idempotency-Key": idempotencyKey
        },
        body: request
      });

      setCreatedOrder(response);
      setOrderIdInput(String(response.orderId));
      setSuccess(`Order created. orderId=${response.orderId}`);
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function handleLoadOrder() {
    setLoading(true);
    setError("");
    setSuccess("");
    setLoadedOrder(null);

    try {
      const auth = await resolveAuthContext();
      const targetOrderId = parsePositiveInt(orderIdInput, "Order ID");
      const response = await apiRequest<OrderDetailResponse>(`/api/v1/orders/${targetOrderId}`, {
        method: "GET",
        token: auth.accessToken
      });
      setLoadedOrder(response);
      setSuccess(`Order loaded. orderId=${targetOrderId}`);
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function handleSearchStore() {
    setLoading(true);
    setError("");
    setSuccess("");
    setStoreSearchResult(null);

    try {
      const auth = await resolveAuthContext();
      const query = searchQuery.trim();
      if (!query) {
        throw new Error("Search query is required.");
      }
      const response = await apiRequest<StoreSearchPageResponse>(
        `/api/v1/stores/search?q=${encodeURIComponent(query)}&page=0&size=20`,
        {
          method: "GET",
          token: auth.accessToken
        }
      );

      setStoreSearchResult(response);
      setSuccess(`Store search complete. ${response.totalElements} hit(s).`);
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="stack">
      <section className="card stack">
        <h1 style={{ margin: 0 }}>Create order</h1>
        <p className="muted" style={{ margin: 0 }}>
          Login first. userId is extracted from JWT payload and sent automatically.
        </p>
      </section>

      <div className="grid-2">
        <section className="card stack">
          <h2 style={{ margin: 0 }}>1) Create order</h2>
          <form className="stack" onSubmit={handleCreateOrder}>
            <div className="grid-2">
              <div>
                <div className="label">Store ID</div>
                <input
                  className="input"
                  value={storeId}
                  onChange={(event) => setStoreId(event.target.value)}
                  placeholder="storeId"
                  required
                />
              </div>
              <div>
                <div className="label">Used point amount</div>
                <input
                  className="input"
                  type="number"
                  min={0}
                  value={usedPointAmount}
                  onChange={(event) => setUsedPointAmount(event.target.value)}
                />
              </div>
            </div>
            <div>
              <div className="label">Delivery address</div>
              <input
                className="input"
                value={deliveryAddress}
                onChange={(event) => setDeliveryAddress(event.target.value)}
                placeholder="Sanbon-ro 10, Gunpo-si"
                required
              />
            </div>
            <div>
              <div className="label">X-Idempotency-Key (UUID v4)</div>
              <div className="row">
                <input
                  className="input"
                  value={idempotencyKey}
                  onChange={(event) => setIdempotencyKey(event.target.value)}
                  required
                />
                <button
                  type="button"
                  className="button secondary"
                  style={{ width: "auto", paddingInline: 14 }}
                  onClick={() => setIdempotencyKey(generateIdempotencyKey())}
                >
                  Regenerate
                </button>
              </div>
            </div>

            <div className="stack">
              <div className="label">Items</div>
              {items.map((item, index) => (
                <div className="card stack" key={index} style={{ padding: 12 }}>
                  <div className="row">
                    <b>Item #{index + 1}</b>
                    <button
                      type="button"
                      className="button danger"
                      style={{ width: "auto", paddingInline: 10 }}
                      onClick={() => removeItem(index)}
                      disabled={items.length <= 1}
                    >
                      Remove
                    </button>
                  </div>
                  <div className="grid-2">
                    <div>
                      <div className="label">menuId</div>
                      <input
                        className="input"
                        value={item.menuId}
                        onChange={(event) => updateItem(index, { menuId: event.target.value })}
                        required
                      />
                    </div>
                    <div>
                      <div className="label">menuName</div>
                      <input
                        className="input"
                        value={item.menuName}
                        onChange={(event) => updateItem(index, { menuName: event.target.value })}
                        required
                      />
                    </div>
                  </div>
                  <div className="grid-2">
                    <div>
                      <div className="label">unitPrice</div>
                      <input
                        className="input"
                        type="number"
                        min={1}
                        value={item.unitPrice}
                        onChange={(event) => updateItem(index, { unitPrice: event.target.value })}
                        required
                      />
                    </div>
                    <div>
                      <div className="label">quantity</div>
                      <input
                        className="input"
                        type="number"
                        min={1}
                        value={item.quantity}
                        onChange={(event) => updateItem(index, { quantity: event.target.value })}
                        required
                      />
                    </div>
                  </div>
                </div>
              ))}
              <button type="button" className="button secondary" onClick={addItem}>
                Add item
              </button>
            </div>

            <button className="button" type="submit" disabled={loading}>
              Create order
            </button>
          </form>
          {createdOrder && (
            <div className="stack">
              <Link
                className="button secondary"
                href={`/payment/checkout?orderId=${createdOrder.orderId}&amount=${createdOrder.finalAmount}`}
              >
                Go to payment checkout
              </Link>
              <pre className="pre">{JSON.stringify(createdOrder, null, 2)}</pre>
            </div>
          )}
        </section>

        <section className="card stack">
          <h2 style={{ margin: 0 }}>2) Load order detail</h2>
          <div>
            <div className="label">Order ID</div>
            <input
              className="input"
              value={orderIdInput}
              onChange={(event) => setOrderIdInput(event.target.value)}
              placeholder="orderId"
            />
          </div>
          <button className="button secondary" onClick={handleLoadOrder} disabled={loading}>
            Load order
          </button>
          {loadedOrder && <pre className="pre">{JSON.stringify(loadedOrder, null, 2)}</pre>}
        </section>
      </div>

      <section className="card stack">
        <h2 style={{ margin: 0 }}>3) Search stores (for storeId)</h2>
        <div className="row">
          <input
            className="input"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder="chicken"
          />
          <button className="button secondary" style={{ width: "auto", paddingInline: 18 }} onClick={handleSearchStore} disabled={loading}>
            Search
          </button>
        </div>
        {storeSearchResult && <pre className="pre">{JSON.stringify(storeSearchResult, null, 2)}</pre>}
      </section>

      {(error || success) && (
        <section className="card stack">
          {success && <p className="success" style={{ margin: 0 }}>{success}</p>}
          {error && <p className="error" style={{ margin: 0 }}>{error}</p>}
        </section>
      )}

      <SessionPanel />
    </div>
  );
}

async function resolveAuthContext(): Promise<AuthContext> {
  await ensureDevSession();
  const bundle = getTokenBundle();
  if (!bundle?.accessToken) {
    throw new Error("Login required.");
  }

  const payload = decodeJwtPayload(bundle.accessToken);
  const parsedUserId = payload?.user_id ?? (payload?.sub ? Number(payload.sub) : NaN);
  if (!Number.isFinite(parsedUserId) || Number(parsedUserId) <= 0) {
    throw new Error("Invalid access token payload. user_id is missing.");
  }

  return {
    accessToken: bundle.accessToken,
    userId: Number(parsedUserId)
  };
}

function toOrderItemRequest(item: DraftOrderItem, index: number): CreateOrderItemRequest {
  return {
    menuId: parsePositiveInt(item.menuId, `items[${index}].menuId`),
    menuName: item.menuName.trim(),
    unitPrice: parsePositiveInt(item.unitPrice, `items[${index}].unitPrice`),
    quantity: parsePositiveInt(item.quantity, `items[${index}].quantity`)
  };
}

function parsePositiveInt(value: string, label: string): number {
  const num = Number(value);
  if (!Number.isInteger(num) || num <= 0) {
    throw new Error(`${label} must be a positive integer.`);
  }
  return num;
}

function parseNonNegativeInt(value: string, label: string): number {
  const num = Number(value);
  if (!Number.isInteger(num) || num < 0) {
    throw new Error(`${label} must be a non-negative integer.`);
  }
  return num;
}

function generateIdempotencyKey(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  const pattern = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx";
  return pattern.replace(/[xy]/g, (char) => {
    const rand = Math.floor(Math.random() * 16);
    const value = char === "x" ? rand : (rand & 0x3) | 0x8;
    return value.toString(16);
  });
}

function toErrorMessage(caught: unknown): string {
  if (caught instanceof HttpError) {
    return `${caught.code ?? "HTTP_ERROR"}: ${caught.message}`;
  }
  if (caught instanceof Error) {
    return caught.message;
  }
  return "Request failed.";
}
