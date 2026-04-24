"use client";

import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import { SessionPanel } from "@/components/SessionPanel";
import { decodeJwtPayload, getTokenBundle } from "@/lib/auth-storage";
import { ensureDevSession } from "@/lib/dev-session";
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

type CheckoutStage = "preparing" | "ready" | "paying" | "confirming" | "waiting_owner" | "accepted" | "rejected" | "failed";

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
  const [orderReady, setOrderReady] = useState(false);
  const [stage, setStage] = useState<CheckoutStage>("preparing");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loadedOrder, setLoadedOrder] = useState<OrderDetailResponse | null>(null);
  const [paymentResponse, setPaymentResponse] = useState<PortOnePaymentResponse | null>(null);
  const [confirmResponse, setConfirmResponse] = useState<unknown>(null);

  const autoConfirmKeyRef = useRef("");

  const canPay = useMemo(() => {
    return orderReady && sdkReady && Number(orderId) > 0 && Number(amount) > 0 && paymentId.trim().length > 0;
  }, [amount, orderId, orderReady, paymentId, sdkReady]);

  useEffect(() => {
    void (async () => {
      try {
        await ensureDevSession();
      } catch (caught) {
        setError(toErrorMessage(caught));
        return;
      }

      const params = new URLSearchParams(window.location.search);
      const queryOrderId = params.get("orderId");
      const queryAmount = params.get("amount");
      const queryPaymentId = params.get("paymentId");

      if (queryOrderId) {
        setOrderId(queryOrderId);
        void preparePaymentFromOrder(queryOrderId);
      }
      if (queryAmount) {
        setAmount(queryAmount);
      }
      if (queryPaymentId && isUuidV4(queryPaymentId)) {
        setPaymentId(queryPaymentId);
      } else if (queryPaymentId) {
        setPaymentId(generatePaymentId());
      }
    })();
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

  useEffect(() => {
    void (async () => {
      try {
        await ensureDevSession();
      } catch (caught) {
        setError(toErrorMessage(caught));
        return;
      }

      const params = new URLSearchParams(window.location.search);
      const queryOrderId = params.get("orderId");
      const queryAmount = params.get("amount");
      const queryPaymentId = params.get("paymentId");

      if (!queryOrderId || !queryAmount || !queryPaymentId || !isUuidV4(queryPaymentId)) {
        return;
      }

      const autoConfirmKey = `${queryOrderId}:${queryPaymentId}`;
      if (autoConfirmKeyRef.current === autoConfirmKey) {
        return;
      }
      autoConfirmKeyRef.current = autoConfirmKey;

      void autoConfirmAndWatch(queryOrderId, queryPaymentId, queryAmount);
    })();
  }, []);

  async function handleLoadOrder() {
    setLoading(true);
    setError("");
    setSuccess("");
    setOrderReady(false);
    setLoadedOrder(null);

    try {
      const token = resolveAccessToken();
      const targetOrderId = parsePositiveInt(orderId, "orderId");
      const response = await fetchOrder(targetOrderId, token);

      hydrateOrder(response, true);
      setSuccess(`주문 조회 완료. status=${response.status}, amount=${response.finalAmount}`);
    } catch (caught) {
      setStage("failed");
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function handlePollUntilPayWaiting() {
    setPolling(true);
    setError("");
    setSuccess("");
    setOrderReady(false);
    setStage("preparing");

    try {
      const token = resolveAccessToken();
      const targetOrderId = parsePositiveInt(orderId, "orderId");
      const response = await pollOrderUntilPayWaiting(targetOrderId, token);

      hydrateOrder(response, false);
      setOrderReady(true);
      setStage("ready");
      setSuccess(`결제 가능 상태 확인 완료. orderId=${response.orderId}`);
    } catch (caught) {
      setStage("failed");
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
    setStage("paying");

    try {
      if (!window.PortOne?.requestPayment) {
        throw new Error("PortOne SDK is not ready yet.");
      }

      const targetOrderId = parsePositiveInt(orderId, "orderId");
      const totalAmount = parsePositiveInt(amount, "amount");
      const targetPaymentId = paymentId.trim();

      if (!orderReady) {
        throw new Error("주문이 아직 결제 대기 상태가 아닙니다. PAY_WAITING 확인 후 다시 결제해주세요.");
      }

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

      const confirmedPaymentId = response?.paymentId ?? targetPaymentId;
      if (!confirmedPaymentId) {
        throw new Error("PortOne response does not include paymentId.");
      }

      setPaymentId(confirmedPaymentId);
      await confirmAndWatchOwnerDecision(targetOrderId, confirmedPaymentId, totalAmount);
    } catch (caught) {
      setStage("failed");
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
      await confirmAndWatchOwnerDecision(
        parsePositiveInt(orderId, "orderId"),
        paymentId.trim(),
        parsePositiveInt(amount, "amount")
      );
    } catch (caught) {
      setStage("failed");
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function preparePaymentFromOrder(targetOrderIdValue: string) {
    setPolling(true);
    setError("");
    setSuccess("주문 검증 결과를 기다리는 중입니다. 결제 가능한 상태가 되면 버튼이 열립니다.");
    setOrderReady(false);
    setStage("preparing");

    try {
      const token = resolveAccessToken();
      const targetOrderId = parsePositiveInt(targetOrderIdValue, "orderId");
      const response = await pollOrderUntilPayWaiting(targetOrderId, token);

      hydrateOrder(response, false);
      setOrderReady(true);
      setStage("ready");
      setSuccess(`결제 가능 상태 확인 완료. status=${response.status}, orderId=${response.orderId}`);
    } catch (caught) {
      setStage("failed");
      setError(toErrorMessage(caught));
    } finally {
      setPolling(false);
    }
  }

  async function autoConfirmAndWatch(queryOrderId: string, queryPaymentId: string, queryAmount: string) {
    setLoading(true);
    setError("");
    setSuccess("결제가 완료되어 자동 confirm 중입니다.");
    setPaymentId(queryPaymentId);
    setOrderId(queryOrderId);
    setAmount(queryAmount);

    try {
      await confirmAndWatchOwnerDecision(
        parsePositiveInt(queryOrderId, "orderId"),
        queryPaymentId,
        parsePositiveInt(queryAmount, "amount")
      );
    } catch (caught) {
      setStage("failed");
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function confirmAndWatchOwnerDecision(targetOrderId: number, targetPaymentId: string, totalAmount: number) {
    if (!targetPaymentId) {
      throw new Error("paymentId is required.");
    }

    const token = resolveAccessToken();
    setStage("confirming");
    setSuccess("결제 confirm 요청 중입니다.");

    const response = await confirmPaymentWithRetry(token, {
      orderId: targetOrderId,
      paymentId: targetPaymentId,
      amount: totalAmount
    });

    setConfirmResponse(response);
    setStage("waiting_owner");
    setSuccess("결제 confirm 완료. 사장님이 주문을 확인하고 있습니다...");

    const finalOrder = await pollOrderUntilOwnerDecision(targetOrderId, token, (order) => {
      hydrateOrder(order, false);
      if (order.status === "PAID") {
        setStage("waiting_owner");
        setSuccess("사장님이 고민중입니다...");
      }
    });

    hydrateOrder(finalOrder, false);
    if (finalOrder.status === "ACCEPTED") {
      setStage("accepted");
      setSuccess("사장님이 주문을 수락했습니다. 곧 조리가 시작됩니다.");
      return;
    }

    if (finalOrder.status === "REFUND_PENDING") {
      setStage("rejected");
      setSuccess("사장님이 주문을 거절했습니다. 환불 대기 상태로 전환되었습니다.");
      return;
    }

    if (finalOrder.status === "FAILED" || finalOrder.status === "CANCELLED") {
      setStage("failed");
      setError(`주문이 ${finalOrder.status} 상태가 되었습니다.`);
    }
  }

  function hydrateOrder(order: OrderDetailResponse, regeneratePaymentId: boolean) {
    setLoadedOrder(order);
    setAmount(String(order.finalAmount));
    setOrderName(buildOrderName(order));
    setOrderReady(isPaymentReadyStatus(order.status));
    if (regeneratePaymentId) {
      setPaymentId(generatePaymentId(order.orderId));
    }
  }

  const statusCard = buildStatusCard(stage, loadedOrder?.status);

  return (
    <div className="mx-auto flex min-h-screen max-w-[720px] flex-col bg-[#f7f8fb] text-[#20242c]">
      <section className="relative overflow-hidden bg-[#111827] px-6 pb-8 pt-10 text-white">
        <div className="absolute -right-14 -top-20 h-44 w-44 rounded-full bg-[#50b5ff]/30 blur-2xl" />
        <div className="absolute -bottom-24 left-8 h-44 w-44 rounded-full bg-[#ffe761]/30 blur-2xl" />
        <div className="relative z-10">
          <p className="text-sm font-bold text-[#8fd3ff]">AekioriEats Pay</p>
          <h1 className="mt-2 text-[30px] font-black tracking-[-0.04em]">카카오페이 결제</h1>
          <p className="mt-3 text-sm leading-6 text-white/70">
            결제 완료 후 confirm은 자동으로 호출됩니다. 이후 주문 상태를 폴링해서 사장님 수락/거절 결과를 보여줍니다.
          </p>
        </div>
      </section>

      <main className="-mt-4 flex flex-1 flex-col gap-4 px-4 pb-24">
        <section className="rounded-[28px] border border-white bg-white p-5 shadow-[0_18px_50px_rgba(20,29,45,0.10)]">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.2em] text-[#93a0b4]">현재 상태</p>
              <h2 className="mt-2 text-2xl font-black tracking-[-0.04em]">{statusCard.title}</h2>
            </div>
            <div className={`rounded-2xl px-4 py-2 text-sm font-black ${statusCard.badgeClass}`}>{statusCard.badge}</div>
          </div>
          <p className="mt-3 text-[15px] leading-6 text-[#687386]">{statusCard.description}</p>
          {loadedOrder && (
            <div className="mt-4 grid grid-cols-3 gap-2 text-center">
              <div className="rounded-2xl bg-[#f3f6fa] p-3">
                <p className="text-[11px] font-bold text-[#93a0b4]">orderId</p>
                <p className="mt-1 text-lg font-black">{loadedOrder.orderId}</p>
              </div>
              <div className="rounded-2xl bg-[#f3f6fa] p-3">
                <p className="text-[11px] font-bold text-[#93a0b4]">status</p>
                <p className="mt-1 text-sm font-black">{loadedOrder.status}</p>
              </div>
              <div className="rounded-2xl bg-[#f3f6fa] p-3">
                <p className="text-[11px] font-bold text-[#93a0b4]">amount</p>
                <p className="mt-1 text-sm font-black">{formatWon(loadedOrder.finalAmount)}</p>
              </div>
            </div>
          )}
        </section>

        <section className="rounded-[28px] bg-white p-5 shadow-[0_12px_35px_rgba(20,29,45,0.08)]">
          <h2 className="text-lg font-black">주문 정보</h2>
          <div className="mt-4 grid grid-cols-2 gap-3">
            <LabeledInput label="orderId" value={orderId} onChange={setOrderId} placeholder="1" />
            <LabeledInput label="amount" value={amount} onChange={setAmount} placeholder="19900" />
          </div>
          <div className="mt-3">
            <LabeledInput label="orderName" value={orderName} onChange={setOrderName} />
          </div>
          <div className="mt-3">
            <div className="mb-1 text-xs font-black text-[#7b8492]">paymentId</div>
            <div className="flex gap-2">
              <input className="min-w-0 flex-1 rounded-2xl border border-[#e1e7ef] px-4 py-3 text-sm outline-none focus:border-[#50b5ff]" value={paymentId} onChange={(event) => setPaymentId(event.target.value)} />
              <button
                type="button"
                className="rounded-2xl border border-[#d8e1eb] px-4 text-sm font-black text-[#2f89c9]"
                onClick={() => setPaymentId(generatePaymentId(Number(orderId) || undefined))}
              >
                재생성
              </button>
            </div>
          </div>
          <div className="mt-4 flex gap-2">
            <button className="flex-1 rounded-2xl bg-[#edf4ff] px-4 py-3 text-sm font-black text-[#267cc2]" onClick={handleLoadOrder} disabled={loading}>
              주문 조회
            </button>
            <button className="flex-1 rounded-2xl bg-[#edf4ff] px-4 py-3 text-sm font-black text-[#267cc2]" onClick={handlePollUntilPayWaiting} disabled={polling || loading}>
              {polling ? "대기 중..." : "결제대기 확인"}
            </button>
          </div>
        </section>

        <section className="rounded-[28px] bg-white p-5 shadow-[0_12px_35px_rgba(20,29,45,0.08)]">
          <h2 className="text-lg font-black">구매자 정보</h2>
          <div className="mt-4 flex flex-col gap-3">
            <LabeledInput label="이름" value={customerName} onChange={setCustomerName} />
            <LabeledInput label="전화번호" value={customerPhone} onChange={setCustomerPhone} />
            <LabeledInput label="이메일" value={customerEmail} onChange={setCustomerEmail} />
          </div>
        </section>

        <section className="rounded-[28px] bg-white p-5 shadow-[0_12px_35px_rgba(20,29,45,0.08)]">
          <form onSubmit={handleRequestPayment}>
            <button
              className="w-full rounded-[22px] bg-[#50b5ff] px-5 py-4 text-[17px] font-black text-white shadow-[0_12px_25px_rgba(80,181,255,0.35)] disabled:bg-[#bfccda] disabled:shadow-none"
              type="submit"
              disabled={!canPay || loading}
            >
              {loading ? "처리 중..." : polling ? "주문 검증 대기 중..." : orderReady ? `${formatWon(Number(amount) || 0)} 카카오페이 결제하기` : "결제 대기 상태 확인 중"}
            </button>
          </form>
          <button className="mt-3 w-full rounded-[22px] border border-[#d8e1eb] px-5 py-4 text-[15px] font-black text-[#536174]" onClick={handleConfirmPayment} disabled={loading}>
            수동 confirm 재시도
          </button>
        </section>

        {(error || success) && (
          <section className="rounded-[24px] bg-white p-5 shadow-[0_12px_35px_rgba(20,29,45,0.08)]">
            {success && <p className="text-sm font-bold leading-6 text-[#198754]">{success}</p>}
            {error && <p className="mt-2 text-sm font-bold leading-6 text-[#d64545]">{error}</p>}
          </section>
        )}

        {(paymentResponse !== null || confirmResponse !== null) && (
          <details className="rounded-[24px] bg-[#111827] p-5 text-white">
            <summary className="cursor-pointer text-sm font-black">디버그 응답 보기</summary>
            {paymentResponse && <pre className="mt-4 overflow-auto text-xs text-white/80">{JSON.stringify(paymentResponse, null, 2)}</pre>}
            {confirmResponse !== null && <pre className="mt-4 overflow-auto text-xs text-white/80">{JSON.stringify(confirmResponse, null, 2)}</pre>}
          </details>
        )}

        <SessionPanel />
      </main>
    </div>
  );
}

function LabeledInput({ label, value, onChange, placeholder }: { label: string; value: string; onChange: (value: string) => void; placeholder?: string }) {
  return (
    <label className="block">
      <span className="mb-1 block text-xs font-black text-[#7b8492]">{label}</span>
      <input
        className="w-full rounded-2xl border border-[#e1e7ef] px-4 py-3 text-sm outline-none focus:border-[#50b5ff]"
        value={value}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  );
}

async function fetchOrder(orderId: number, token: string): Promise<OrderDetailResponse> {
  return apiRequest<OrderDetailResponse>(`/api/v1/orders/${orderId}`, {
    method: "GET",
    token
  });
}

async function pollOrderUntilPayWaiting(orderId: number, token: string): Promise<OrderDetailResponse> {
  const maxAttempts = 30;
  const delayMs = 2000;

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    const response = await fetchOrder(orderId, token);

    if (isPaymentReadyStatus(response.status)) {
      return response;
    }

    await sleep(delayMs);
  }

  throw new Error("Timed out waiting for payment-ready status.");
}

async function pollOrderUntilOwnerDecision(
  orderId: number,
  token: string,
  onOrder: (order: OrderDetailResponse) => void
): Promise<OrderDetailResponse> {
  const maxAttempts = 120;
  const delayMs = 2000;
  let lastOrder: OrderDetailResponse | null = null;

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    const response = await fetchOrder(orderId, token);
    lastOrder = response;
    onOrder(response);

    if (["ACCEPTED", "REFUND_PENDING", "FAILED", "CANCELLED"].includes(response.status)) {
      return response;
    }

    await sleep(delayMs);
  }

  if (lastOrder) {
    return lastOrder;
  }

  throw new Error("Timed out waiting for store owner decision.");
}

async function confirmPaymentWithRetry(
  token: string,
  body: { orderId: number; paymentId: string; amount: number }
): Promise<unknown> {
  const maxAttempts = 6;

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      return await apiRequest<unknown>("/api/v1/payments/confirm", {
        method: "POST",
        token,
        body
      });
    } catch (caught) {
      const shouldRetry = caught instanceof HttpError && caught.status === 404 && attempt < maxAttempts;
      if (!shouldRetry) {
        throw caught;
      }
      await sleep(1500);
    }
  }

  throw new Error("Payment request is not ready yet.");
}

function isPaymentReadyStatus(status: string): boolean {
  return status === "PAY_WAITING" || status === "PAYMENT_PENDING";
}

function buildOrderName(order: OrderDetailResponse): string {
  const firstItem = order.items[0];
  if (!firstItem) {
    return `AekioriEats order ${order.orderId}`;
  }

  const suffix = order.items.length > 1 ? ` 외 ${order.items.length - 1}개` : "";
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

function generatePaymentId(_orderId?: number): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }

  return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, (char) => {
    const value = Number(char);
    return (value ^ (Math.random() * 16) >> (value / 4)).toString(16);
  });
}

function isUuidV4(value: string): boolean {
  return /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$/.test(value);
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

function formatWon(value: number): string {
  return `${value.toLocaleString("ko-KR")}원`;
}

function buildStatusCard(stage: CheckoutStage, orderStatus?: string) {
  if (stage === "accepted" || orderStatus === "ACCEPTED") {
    return {
      title: "주문 수락 완료",
      badge: "ACCEPTED",
      badgeClass: "bg-[#e8f8ef] text-[#198754]",
      description: "사장님이 주문을 수락했습니다. 이제 조리 단계로 넘어갈 수 있습니다."
    };
  }

  if (stage === "rejected" || orderStatus === "REFUND_PENDING") {
    return {
      title: "주문 거절",
      badge: "REFUND",
      badgeClass: "bg-[#fff1f1] text-[#d64545]",
      description: "사장님이 주문을 거절했습니다. 결제 환불 대기 상태로 전환되었습니다."
    };
  }

  if (stage === "waiting_owner" || orderStatus === "PAID") {
    return {
      title: "사장님이 고민중입니다...",
      badge: "PAID",
      badgeClass: "bg-[#fff8d8] text-[#9a7500]",
      description: "결제는 완료됐고, 가게의 최종 수락/거절 응답을 기다리는 중입니다. 별도 API로 수락/거절하면 이 화면에 자동 반영됩니다."
    };
  }

  if (stage === "confirming") {
    return {
      title: "결제 confirm 중",
      badge: "CONFIRM",
      badgeClass: "bg-[#edf4ff] text-[#267cc2]",
      description: "포트원 결제 완료 후 payment-service에 confirm 요청을 보내고 있습니다."
    };
  }

  if (stage === "paying") {
    return {
      title: "카카오페이 결제 중",
      badge: "PAYING",
      badgeClass: "bg-[#fff8d8] text-[#9a7500]",
      description: "카카오페이 결제창에서 테스트 결제를 완료해주세요. 완료되면 confirm은 자동으로 진행됩니다."
    };
  }

  if (stage === "failed" || orderStatus === "FAILED" || orderStatus === "CANCELLED") {
    return {
      title: "처리 확인 필요",
      badge: orderStatus ?? "ERROR",
      badgeClass: "bg-[#fff1f1] text-[#d64545]",
      description: "결제 또는 주문 흐름에서 확인이 필요한 상태입니다. 아래 오류 메시지와 서버 로그를 확인해주세요."
    };
  }

  if (stage === "ready") {
    return {
      title: "결제 준비 완료",
      badge: "READY",
      badgeClass: "bg-[#e8f8ef] text-[#198754]",
      description: "주문이 결제 대기 상태입니다. 카카오페이 결제를 진행할 수 있습니다."
    };
  }

  return {
    title: "주문 검증 대기",
    badge: "WAIT",
    badgeClass: "bg-[#edf4ff] text-[#267cc2]",
    description: "store-service가 주문 가능 여부를 검증하고 있습니다. 결제 가능 상태가 되면 버튼이 활성화됩니다."
  };
}
