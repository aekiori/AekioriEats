"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { SessionPanel } from "@/components/SessionPanel";
import { decodeJwtPayload, getTokenBundle } from "@/lib/auth-storage";
import { apiRequest, HttpError } from "@/lib/http";
import { OrderDetailResponse } from "@/lib/types";

const PORTONE_STORE_ID = process.env.NEXT_PUBLIC_PORTONE_STORE_ID ?? "store-f68c0d7a-c89c-4e1b-b3fd-7655a1d8899e";
const PORTONE_CHANNEL_KEY = process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY ?? "channel-key-b8338238-4b20-4ff2-ae68-6c6e3ed4c857";
const PORTONE_SDK_URL = "https://cdn.portone.io/v2/browser-sdk.js";

type PortOnePaymentResponse = {
  transactionType?: "PAYMENT";
  txId?: string;
  paymentId?: string;
  paymentToken?: string;
  code?: string;
  message?: string;
  pgCode?: string;
  pgMessage?: string;
};

type PortOneSdk = {
  requestPayment: (request: Record<string, unknown>) => Promise<PortOnePaymentResponse | undefined>;
};

declare global {
  interface Window {
    PortOne?: PortOneSdk;
  }
}

export default function CheckoutPage() {
  const [orderId, setOrderId] = useState("");
  const [amount, setAmount] = useState("");
  const [orderName, setOrderName] = useState("AekioriEats order");
  const [paymentId, setPaymentId] = useState(generatePaymentId());
  const [customerName, setCustomerName] = useState("Aekiori Tester");
  const [customerPhone, setCustomerPhone] = useState("01012345678");
  const [customerEmail, setCustomerEmail] = useState("tester@aekiori.local");

  const [sdkReady, setSdkReady] = useState(false);
  const [loading, setLoading] = useState(false);
  const [polling, setPolling] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loadedOrder, setLoadedOrder] = useState<OrderDetailResponse | null>(null);
  const [paymentResponse, setPaymentResponse] = useState<PortOnePaymentResponse | null>(null);
  const [confirmResponse, setConfirmResponse] = useState<unknown>(null);

  const canPay = useMemo(() => {
    return sdkReady && Number(orderId) > 0 && Number(amount) > 0 && paymentId.trim().length > 0;
  }, [amount, orderId, paymentId, sdkReady]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const queryOrderId = params.get("orderId");
    const queryAmount = params.get("amount");
    const queryPaymentId = params.get("paymentId");

    if (queryOrderId) {
      setOrderId(queryOrderId);
    }
    if (queryAmount) {
      setAmount(queryAmount);
    }
    if (queryPaymentId) {
      setPaymentId(queryPaymentId);
    }
  }, []);

  useEffect(() => {
    if (window.PortOne?.requestPayment) {
      setSdkReady(true);
      return;
    }

    const existingScript = document.querySelector<HTMLScriptElement>(`script[src="${PORTONE_SDK_URL}"]`);
    if (existingScript) {
      existingScript.addEventListener("load", () => setSdkReady(Boolean(window.PortOne?.requestPayment)));
      existingScript.addEventListener("error", () => setError("PortOne SDK load failed."));
      return;
    }

    const script = document.createElement("script");
    script.src = PORTONE_SDK_URL;
    script.async = true;
    script.onload = () => setSdkReady(Boolean(window.PortOne?.requestPayment));
    script.onerror = () => setError("PortOne SDK load failed.");
    document.body.appendChild(script);
  }, []);

  async function handleLoadOrder() {
    setLoading(true);
    setError("");
    setSuccess("");
    setLoadedOrder(null);

    try {
      const token = resolveAccessToken();
      const targetOrderId = parsePositiveInt(orderId, "orderId");
      const response = await apiRequest<OrderDetailResponse>(`/api/v1/orders/${targetOrderId}`, {
        method: "GET",
        token
      });

      setLoadedOrder(response);
      setAmount(String(response.finalAmount));
      setOrderName(buildOrderName(response));
      setPaymentId(generatePaymentId(response.orderId));
      setSuccess(`주문 조회 완료. status=${response.status}, amount=${response.finalAmount}`);
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function handlePollUntilPayWaiting() {
    setPolling(true);
    setError("");
    setSuccess("");

    try {
      const token = resolveAccessToken();
      const targetOrderId = parsePositiveInt(orderId, "orderId");
      const response = await pollOrderUntilPayWaiting(targetOrderId, token);

      setLoadedOrder(response);
      setAmount(String(response.finalAmount));
      setOrderName(buildOrderName(response));
      setSuccess(`PAY_WAITING 감지. 이제 결제창을 띄워도 됨. orderId=${response.orderId}`);
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setPolling(false);
    }
  }

  async function handleRequestPayment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setSuccess("");
    setPaymentResponse(null);
    setConfirmResponse(null);

    try {
      if (!window.PortOne?.requestPayment) {
        throw new Error("PortOne SDK is not ready yet.");
      }

      const targetOrderId = parsePositiveInt(orderId, "orderId");
      const totalAmount = parsePositiveInt(amount, "amount");
      const targetPaymentId = paymentId.trim();

      const response = await window.PortOne.requestPayment({
        storeId: PORTONE_STORE_ID,
        channelKey: PORTONE_CHANNEL_KEY,
        paymentId: targetPaymentId,
        orderName: orderName.trim() || `AekioriEats order ${targetOrderId}`,
        totalAmount,
        currency: "KRW",
        payMethod: "EASY_PAY",
        easyPay: {
          easyPayProvider: "KAKAOPAY"
        },
        customer: {
          customerId: resolveCustomerId(),
          fullName: customerName.trim(),
          phoneNumber: customerPhone.trim(),
          email: customerEmail.trim()
        },
        customData: {
          orderId: targetOrderId
        },
        redirectUrl: `${window.location.origin}/payment/checkout?orderId=${targetOrderId}&amount=${totalAmount}&paymentId=${encodeURIComponent(targetPaymentId)}`
      });

      setPaymentResponse(response ?? null);

      if (response?.code) {
        throw new Error(`${response.code}: ${response.message ?? "Payment failed."}`);
      }

      if (!response?.paymentId) {
        throw new Error("PortOne response does not include paymentId.");
      }

      setPaymentId(response.paymentId);
      setSuccess(`포트원 결제창 완료. paymentId=${response.paymentId}`);
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function handleConfirmPayment() {
    setLoading(true);
    setError("");
    setSuccess("");
    setConfirmResponse(null);

    try {
      const token = resolveAccessToken();
      const body = {
        orderId: parsePositiveInt(orderId, "orderId"),
        paymentId: paymentId.trim(),
        amount: parsePositiveInt(amount, "amount")
      };

      if (!body.paymentId) {
        throw new Error("paymentId is required.");
      }

      const response = await apiRequest<unknown>("/api/v1/payments/confirm", {
        method: "POST",
        token,
        body
      });

      setConfirmResponse(response);
      setSuccess("결제 confirm 요청 완료. 이제 order-service가 payment.succeeded 이벤트를 소비하면 PAID로 넘어가면 됨.");
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="stack">
      <section className="card stack">
        <div className="row" style={{ alignItems: "center", justifyContent: "space-between" }}>
          <div>
            <h1 style={{ margin: 0 }}>KakaoPay checkout</h1>
            <p className="muted" style={{ margin: "6px 0 0" }}>
              주문이 PAY_WAITING이 되면 포트원 카카오페이 결제창을 띄우고, 완료 후 Payment confirm API를 호출한다.
            </p>
          </div>
          <span className={sdkReady ? "success" : "muted"}>{sdkReady ? "PortOne SDK ready" : "Loading SDK..."}</span>
        </div>
      </section>

      <div className="grid-2">
        <section className="card stack">
          <h2 style={{ margin: 0 }}>1) 주문 정보</h2>
          <div className="grid-2">
            <div>
              <div className="label">orderId</div>
              <input className="input" value={orderId} onChange={(event) => setOrderId(event.target.value)} placeholder="1" />
            </div>
            <div>
              <div className="label">amount</div>
              <input className="input" value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="19900" />
            </div>
          </div>
          <div>
            <div className="label">orderName</div>
            <input className="input" value={orderName} onChange={(event) => setOrderName(event.target.value)} />
          </div>
          <div>
            <div className="label">paymentId</div>
            <div className="row">
              <input className="input" value={paymentId} onChange={(event) => setPaymentId(event.target.value)} />
              <button
                type="button"
                className="button secondary"
                style={{ width: "auto", paddingInline: 14 }}
                onClick={() => setPaymentId(generatePaymentId(Number(orderId) || undefined))}
              >
                Regenerate
              </button>
            </div>
          </div>
          <div className="row">
            <button className="button secondary" style={{ width: "auto", paddingInline: 18 }} onClick={handleLoadOrder} disabled={loading}>
              Load order
            </button>
            <button className="button secondary" style={{ width: "auto", paddingInline: 18 }} onClick={handlePollUntilPayWaiting} disabled={polling || loading}>
              {polling ? "Polling..." : "Poll PAY_WAITING"}
            </button>
          </div>
          {loadedOrder && <pre className="pre">{JSON.stringify(loadedOrder, null, 2)}</pre>}
        </section>

        <section className="card stack">
          <h2 style={{ margin: 0 }}>2) 구매자 정보</h2>
          <div>
            <div className="label">customerName</div>
            <input className="input" value={customerName} onChange={(event) => setCustomerName(event.target.value)} />
          </div>
          <div>
            <div className="label">phoneNumber</div>
            <input className="input" value={customerPhone} onChange={(event) => setCustomerPhone(event.target.value)} />
          </div>
          <div>
            <div className="label">email</div>
            <input className="input" value={customerEmail} onChange={(event) => setCustomerEmail(event.target.value)} />
          </div>
          <div className="pre">
            <div>storeId: {PORTONE_STORE_ID}</div>
            <div>channelKey: {PORTONE_CHANNEL_KEY}</div>
            <div>payMethod: EASY_PAY / KAKAOPAY</div>
          </div>
        </section>
      </div>

      <section className="card stack">
        <h2 style={{ margin: 0 }}>3) 결제 실행</h2>
        <form className="stack" onSubmit={handleRequestPayment}>
          <button className="button" type="submit" disabled={!canPay || loading}>
            {loading ? "Processing..." : "카카오페이 결제창 열기"}
          </button>
        </form>
        <button className="button secondary" onClick={handleConfirmPayment} disabled={loading}>
          POST /api/v1/payments/confirm
        </button>
        {paymentResponse && <pre className="pre">{JSON.stringify(paymentResponse, null, 2)}</pre>}
        {confirmResponse !== null && <pre className="pre">{JSON.stringify(confirmResponse, null, 2)}</pre>}
      </section>

      {(error || success) && (
        <section className="card stack">
          {success && (
            <p className="success" style={{ margin: 0 }}>
              {success}
            </p>
          )}
          {error && (
            <p className="error" style={{ margin: 0 }}>
              {error}
            </p>
          )}
        </section>
      )}

      <SessionPanel />
    </div>
  );
}

