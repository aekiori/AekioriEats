"use client";

import { FormEvent, useMemo, useState } from "react";
import { decodeJwtPayload, getTokenBundle } from "@/lib/auth-storage";
import { apiRequest, HttpError } from "@/lib/http";
import { StoreDetailQueryResponse } from "@/lib/types";
import { MenuGroupSection, MenuCardViewModel } from "@/components/store-detail/MenuGroupSection";
import { MenuOptionSheet } from "@/components/store-detail/MenuOptionSheet";

type AuthContext = { accessToken: string };

export default function StoreDetailPage() {
  const [storeIdInput, setStoreIdInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [store, setStore] = useState<StoreDetailQueryResponse | null>(null);
  const [selectedMenuId, setSelectedMenuId] = useState<number | null>(null);

  const groups = useMemo(() => {
    if (!store) return [];
    return store.menuGroups.map((group) => ({
      groupId: group.id,
      groupName: group.id > 0 ? group.name : "기타 메뉴",
      items: group.menus.map<MenuCardViewModel>((menu) => ({
        menuId: menu.id,
        name: menu.name,
        description: menu.description,
        price: menu.price,
        imageUrl: menu.imageUrl,
        badges: (menu.tags ?? []).map((tag) => tag.name).slice(0, 2)
      }))
    }));
  }, [store]);

  const flatMenus = useMemo(() => {
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

  async function handleLoadStore(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      const auth = resolveAuthContext();
      const storeId = parsePositiveInt(storeIdInput, "Store ID");
      const response = await apiRequest<StoreDetailQueryResponse>(`/api/v1/stores/${storeId}`, {
        method: "GET",
        token: auth.accessToken
      });
      setStore(response);
      setSelectedMenuId(null);
    } catch (caught) {
      setStore(null);
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-4">
      <section className="card stack">
        <h1 className="title">메뉴 상세</h1>
        <form className="row" onSubmit={handleLoadStore}>
          <input
            className="input"
            value={storeIdInput}
            onChange={(event) => setStoreIdInput(event.target.value)}
            placeholder="스토어 ID"
          />
          <button className="button" style={{ width: "auto", paddingInline: 16 }} disabled={loading}>
            {loading ? "불러오는 중..." : "메뉴 불러오기"}
          </button>
        </form>
        {error && <p className="error" style={{ margin: 0 }}>{error}</p>}
      </section>

      {store && (
        <>
          <section className="card stack">
            <h2 style={{ margin: 0 }}>{store.name}</h2>
            <p className="muted" style={{ margin: 0 }}>
              최소주문 {toWon(store.deliveryPolicy.minOrderAmount)} · 배달팁 {toWon(store.deliveryPolicy.deliveryTip)}
            </p>
            <div className="row">
              {store.categories.map((category) => (
                <span key={category.id} className="button secondary" style={{ width: "auto", padding: "4px 10px", fontSize: 12 }}>
                  {category.name}
                </span>
              ))}
            </div>
          </section>
          {groups.map((group) => (
            <MenuGroupSection
              key={group.groupId}
              title={group.groupName}
              items={group.items}
              onSelectMenu={(menuId) => setSelectedMenuId(menuId)}
            />
          ))}
        </>
      )}

      <MenuOptionSheet open={selectedMenu !== null} menu={selectedMenu} onClose={() => setSelectedMenuId(null)} />
    </div>
  );
}

function resolveAuthContext(): AuthContext {
  const bundle = getTokenBundle();
  if (!bundle?.accessToken) throw new Error("로그인이 필요해.");
  const payload = decodeJwtPayload(bundle.accessToken);
  const userId = payload?.user_id ?? (payload?.sub ? Number(payload.sub) : NaN);
  if (!Number.isFinite(userId) || userId <= 0) throw new Error("토큰 user_id가 유효하지 않다.");
  return { accessToken: bundle.accessToken };
}

function parsePositiveInt(value: string, label: string): number {
  const num = Number(value);
  if (!Number.isInteger(num) || num <= 0) throw new Error(`${label}는 양의 정수여야 한다.`);
  return num;
}

function toWon(amount: number): string {
  return `${amount.toLocaleString("ko-KR")}원`;
}

function toErrorMessage(caught: unknown): string {
  if (caught instanceof HttpError) return `${caught.code ?? "HTTP_ERROR"}: ${caught.message}`;
  if (caught instanceof Error) return caught.message;
  return "요청 실패";
}
