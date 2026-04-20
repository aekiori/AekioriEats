"use client";

import Link from "next/link";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { addCartItem, CartSnapshot, getCart, getCartTotals } from "@/lib/cart-storage";
import { apiRequest, HttpError } from "@/lib/http";
import { StoreDetailQueryOptionGroup, StoreDetailQueryResponse } from "@/lib/types";
import { MenuCardViewModel, MenuGroupSection } from "@/components/store-detail/MenuGroupSection";
import { AddMenuToCartPayload, MenuOptionSheet } from "@/components/store-detail/MenuOptionSheet";
import { foodImage, formatWon, isStoreOpenNow } from "@/components/store-detail/store-ui-utils";

type MenuOptionItem = {
  menuId: number;
  name: string;
  description: string | null;
  price: number;
  imageUrl: string | null;
  optionGroups: StoreDetailQueryOptionGroup[];
};

const DEFAULT_STORE_ID = "10101";

export default function StoreDetailPage() {
  const [storeIdInput, setStoreIdInput] = useState(DEFAULT_STORE_ID);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [store, setStore] = useState<StoreDetailQueryResponse | null>(null);
  const [selectedMenuId, setSelectedMenuId] = useState<number | null>(null);
  const [cart, setCart] = useState<CartSnapshot | null>(null);
  const [cartMessage, setCartMessage] = useState("");

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const storeId = params.get("storeId") ?? DEFAULT_STORE_ID;
    setStoreIdInput(storeId);
    setCart(getCart());
    void loadStore(storeId);
  }, []);

  const groups = useMemo(() => {
    if (!store) return [];
    return store.menuGroups.map((group, groupIndex) => ({
      groupId: group.id,
      groupName: group.id > 0 ? group.name : "기타 메뉴",
      items: group.menus.map<MenuCardViewModel>((menu, menuIndex) => ({
        menuId: menu.id,
        name: menu.name,
        description: menu.description,
        price: menu.price,
        imageUrl: menu.imageUrl,
        badges: (menu.tags ?? []).map((tag) => tag.name).slice(0, 2),
        orderCount: 120 + groupIndex * 37 + menuIndex * 19,
        reviewCount: 31 + groupIndex * 12 + menuIndex * 7,
        likeRate: 97 + ((groupIndex + menuIndex) % 3)
      }))
    }));
  }, [store]);

  const flatMenus = useMemo<MenuOptionItem[]>(() => {
    if (!store) return [];
    return store.menuGroups.flatMap((group) =>
      group.menus.map((menu) => ({
        menuId: menu.id,
        name: menu.name,
        description: menu.description,
        price: menu.price,
        imageUrl: menu.imageUrl,
        optionGroups: menu.optionGroups ?? []
      }))
    );
  }, [store]);

  const selectedMenu = useMemo(() => {
    if (selectedMenuId === null) return null;
    return flatMenus.find((menu) => menu.menuId === selectedMenuId) ?? null;
  }, [flatMenus, selectedMenuId]);

  const activeCart = store && cart?.storeId === store.storeId ? cart : null;
  const cartTotals = getCartTotals(activeCart);
  const storeOpenNow = store ? isStoreOpenNow(store.operatingHours) : false;

  async function loadStore(storeIdValue: string) {
    setLoading(true);
    setError("");

    try {
      const storeId = parsePositiveInt(storeIdValue, "Store ID");
      const response = await apiRequest<StoreDetailQueryResponse>(`/api/v1/stores/${storeId}`, { method: "GET" });
      setStore(response);
      setSelectedMenuId(null);
    } catch (caught) {
      setStore(null);
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function handleLoadStore(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await loadStore(storeIdInput);
  }

  function handleAddToCart(payload: AddMenuToCartPayload) {
    if (!store) return;
    if (!storeOpenNow) {
      setSelectedMenuId(null);
      setCartMessage("마감된 가게라 지금은 주문할 수 없어요");
      window.setTimeout(() => setCartMessage(""), 2200);
      return;
    }

    const nextCart = addCartItem({
      storeId: store.storeId,
      storeName: store.name,
      menuId: payload.menuId,
      menuName: payload.menuName,
      basePrice: payload.basePrice,
      unitPrice: payload.unitPrice,
      quantity: payload.quantity,
      imageUrl: payload.imageUrl,
      options: payload.options
    });

    setCart(nextCart);
    setSelectedMenuId(null);
    setCartMessage(`${payload.menuName} 담았어요`);
    window.setTimeout(() => setCartMessage(""), 1800);
  }

  return (
    <div className="mx-auto min-h-screen max-w-[430px] overflow-hidden bg-white shadow-[0_24px_80px_rgba(15,23,42,0.18)] md:rounded-[36px]">
      {loading && (
        <div className="grid min-h-screen place-items-center bg-white px-6 text-center">
          <div>
            <p className="text-[13px] font-black uppercase tracking-[0.18em] text-[#52aef5]">AekioriEats</p>
            <h1 className="mt-2 text-[24px] font-black tracking-[-0.06em] text-[#20242c]">가게 정보를 불러오는 중...</h1>
          </div>
        </div>
      )}

      {!loading && !store && (
        <div className="min-h-screen bg-white px-5 py-8">
          <h1 className="text-[24px] font-black tracking-[-0.06em] text-[#20242c]">가게를 불러오지 못했어요</h1>
          <p className="mt-3 rounded-2xl bg-[#fff1f0] px-4 py-3 text-[13px] font-bold text-[#d85b50]">{error}</p>
          <form className="mt-5 flex gap-2" onSubmit={handleLoadStore}>
            <input
              className="h-14 min-w-0 flex-1 rounded-2xl border-0 bg-[#f3f5f8] px-4 text-[15px] font-bold text-[#20242c] outline-none"
              value={storeIdInput}
              onChange={(event) => setStoreIdInput(event.target.value)}
              placeholder="스토어 ID"
            />
            <button className="rounded-2xl bg-[#20242c] px-4 text-[13px] font-black text-white">조회</button>
          </form>
        </div>
      )}

      {!loading && store && (
        <>
          <section className="relative h-[330px] overflow-hidden bg-[#1a1f2b] text-white">
            <img src={foodImage(store.storeId, store.images.storeLogoUrl)} alt={store.name} className={`h-full w-full object-cover opacity-80 ${storeOpenNow ? "" : "grayscale"}`} />
            <div className="absolute inset-0 bg-gradient-to-b from-black/55 via-black/10 to-black/65" />
            {!storeOpenNow && (
              <div className="absolute inset-0 z-10 grid place-items-center bg-black/45 text-center">
                <div className="rounded-[24px] border border-white/35 bg-white/12 px-7 py-5 text-white backdrop-blur-sm">
                  <p className="text-[28px] font-black tracking-[-0.06em]">마감했습니다</p>
                  <p className="mt-2 text-[13px] font-bold text-white/80">영업시간에 다시 주문해주세요</p>
                </div>
              </div>
            )}

            <div className="absolute left-0 right-0 top-0 z-20 flex items-center justify-between px-5 pt-5">
              <button className="grid h-11 w-11 place-items-center rounded-full bg-black/25 text-[28px] backdrop-blur-md" type="button" onClick={() => history.back()}>‹</button>
              <div className="flex gap-2">
                <button className="grid h-11 w-11 place-items-center rounded-full bg-black/25 text-[14px] font-black backdrop-blur-md" type="button">공유</button>
                <button className="grid h-11 w-11 place-items-center rounded-full bg-black/25 text-[14px] font-black backdrop-blur-md" type="button">찜</button>
              </div>
            </div>

            <span className="absolute bottom-[112px] left-5 z-20 rounded-full bg-black/55 px-3 py-1 text-[12px] font-bold">1 / 3</span>

            <div className="absolute bottom-6 left-5 right-5 z-20 rounded-[26px] bg-white px-5 py-5 text-center text-[#20242c] shadow-[0_16px_36px_rgba(0,0,0,0.22)]">
              <div className="flex items-center justify-center gap-2">
                <h1 className="text-[24px] font-black tracking-[-0.05em]">{store.name}</h1>
                {!storeOpenNow && <span className="rounded-md bg-[#eef1f5] px-2 py-1 text-[11px] font-black text-[#7b8492]">마감</span>}
              </div>
              <div className="mt-2 flex items-center justify-center gap-2 text-[16px] font-extrabold">
                <span className="text-[#ffc928]">★</span>
                <span>{(4.6 + ((store.storeId % 4) / 10)).toFixed(1)}</span>
                <span className="text-[#7d8591]">({120 + (store.storeId % 900)})</span>
                <span className="text-[#9aa3ad]">›</span>
              </div>
              <div className="mt-4 flex justify-center gap-2">
                <span className="rounded-lg bg-[#eaf5ff] px-3 py-1.5 text-[13px] font-black text-[#2b78bf]">{store.deliveryPolicy.deliveryTip === 0 ? "WOW! 배달비 0원" : `배달비 ${formatWon(store.deliveryPolicy.deliveryTip)}`}</span>
                <span className="rounded-lg bg-[#f2f6fb] px-3 py-1.5 text-[13px] font-black text-[#4e5968]">리뷰이벤트</span>
              </div>
            </div>
          </section>

          <section className="border-b border-[#edf0f4] bg-white px-5 py-5">
            <form className="flex gap-2" onSubmit={handleLoadStore}>
              <label className="relative flex-1">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-[20px] text-[#8b95a1]">검색</span>
                <input
                  className="h-14 w-full rounded-2xl border-0 bg-[#f3f5f8] py-4 pl-16 pr-4 text-[15px] font-bold text-[#20242c] outline-none placeholder:text-[#9aa3ad]"
                  value={storeIdInput}
                  onChange={(event) => setStoreIdInput(event.target.value)}
                  placeholder="스토어 ID 입력"
                />
              </label>
              <button className="rounded-2xl bg-[#20242c] px-4 text-[13px] font-black text-white" disabled={loading}>
                조회
              </button>
            </form>
            {error && <p className="mt-3 rounded-2xl bg-[#fff1f0] px-4 py-3 text-[13px] font-bold text-[#d85b50]">{error}</p>}
          </section>

          <section className="bg-white px-5 py-5">
            <div className="grid grid-cols-2 border-b border-[#edf0f4] pb-4 text-center">
              <div>
                <p className="text-[20px] font-black text-[#52aef5]">배달 {24 + (store.storeId % 18)}분</p>
                <p className="mt-1 text-[12px] font-bold text-[#9aa3ad]">도착까지 예상 시간</p>
              </div>
              <div>
                <p className="text-[20px] font-black text-[#20242c]">포장 15분</p>
                <p className="mt-1 text-[12px] font-bold text-[#9aa3ad]">바로 픽업 가능</p>
              </div>
            </div>

            <div className="mt-5 space-y-3 text-[15px]">
              <div className="flex justify-between">
                <span className="font-bold text-[#8a929e]">최소주문</span>
                <span className="font-black text-[#20242c]">{formatWon(store.deliveryPolicy.minOrderAmount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="font-bold text-[#8a929e]">배달비</span>
                <span className="font-black text-[#20242c]">{store.deliveryPolicy.deliveryTip === 0 ? "무료" : formatWon(store.deliveryPolicy.deliveryTip)}</span>
              </div>
            </div>

            <div className="mt-5 rounded-2xl bg-[#123d79] px-4 py-4 text-white shadow-[0_12px_28px_rgba(18,61,121,0.22)]">
              <div className="flex items-center justify-between">
                <span className="text-[19px] font-black">WOW!</span>
                <span className="text-[14px] font-extrabold">와우회원은 매 주문 배달비 0원</span>
              </div>
            </div>

            {!storeOpenNow && (
              <div className="mt-4 rounded-2xl bg-[#f2f4f7] px-4 py-4 text-[14px] font-black text-[#6f7885]">
                지금은 마감 상태라 카트에 담아도 주문 검증에서 거절돼요.
              </div>
            )}
          </section>

          <section className="border-y-[10px] border-[#f4f5f7] bg-white px-5 py-5">
            <p className="text-[13px] font-black text-[#e8563f]">실시간 인기 리뷰</p>
            <div className="mt-3 flex gap-3 overflow-x-auto pb-1">
              {[0, 1, 2].map((index) => (
                <div key={index} className="w-[260px] shrink-0 rounded-2xl border border-[#e7ebf0] bg-white p-3 shadow-[0_10px_26px_rgba(20,28,38,0.06)]">
                  <div className="flex gap-3">
                    <img src={foodImage(index + 20)} alt="review" className="h-20 w-20 rounded-xl object-cover" />
                    <div>
                      <p className="line-clamp-2 text-[13px] font-bold leading-5 text-[#59616d]">처음 시켜봤는데 너무 맛있어요. 포장도 깔끔하고 다음에도 주문할게요.</p>
                      <p className="mt-2 text-[#ffc928]">★★★★★</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>

          <nav className="sticky top-0 z-20 flex gap-2 overflow-x-auto border-b border-[#edf0f4] bg-white px-5 py-4 shadow-[0_8px_18px_rgba(20,28,38,0.05)]">
            {groups.map((group, index) => (
              <a
                key={group.groupId}
                href={`#menu-group-${group.groupName}`}
                className={`shrink-0 border-b-4 px-2 pb-2 text-[15px] font-black tracking-[-0.04em] ${index === 0 ? "border-[#2d223f] text-[#20242c]" : "border-transparent text-[#6f7885]"}`}
              >
                {group.groupName}
              </a>
            ))}
          </nav>

          {groups.map((group) => (
            <MenuGroupSection key={group.groupId} title={group.groupName} items={group.items} onSelectMenu={(menuId) => setSelectedMenuId(menuId)} />
          ))}

          <div className="h-28 bg-white" />
          {cartMessage && (
            <div className="fixed bottom-24 left-1/2 z-40 w-[calc(100%-32px)] max-w-[398px] -translate-x-1/2 rounded-2xl bg-[#20242c] px-4 py-3 text-center text-[14px] font-black text-white shadow-[0_12px_28px_rgba(15,23,42,0.25)]">
              {cartMessage}
            </div>
          )}
          {activeCart && cartTotals.itemCount > 0 && (
            <Link href="/cart" className="fixed bottom-0 left-1/2 z-30 w-full max-w-[430px] -translate-x-1/2 overflow-hidden bg-white shadow-[0_-12px_32px_rgba(15,23,42,0.14)]">
              <div className="bg-[#123d79] px-5 py-3 text-[14px] font-black text-white">WOW! 와우회원 배달비 0원 혜택 적용</div>
              <div className="flex items-center justify-between bg-[#52aef5] px-5 py-5 text-white">
                <span className="flex items-center gap-3 text-[18px] font-black">
                  <span className="grid h-8 w-8 place-items-center rounded-full bg-white text-[15px] text-[#52aef5]">{cartTotals.itemCount}</span>
                  카트 보기
                </span>
                <span className="text-[18px] font-black">{formatWon(cartTotals.totalAmount)}</span>
              </div>
            </Link>
          )}
          <MenuOptionSheet open={selectedMenu !== null} menu={selectedMenu} onClose={() => setSelectedMenuId(null)} onAddToCart={handleAddToCart} />
        </>
      )}
    </div>
  );
}

function parsePositiveInt(value: string, label: string): number {
  const num = Number(value);
  if (!Number.isInteger(num) || num <= 0) throw new Error(`${label}는 양의 정수여야 합니다.`);
  return num;
}

function toErrorMessage(caught: unknown): string {
  if (caught instanceof HttpError) return `${caught.code ?? "HTTP_ERROR"}: ${caught.message}`;
  if (caught instanceof Error) return caught.message;
  return "요청 실패";
}