async function pollOrderUntilPayWaiting(orderId: number, token: string): Promise<OrderDetailResponse> {
  const maxAttempts = 30;
  const delayMs = 2000;

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    const response = await apiRequest<OrderDetailResponse>(`/api/v1/orders/${orderId}`, {
      method: "GET",
      token
    });

    if (response.status === "PAY_WAITING" || response.status === "PAYMENT_PENDING") {
      return response;
    }

    await sleep(delayMs);
  }

  throw new Error("Timed out waiting for PAY_WAITING.");
}

function buildOrderName(order: OrderDetailResponse): string {
  const firstItem = order.items[0];
  if (!firstItem) {
    return `AekioriEats order ${order.orderId}`;
  }

  const suffix = order.items.length > 1 ? ` plus ${order.items.length - 1}` : "";
  return `${firstItem.menuName}${suffix}`;
}

function resolveAccessToken(): string {
  const bundle = getTokenBundle();
  if (!bundle?.accessToken) {
    throw new Error("Login required.");
  }
  return bundle.accessToken;
}

function resolveCustomerId(): string {
  const bundle = getTokenBundle();
  const payload = bundle?.accessToken ? decodeJwtPayload(bundle.accessToken) : null;
  const userId = payload?.user_id ?? payload?.sub;
  return userId ? `user-${userId}` : "guest";
}

function parsePositiveInt(value: string, label: string): number {
  const num = Number(value);
  if (!Number.isInteger(num) || num <= 0) {
    throw new Error(`${label} must be a positive integer.`);
  }
  return num;
}

function generatePaymentId(orderId?: number): string {
  const suffix = `${Date.now()}${Math.random().toString(36).slice(2, 10)}`;

  return orderId ? `paymentorder${orderId}${suffix}` : `payment${suffix}`;
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms);
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
