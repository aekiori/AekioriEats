"use client";

import Link from "next/link";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { apiRequest, HttpError } from "@/lib/http";
import { StoreDetailQueryResponse, StoreSearchItem, StoreSearchPageResponse } from "@/lib/types";
import { foodImage, formatWon, isStoreOpenNow } from "@/components/store-detail/store-ui-utils";

type StoreCard = {
  storeId: number;
  name: string;
  status: string;
  minOrderAmount: number;
  deliveryTip: number;
  storeLogoUrl: string | null;
  matchedBy: string[];
  rating: number;
  reviewCount: number;
  distanceKm: number;
  etaMinutes: number;
  promotion: string;
  isOpenNow: boolean;
};

const categories = ["전체", "치킨", "분식", "돈까스", "족발", "찜탕", "피자", "카페"];

const brandChips = [
  { name: "마왕치킨", color: "#f4efe3" },
  { name: "빨간닭강정", color: "#ffe8df" },
  { name: "교촌", color: "#ffd75a" },
  { name: "버거킹", color: "#fff0e4" },
  { name: "국밥", color: "#edf7ee" },
  { name: "카페", color: "#f3f0eb" }
];

export default function HomePage() {
  const [activeCategory, setActiveCategory] = useState("전체");
  const [query, setQuery] = useState("");
  const [stores, setStores] = useState<StoreCard[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    void loadStores("");
  }, []);

  const visibleStores = useMemo(() => {
    if (activeCategory === "전체") return stores;
    return stores.filter((store) => store.name.includes(activeCategory) || store.matchedBy.some((value) => value.includes(activeCategory)));
  }, [activeCategory, stores]);

  async function loadStores(searchQuery: string) {
    setLoading(true);
    setError("");

    try {
      const response = await apiRequest<StoreSearchPageResponse>(
        `/api/v1/stores/search?q=${encodeURIComponent(searchQuery.trim())}&page=0&size=20`,
        { method: "GET" }
      );

      const cards = await Promise.all(response.content.map(toStoreCard));
      setStores(cards);
      if (response.content.length === 0) {
        setError("검색 결과가 없어요. Store DB에 더미 데이터가 들어갔는지 확인해줘.");
      }
    } catch (caught) {
      setStores([]);
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function handleSearch(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault();
    setActiveCategory("전체");
    await loadStores(query);
  }

  return (
    <div className="mx-auto min-h-screen max-w-[430px] overflow-hidden bg-white shadow-[0_24px_80px_rgba(15,23,42,0.18)] md:rounded-[36px]">
      <header className="sticky top-0 z-30 border-b border-[#edf0f4] bg-white/95 px-5 pb-3 pt-5 backdrop-blur-xl">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#52aef5]">AekioriEats</p>
            <h1 className="mt-1 text-[24px] font-black tracking-[-0.06em] text-[#20242c]">오늘 뭐 먹지?</h1>
          </div>
          <Link href="/me" className="grid h-11 w-11 place-items-center rounded-full bg-[#f3f5f8] text-[14px] font-black text-[#20242c]">
            MY
          </Link>
        </div>

        <form className="mt-4 flex gap-2" onSubmit={handleSearch}>
          <label className="relative flex-1">
            <span className="absolute left-4 top-1/2 -translate-y-1/2 text-[15px] font-black text-[#8b95a1]">검색</span>
            <input
              className="h-14 w-full rounded-2xl border-0 bg-[#f3f5f8] py-4 pl-16 pr-4 text-[15px] font-bold text-[#20242c] outline-none placeholder:text-[#9aa3ad]"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="가게 이름을 검색해보세요"
            />
          </label>
          <button className="rounded-2xl bg-[#20242c] px-4 text-[13px] font-black text-white" disabled={loading}>
            {loading ? "로딩" : "검색"}
          </button>
        </form>
      </header>

      <main className="pb-24">
        <nav className="flex gap-7 overflow-x-auto border-b border-[#edf0f4] bg-white px-5 py-4">
          {categories.map((category) => (
            <button
              key={category}
              type="button"
              onClick={() => {
                setActiveCategory(category);
                if (category === "전체") void loadStores("");
              }}
              className={`shrink-0 border-b-4 pb-2 text-[16px] font-black tracking-[-0.04em] ${activeCategory === category ? "border-[#52aef5] text-[#2599e6]" : "border-transparent text-[#20242c]"}`}
            >
              {category}
            </button>
          ))}
        </nav>

        <section className="border-b-[10px] border-[#f4f5f7] bg-white px-5 py-5">
          <div className="flex gap-4 overflow-x-auto pb-1">
            {brandChips.map((brand) => (
              <button
                key={brand.name}
                type="button"
                className="shrink-0 text-center"
                onClick={() => {
                  setQuery(brand.name);
                  setActiveCategory("전체");
                  void loadStores(brand.name);
                }}
              >
                <div className="grid h-20 w-20 place-items-center rounded-full border border-[#e7ebf0] px-2 text-[16px] font-black text-[#9a6d18]" style={{ backgroundColor: brand.color }}>
                  {brand.name}
                </div>
                <p className="mt-2 text-[13px] font-bold text-[#4e5968]">{brand.name}</p>
              </button>
            ))}
          </div>
        </section>

        <section className="bg-white px-5 py-4">
          <div className="mb-4 flex items-center justify-between">
            <label className="flex items-center gap-2 text-[15px] font-black text-[#123d79]">
              <span className="grid h-7 w-7 place-items-center rounded-md border border-[#cfd8e3] text-[#9aa3ad]">✓</span>
              실제 Store DB
            </label>
            <div className="flex gap-2">
              <button className="rounded-full border border-[#e0e5ec] px-3 py-2 text-[13px] font-bold text-[#4e5968]">추천순</button>
              <button className="rounded-full border border-[#e0e5ec] px-3 py-2 text-[13px] font-bold text-[#4e5968]">필터</button>
            </div>
          </div>

          {error && <p className="mb-4 rounded-2xl bg-[#fff7e6] px-4 py-3 text-[13px] font-bold text-[#9a6900]">{error}</p>}
          {loading && <p className="rounded-2xl bg-[#f3f5f8] px-4 py-8 text-center text-[14px] font-black text-[#7b8492]">가게 목록 불러오는 중...</p>}

          {!loading && visibleStores.length > 0 && (
            <div className="space-y-8">
              {visibleStores.map((store, index) => (
                <StoreListCard key={`${store.storeId}-${index}`} store={store} index={index} />
              ))}
            </div>
          )}

          {!loading && visibleStores.length === 0 && !error && (
            <p className="rounded-2xl bg-[#f3f5f8] px-4 py-8 text-center text-[14px] font-black text-[#7b8492]">표시할 가게가 없어요.</p>
          )}
        </section>
      </main>

      <BottomTabs />
    </div>
  );
}

function StoreListCard({ store, index }: { store: StoreCard; index: number }) {
  const hero = foodImage(store.storeId + index, store.storeLogoUrl);
  const thumbA = foodImage(store.storeId + index + 11);
  const thumbB = foodImage(store.storeId + index + 19);

  return (
    <Link href={`/store/detail?storeId=${store.storeId}`} className="block">
      <article className="group">
        <div className="grid h-[220px] grid-cols-[1fr_102px] gap-2 overflow-hidden rounded-[22px] bg-[#edf0f4]">
          <div className="relative overflow-hidden">
            <img src={hero} alt={store.name} className={`h-full w-full object-cover transition duration-500 group-hover:scale-105 ${store.isOpenNow ? "" : "grayscale"}`} />
            {!store.isOpenNow && (
              <div className="absolute inset-0 grid place-items-center bg-black/55 text-center">
                <div className="rounded-2xl border border-white/35 bg-white/12 px-5 py-3 text-white backdrop-blur-sm">
                  <p className="text-[22px] font-black tracking-[-0.05em]">마감했습니다</p>
                  <p className="mt-1 text-[12px] font-bold text-white/80">영업시간에 다시 주문해주세요</p>
                </div>
              </div>
            )}
            <div className="absolute bottom-0 left-0 right-0 bg-[#123d79] px-4 py-2 text-center text-[14px] font-black text-white">
              {store.isOpenNow ? store.promotion : "지금은 주문할 수 없어요"}
            </div>
          </div>
          <div className="grid gap-2">
            <img src={thumbA} alt="menu" className="h-full w-full object-cover" />
            <img src={thumbB} alt="menu" className="h-full w-full object-cover" />
          </div>
        </div>

        <div className="mt-4 flex items-start justify-between gap-3">
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <h2 className="line-clamp-1 text-[21px] font-black tracking-[-0.05em] text-[#20242c]">{store.name}</h2>
              {!store.isOpenNow && <span className="shrink-0 rounded-md bg-[#eef1f5] px-2 py-1 text-[11px] font-black text-[#7b8492]">마감</span>}
            </div>
            <div className="mt-2 flex flex-wrap items-center gap-1.5 text-[14px] font-bold text-[#6f7885]">
              <span className="text-[#ffc928]">★</span>
              <span className="text-[#20242c]">{store.rating.toFixed(1)}</span>
              <span>({store.reviewCount})</span>
              <span>·</span>
              <span>{store.distanceKm.toFixed(1)}km</span>
              <span>·</span>
              <span className="text-[#20242c]">배달비 {store.deliveryTip === 0 ? "0원" : formatWon(store.deliveryTip)}</span>
              <span>·</span>
              <span>최소주문 {formatWon(store.minOrderAmount)}</span>
            </div>
            <div className="mt-3 flex gap-2">
              {store.matchedBy.slice(0, 2).map((category) => (
                <span key={category} className="rounded-md bg-[#eaf7ff] px-2 py-1 text-[12px] font-black text-[#3197d6]">
                  {category}
                </span>
              ))}
            </div>
          </div>
          <span className="shrink-0 pt-1 text-[14px] font-black text-[#6f7885]">{store.etaMinutes}분</span>
        </div>
      </article>
    </Link>
  );
}

function BottomTabs() {
  return (
    <nav className="fixed bottom-0 left-1/2 z-40 grid h-20 w-full max-w-[430px] -translate-x-1/2 grid-cols-4 border-t border-[#e7ebf0] bg-white/95 px-3 pb-3 pt-2 text-center shadow-[0_-10px_28px_rgba(15,23,42,0.08)] backdrop-blur-xl md:rounded-t-[28px]">
      <Link href="/" className="text-[12px] font-black text-[#52aef5]">
        <span className="block text-[22px]">홈</span>홈
      </Link>
      <Link href="/store/detail" className="text-[12px] font-bold text-[#7b8492]">
        <span className="block text-[22px]">가게</span>가게
      </Link>
      <Link href="/cart" className="text-[12px] font-bold text-[#7b8492]">
        <span className="block text-[22px]">카트</span>카트
      </Link>
      <Link href="/payment/checkout" className="text-[12px] font-bold text-[#7b8492]">
        <span className="block text-[22px]">결제</span>결제
      </Link>
    </nav>
  );
}

async function toStoreCard(item: StoreSearchItem): Promise<StoreCard> {
  const matchedBy = item.matchedBy.length > 0 && item.matchedBy[0] !== "STORE_NAME" ? item.matchedBy : ["Store DB"];
  const isOpenNowValue = await fetchStoreOpenStatus(item);
  return {
    storeId: item.storeId,
    name: item.name,
    status: item.status,
    minOrderAmount: item.deliveryPolicy.minOrderAmount,
    deliveryTip: item.deliveryPolicy.deliveryTip,
    storeLogoUrl: item.storeLogoUrl,
    matchedBy,
    rating: 4.6 + ((item.storeId % 4) / 10),
    reviewCount: 120 + (item.storeId % 900),
    distanceKm: 0.8 + ((item.storeId % 17) / 10),
    etaMinutes: 24 + (item.storeId % 18),
    promotion: item.deliveryPolicy.deliveryTip === 0 ? "WOW! 와우회원은 매 주문 배달비 0원" : "리뷰 이벤트 진행 중",
    isOpenNow: isOpenNowValue
  };
}

async function fetchStoreOpenStatus(item: StoreSearchItem): Promise<boolean> {
  if (item.status !== "OPEN") return false;

  try {
    const detail = await apiRequest<StoreDetailQueryResponse>(`/api/v1/stores/${item.storeId}`, { method: "GET" });
    return isStoreOpenNow(detail.operatingHours);
  } catch {
    return item.status === "OPEN";
  }
}

function toErrorMessage(caught: unknown): string {
  if (caught instanceof HttpError) return `${caught.code ?? "HTTP_ERROR"}: ${caught.message}`;
  if (caught instanceof Error) return caught.message;
  return "요청 실패";
}
