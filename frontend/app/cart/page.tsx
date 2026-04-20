"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { decodeJwtPayload, getTokenBundle } from "@/lib/auth-storage";
import { CartSnapshot, clearCart, getCart, getCartTotals, updateCartItemQuantity } from "@/lib/cart-storage";
import { apiRequest, HttpError } from "@/lib/http";
import { CreateOrderRequest, CreateOrderResponse } from "@/lib/types";
import { foodImage, formatWon } from "@/components/store-detail/store-ui-utils";

const DEFAULT_DELIVERY_ADDRESS = "경기 군포시 오금로 16 322동 104호";

export default function CartPage() {
  const [cart, setCart] = useState<CartSnapshot | null>(null);
  const [deliveryAddress, setDeliveryAddress] = useState(DEFAULT_DELIVERY_ADDRESS);
  const [deliveryTip, setDeliveryTip] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    setCart(getCart());
  }, []);

  const totals = getCartTotals(cart);
  const paymentAmount = totals.totalAmount + deliveryTip;

  const orderItems = useMemo(() => {
    if (!cart) return [];
    return cart.items.map((item) => ({
      menuId: item.menuId,
      menuName: item.options.length > 0 ? `${item.menuName} (${item.options.map((option) => option.optionName).join(", ")})` : item.menuName,
      unitPrice: item.unitPrice,
      quantity: item.quantity
    }));
  }, [cart]);

  function refreshCart(nextCart: CartSnapshot | null) {
    setCart(nextCart);
  }

  function changeQuantity(cartItemId: string, quantity: number) {
    refreshCart(updateCartItemQuantity(cartItemId, quantity));
  }

  async function handleCreateOrder() {
    if (!cart || cart.items.length === 0) return;

    setLoading(true);
    setError("");

    try {
      const tokenBundle = getTokenBundle();
      if (!tokenBundle?.accessToken) {
        throw new Error("로그인이 필요합니다. 먼저 로그인한 뒤 다시 주문해주세요.");
      }

      const payload = decodeJwtPayload(tokenBundle.accessToken);
      const userId = Number(payload?.user_id ?? payload?.sub);
      if (!Number.isInteger(userId) || userId <= 0) {
        throw new Error("토큰에서 userId를 확인할 수 없습니다. 다시 로그인해주세요.");
      }

      const body: CreateOrderRequest = {
        userId,
        storeId: cart.storeId,
        deliveryAddress: deliveryAddress.trim() || DEFAULT_DELIVERY_ADDRESS,
        usedPointAmount: 0,
        items: orderItems
      };

      const response = await apiRequest<CreateOrderResponse>("/api/v1/orders", {
        method: "POST",
        token: tokenBundle.accessToken,
        headers: {
          "X-Idempotency-Key": generateUuidV4()
        },
        body
      });

      clearCart();
      window.location.href = `/payment/checkout?orderId=${response.orderId}&amount=${response.finalAmount}`;
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  if (!cart || cart.items.length === 0) {
    return (
      <main className="mx-auto grid min-h-screen max-w-[430px] place-items-center bg-white px-6 text-center shadow-[0_24px_80px_rgba(15,23,42,0.18)] md:rounded-[36px]">
        <div>
          <h1 className="text-[26px] font-black tracking-[-0.06em] text-[#20242c]">카트가 비었어요</h1>
          <p className="mt-3 text-[15px] font-bold leading-6 text-[#7b8492]">먹고 싶은 메뉴를 담으면 여기서 주문하고 결제까지 이어갈 수 있어요.</p>
          <Link href="/" className="mt-7 inline-flex h-14 items-center justify-center rounded-2xl bg-[#52aef5] px-6 text-[16px] font-black text-white">
            가게 보러가기
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto min-h-screen max-w-[430px] bg-white pb-40 shadow-[0_24px_80px_rgba(15,23,42,0.18)] md:rounded-[36px]">
      <header className="sticky top-0 z-30 flex items-center gap-4 border-b border-[#edf0f4] bg-white/95 px-5 py-5 backdrop-blur-xl">
        <Link href={`/store/detail?storeId=${cart.storeId}`} className="grid h-10 w-10 place-items-center text-[32px] text-[#20242c]">×</Link>
        <h1 className="text-[25px] font-black tracking-[-0.06em] text-[#20242c]">카트</h1>
      </header>

      <section className="bg-white px-5 py-6">
        <p className="text-[14px] font-bold text-[#7b8492]">산본3단지다산아파트(으)로 배달</p>
        <div className="mt-2 flex items-center justify-between gap-3">
          <input
            value={deliveryAddress}
            onChange={(event) => setDeliveryAddress(event.target.value)}
            className="min-w-0 flex-1 border-0 bg-transparent text-[19px] font-black tracking-[-0.05em] text-[#20242c] outline-none"
            aria-label="배달 주소"
          />
          <button type="button" className="shrink-0 text-[14px] font-black text-[#2f9be8]">수정</button>
        </div>
      </section>

      <section className="border-y-[10px] border-[#f4f5f7] bg-[#f3f7ff] px-5 py-5">
        <h2 className="text-[18px] font-black text-[#123d79]">WOW! 와우 전용 배달비 혜택</h2>
        <div className="mt-4 space-y-3">
          <label className={`flex cursor-pointer items-center justify-between rounded-2xl border-2 bg-white px-5 py-4 ${deliveryTip === 0 ? "border-[#317dc9]" : "border-transparent"}`}>
            <div className="flex items-center gap-4">
              <input type="radio" name="deliveryTip" checked={deliveryTip === 0} onChange={() => setDeliveryTip(0)} className="h-6 w-6 accent-[#52aef5]" />
              <div>
                <p className="text-[17px] font-black text-[#20242c]">배달비 0원</p>
                <p className="mt-1 text-[13px] font-bold text-[#7b8492]">21-36분</p>
              </div>
            </div>
            <span className="text-[16px] font-black text-[#d85b50]">0원</span>
          </label>
          <label className={`flex cursor-pointer items-center justify-between rounded-2xl border-2 bg-white px-5 py-4 ${deliveryTip === 1000 ? "border-[#317dc9]" : "border-transparent"}`}>
            <div className="flex items-center gap-4">
              <input type="radio" name="deliveryTip" checked={deliveryTip === 1000} onChange={() => setDeliveryTip(1000)} className="h-6 w-6 accent-[#52aef5]" />
              <div>
                <p className="text-[17px] font-black text-[#20242c]">한집배달</p>
                <p className="mt-1 text-[13px] font-bold text-[#7b8492]">19-29분</p>
              </div>
            </div>
            <span className="text-[16px] font-black text-[#20242c]">+1,000원</span>
          </label>
        </div>
        <p className="mt-4 text-[13px] font-bold text-[#6f7885]">배달비 0원으로 주문 시 가까운 주문과 함께 배달될 수 있어요.</p>
      </section>

      <section className="bg-white py-5">
        <div className="px-5 pb-4">
          <h2 className="text-[21px] font-black tracking-[-0.05em] text-[#20242c]">{cart.storeName}</h2>
        </div>

        <div className="divide-y divide-[#edf0f4] border-y border-[#edf0f4]">
          {cart.items.map((item) => (
            <article key={item.cartItemId} className="px-5 py-5">
              <div className="flex gap-4">
                <img src={foodImage(item.menuId, item.imageUrl)} alt={item.menuName} className="h-20 w-20 rounded-2xl object-cover" />
                <div className="min-w-0 flex-1">
                  <h3 className="line-clamp-1 text-[17px] font-black text-[#20242c]">{item.menuName}</h3>
                  {item.options.length > 0 && (
                    <p className="mt-1 line-clamp-2 text-[13px] font-bold leading-5 text-[#8a929e]">
                      {item.options.map((option) => `${option.optionName}${option.extraPrice > 0 ? ` +${formatWon(option.extraPrice)}` : ""}`).join(", ")}
                    </p>
                  )}
                  <p className="mt-2 text-[16px] font-black text-[#20242c]">{formatWon(item.unitPrice * item.quantity)}</p>
                </div>
                <div className="flex h-12 items-center overflow-hidden rounded-full bg-[#52aef5] text-white">
                  <button type="button" className="grid h-12 w-12 place-items-center text-[18px] font-black" onClick={() => changeQuantity(item.cartItemId, item.quantity - 1)}>
                    {item.quantity === 1 ? "삭제" : "-"}
                  </button>
                  <span className="min-w-8 text-center text-[17px] font-black">{item.quantity}</span>
                  <button type="button" className="grid h-12 w-12 place-items-center text-[22px] font-black" onClick={() => changeQuantity(item.cartItemId, item.quantity + 1)}>+</button>
                </div>
              </div>
            </article>
          ))}
        </div>

        <div className="flex justify-end px-5 pt-5">
          <Link href={`/store/detail?storeId=${cart.storeId}`} className="rounded-full border-2 border-[#52aef5] px-5 py-3 text-[15px] font-black text-[#52aef5]">
            + 메뉴 추가
          </Link>
        </div>
      </section>

      <section className="border-y-[10px] border-[#f4f5f7] bg-white px-5 py-5">
        <h2 className="text-[19px] font-black text-[#20242c]">결제 예정 금액</h2>
        <div className="mt-4 space-y-3 text-[15px] font-bold text-[#68717f]">
          <div className="flex justify-between"><span>주문 금액</span><span>{formatWon(totals.totalAmount)}</span></div>
          <div className="flex justify-between"><span>배달비</span><span>{deliveryTip === 0 ? "0원" : formatWon(deliveryTip)}</span></div>
          <div className="flex justify-between border-t border-[#edf0f4] pt-3 text-[20px] font-black text-[#20242c]"><span>총 결제 금액</span><span>{formatWon(paymentAmount)}</span></div>
        </div>
        {error && <p className="mt-4 rounded-2xl bg-[#fff1f0] px-4 py-3 text-[13px] font-bold text-[#d85b50]">{error}</p>}
      </section>

      <div className="fixed bottom-0 left-1/2 z-40 w-full max-w-[430px] -translate-x-1/2 overflow-hidden bg-white shadow-[0_-12px_32px_rgba(15,23,42,0.14)]">
        <div className="bg-[#123d79] px-5 py-3 text-[14px] font-black text-white">WOW! 와우회원 배달비 0원 혜택 적용</div>
        <button type="button" onClick={handleCreateOrder} disabled={loading} className="h-[76px] w-full bg-[#52aef5] text-[19px] font-black text-white disabled:bg-[#b4d8f2]">
          {loading ? "주문 생성 중..." : `배달주문 ${formatWon(paymentAmount)} 결제하기`}
        </button>
      </div>
    </main>
  );
}

function toErrorMessage(caught: unknown): string {
  if (caught instanceof HttpError) return `${caught.code ?? "HTTP_ERROR"}: ${caught.message}`;
  if (caught instanceof Error) return caught.message;
  return "요청 실패";
}

function generateUuidV4(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }

  return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, (char) => {
    const value = Number(char);
    return (value ^ (Math.random() * 16) >> (value / 4)).toString(16);
  });
}
