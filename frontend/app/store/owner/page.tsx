"use client";

import { useEffect, useMemo, useState } from "react";
import { SessionPanel } from "@/components/SessionPanel";
import { decodeJwtPayload, getTokenBundle } from "@/lib/auth-storage";
import { ensureDevSession } from "@/lib/dev-session";
import { apiRequest, HttpError } from "@/lib/http";
import { OwnerStoreSummaryResponse, StoreOrderDecisionResponse, StoreOrderResponse } from "@/lib/types";

type AuthContext = { accessToken: string };
type OrderTab = "PENDING" | "ACCEPTED" | "REJECTED";

export default function OwnerStorePage() {
  const [stores, setStores] = useState<OwnerStoreSummaryResponse[]>([]);
  const [selectedStoreId, setSelectedStoreId] = useState<number | null>(null);
  const [pendingOrders, setPendingOrders] = useState<StoreOrderResponse[]>([]);
  const [acceptedOrders, setAcceptedOrders] = useState<StoreOrderResponse[]>([]);
  const [rejectedOrders, setRejectedOrders] = useState<StoreOrderResponse[]>([]);
  const [tab, setTab] = useState<OrderTab>("PENDING");
  const [loading, setLoading] = useState(false);
  const [mutatingOrderId, setMutatingOrderId] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const selectedStore = useMemo(
    () => stores.find((store) => store.storeId === selectedStoreId) ?? null,
    [selectedStoreId, stores]
  );

  const visibleOrders = useMemo(() => {
    if (tab === "ACCEPTED") {
      return acceptedOrders;
    }
    if (tab === "REJECTED") {
      return rejectedOrders;
    }
    return pendingOrders;
  }, [acceptedOrders, pendingOrders, rejectedOrders, tab]);

  useEffect(() => {
    void bootstrap();
  }, []);

  useEffect(() => {
    if (!selectedStoreId) {
      setPendingOrders([]);
      setAcceptedOrders([]);
      setRejectedOrders([]);
      return;
    }
    void loadOrders(selectedStoreId);
  }, [selectedStoreId]);

  async function bootstrap() {
    setLoading(true);
    setError("");
    try {
      await ensureDevSession();
      const auth = resolveAuthContext();
      const nextStores = await apiRequest<OwnerStoreSummaryResponse[]>("/api/v1/owner/stores", {
        method: "GET",
        token: auth.accessToken
      });
      setStores(nextStores);
      setSelectedStoreId((prev) => {
        if (nextStores.length === 0) {
          return null;
        }
        if (prev && nextStores.some((store) => store.storeId === prev)) {
          return prev;
        }
        return nextStores[0].storeId;
      });
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function loadOrders(storeId: number) {
    setLoading(true);
    setError("");
    try {
      const auth = resolveAuthContext();
      const [pending, accepted, rejected] = await Promise.all([
        fetchStoreOrders(storeId, "PENDING", auth.accessToken),
        fetchStoreOrders(storeId, "ACCEPTED", auth.accessToken),
        fetchStoreOrders(storeId, "REJECTED", auth.accessToken)
      ]);
      setPendingOrders(pending);
      setAcceptedOrders(accepted);
      setRejectedOrders(rejected);
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function decide(order: StoreOrderResponse, decision: "ACCEPTED" | "REJECTED") {
    if (!selectedStoreId) {
      return;
    }

    let rejectReason: string | null = null;
    if (decision === "REJECTED") {
      rejectReason = window.prompt("거절 사유", "재료 소진");
      if (rejectReason === null) {
        return;
      }
    }

    setMutatingOrderId(order.orderId);
    setError("");
    setSuccess("");

    try {
      const auth = resolveAuthContext();
      const response = await apiRequest<StoreOrderDecisionResponse>(
        `/api/v1/owner/stores/${selectedStoreId}/orders/${order.orderId}/decision`,
        {
          method: "POST",
          token: auth.accessToken,
          body: {
            decision,
            rejectReason: rejectReason?.trim() || undefined
          }
        }
      );

      setSuccess(
        decision === "ACCEPTED"
          ? `주문 #${response.orderId} 수락 완료. 고객 화면에 곧 반영됩니다.`
          : `주문 #${response.orderId} 거절 완료. 환불 대기 플로우로 넘어갑니다.`
      );
      await loadOrders(selectedStoreId);
      setTab(decision);
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setMutatingOrderId(null);
    }
  }

  async function refreshAll() {
    await bootstrap();
    if (selectedStoreId) {
      await loadOrders(selectedStoreId);
    }
  }

  return (
    <div className="min-h-screen bg-[#f6f2e9] text-[#20242c]">
      <main className="mx-auto flex max-w-[1180px] flex-col gap-5 px-4 py-6 md:px-8">
        <section className="relative overflow-hidden rounded-[32px] bg-[#1d2b22] p-6 text-white shadow-[0_24px_80px_rgba(29,43,34,0.22)] md:p-8">
          <div className="absolute -right-16 -top-20 h-56 w-56 rounded-full bg-[#f6c85f]/30 blur-3xl" />
          <div className="absolute -bottom-24 left-20 h-56 w-56 rounded-full bg-[#67d59b]/25 blur-3xl" />
          <div className="relative z-10 flex flex-col justify-between gap-5 md:flex-row md:items-end">
            <div>
              <p className="text-sm font-black tracking-[0.22em] text-[#f6c85f]">OWNER CONSOLE</p>
              <h1 className="mt-3 text-[34px] font-black tracking-[-0.05em] md:text-[48px]">사장님 주문 처리</h1>
              <p className="mt-3 max-w-[620px] text-sm leading-6 text-white/70">
                결제 완료된 주문이 여기로 들어옵니다. 수락하면 주문은 ACCEPTED, 거절하면 REFUND_PENDING 플로우로 넘어갑니다.
              </p>
            </div>
            <button
              className="rounded-2xl bg-white px-5 py-3 text-sm font-black text-[#1d2b22] shadow-[0_12px_30px_rgba(0,0,0,0.18)] disabled:opacity-50"
              onClick={refreshAll}
              disabled={loading}
            >
              {loading ? "새로고침 중..." : "전체 새로고침"}
            </button>
          </div>
        </section>

        {(error || success) && (
          <section className="rounded-[24px] bg-white p-5 shadow-[0_16px_50px_rgba(42,32,17,0.08)]">
            {success && <p className="text-sm font-black text-[#16864d]">{success}</p>}
            {error && <p className="mt-2 text-sm font-black text-[#d64545]">{error}</p>}
          </section>
        )}

        <div className="grid gap-5 lg:grid-cols-[340px_1fr]">
          <aside className="rounded-[28px] bg-white p-5 shadow-[0_16px_50px_rgba(42,32,17,0.08)]">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-black tracking-[-0.03em]">내 가게</h2>
              <span className="rounded-full bg-[#f2f6ef] px-3 py-1 text-xs font-black text-[#58704f]">{stores.length}개</span>
            </div>

            <div className="mt-4 flex flex-col gap-3">
              {stores.length === 0 && <p className="text-sm font-bold text-[#7b8492]">등록된 가게가 없습니다.</p>}
              {stores.map((store) => {
                const selected = store.storeId === selectedStoreId;
                return (
                  <button
                    key={store.storeId}
                    type="button"
                    className={`rounded-[22px] border p-4 text-left transition ${selected ? "border-[#1d2b22] bg-[#eef8ef]" : "border-[#edf0e8] bg-white hover:bg-[#fbfaf6]"}`}
                    onClick={() => setSelectedStoreId(store.storeId)}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-[17px] font-black tracking-[-0.03em]">{store.name}</p>
                        <p className="mt-1 text-xs font-bold text-[#7b8492]">#{store.storeId} · {store.status}</p>
                      </div>
                      {store.storeLogoUrl ? (
                        <img className="h-12 w-12 rounded-2xl object-cover" src={store.storeLogoUrl} alt="" />
                      ) : (
                        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#f4f0e6] text-lg font-black">店</div>
                      )}
                    </div>
                    <div className="mt-3 grid grid-cols-2 gap-2 text-xs font-black text-[#536174]">
                      <div className="rounded-2xl bg-white/70 p-2">최소 {formatWon(store.minOrderAmount)}</div>
                      <div className="rounded-2xl bg-white/70 p-2">배달팁 {formatWon(store.deliveryTip)}</div>
                    </div>
                  </button>
                );
              })}
            </div>
          </aside>

          <section className="flex flex-col gap-5">
            <div className="rounded-[28px] bg-white p-5 shadow-[0_16px_50px_rgba(42,32,17,0.08)]">
              <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
                <div>
                  <p className="text-xs font-black tracking-[0.18em] text-[#b18b2f]">ORDER QUEUE</p>
                  <h2 className="mt-1 text-2xl font-black tracking-[-0.04em]">{selectedStore?.name ?? "가게를 선택해주세요"}</h2>
                </div>
                <div className="grid grid-cols-3 gap-2 rounded-[20px] bg-[#f4f0e6] p-1">
                  <TabButton active={tab === "PENDING"} onClick={() => setTab("PENDING")} label={`대기 ${pendingOrders.length}`} />
                  <TabButton active={tab === "ACCEPTED"} onClick={() => setTab("ACCEPTED")} label={`수락 ${acceptedOrders.length}`} />
                  <TabButton active={tab === "REJECTED"} onClick={() => setTab("REJECTED")} label={`거절 ${rejectedOrders.length}`} />
                </div>
              </div>
            </div>

            <div className="grid gap-4">
              {visibleOrders.length === 0 && (
                <div className="rounded-[28px] border border-dashed border-[#d9d2c2] bg-white/70 p-10 text-center">
                  <p className="text-lg font-black">표시할 주문이 없습니다.</p>
                  <p className="mt-2 text-sm font-bold text-[#7b8492]">결제 완료 후 PAID 상태가 되면 대기 주문에 들어옵니다.</p>
                </div>
              )}

              {visibleOrders.map((order) => (
                <article key={order.orderId} className="overflow-hidden rounded-[30px] bg-white shadow-[0_18px_55px_rgba(42,32,17,0.10)]">
                  <div className="flex flex-col gap-4 p-5 md:flex-row md:items-center md:justify-between">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className={`rounded-full px-3 py-1 text-xs font-black ${statusClass(order.status)}`}>{order.status}</span>
                        <span className="text-xs font-bold text-[#93a0a8]">order #{order.orderId}</span>
                      </div>
                      <h3 className="mt-3 text-[28px] font-black tracking-[-0.05em]">{formatWon(order.finalAmount ?? 0)}</h3>
                      <p className="mt-1 text-sm font-bold text-[#687386]">user #{order.userId ?? "-"} · 결제시각 {formatDateTime(order.paidAt ?? order.createdAt)}</p>
                      {order.rejectReason && <p className="mt-2 text-sm font-bold text-[#d64545]">거절 사유: {order.rejectReason}</p>}
                    </div>

                    {order.status === "PENDING" ? (
                      <div className="flex min-w-[240px] gap-2">
                        <button
                          className="flex-1 rounded-2xl bg-[#1d2b22] px-4 py-3 text-sm font-black text-white disabled:opacity-50"
                          disabled={mutatingOrderId === order.orderId}
                          onClick={() => decide(order, "ACCEPTED")}
                        >
                          {mutatingOrderId === order.orderId ? "처리 중" : "수락"}
                        </button>
                        <button
                          className="flex-1 rounded-2xl bg-[#fff0f0] px-4 py-3 text-sm font-black text-[#d64545] disabled:opacity-50"
                          disabled={mutatingOrderId === order.orderId}
                          onClick={() => decide(order, "REJECTED")}
                        >
                          거절
                        </button>
                      </div>
                    ) : (
                      <div className="min-w-[180px] rounded-2xl bg-[#f7f8fb] p-4 text-right">
                        <p className="text-xs font-bold text-[#93a0a8]">처리 완료</p>
                        <p className="mt-1 text-sm font-black">{formatDateTime(order.decidedAt)}</p>
                      </div>
                    )}
                  </div>
                </article>
              ))}
            </div>
          </section>
        </div>

        <SessionPanel />
      </main>
    </div>
  );
}

function TabButton({ active, label, onClick }: { active: boolean; label: string; onClick: () => void }) {
  return (
    <button
      className={`rounded-2xl px-4 py-2 text-sm font-black transition ${active ? "bg-white text-[#1d2b22] shadow-sm" : "text-[#7b6d55] hover:bg-white/50"}`}
      onClick={onClick}
      type="button"
    >
      {label}
    </button>
  );
}

async function fetchStoreOrders(storeId: number, status: OrderTab, token: string): Promise<StoreOrderResponse[]> {
  return apiRequest<StoreOrderResponse[]>(`/api/v1/owner/stores/${storeId}/orders?status=${status}`, {
    method: "GET",
    token
  });
}

function resolveAuthContext(): AuthContext {
  const bundle = getTokenBundle();
  if (!bundle?.accessToken) {
    throw new Error("Login required.");
  }
  const payload = decodeJwtPayload(bundle.accessToken);
  const userId = payload?.user_id ?? (payload?.sub ? Number(payload.sub) : NaN);
  if (!Number.isFinite(userId) || userId <= 0) {
    throw new Error("Invalid access token payload.");
  }
  return { accessToken: bundle.accessToken };
}

function formatWon(amount: number): string {
  return `${amount.toLocaleString("ko-KR")}원`;
}

function formatDateTime(value?: string | null): string {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

function statusClass(status: string): string {
  if (status === "ACCEPTED") {
    return "bg-[#e8f8ef] text-[#16864d]";
  }
  if (status === "REJECTED") {
    return "bg-[#fff0f0] text-[#d64545]";
  }
  return "bg-[#fff8d8] text-[#9a7500]";
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
