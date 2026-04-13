"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { SessionPanel } from "@/components/SessionPanel";
import { MenuGroupSection, MenuCardViewModel } from "@/components/store-detail/MenuGroupSection";
import { MenuOptionSheet } from "@/components/store-detail/MenuOptionSheet";
import { decodeJwtPayload, getTokenBundle } from "@/lib/auth-storage";
import { apiRequest, HttpError } from "@/lib/http";
import {
  CreateMenuGroupRequest,
  CreateMenuRequest,
  CreateOwnerStoreRequest,
  OwnerStoreSummaryResponse,
  StoreCategoryOption,
  StoreDetailQueryMenu,
  StoreDetailQueryResponse,
  UpdateMenuGroupRequest,
  UpdateMenuRequest,
  UpdateOwnerStoreRequest
} from "@/lib/types";

type AuthContext = { accessToken: string };

export default function OwnerStorePage() {
  const [categories, setCategories] = useState<StoreCategoryOption[]>([]);
  const [stores, setStores] = useState<OwnerStoreSummaryResponse[]>([]);
  const [selectedStoreId, setSelectedStoreId] = useState<number | null>(null);
  const [detail, setDetail] = useState<StoreDetailQueryResponse | null>(null);
  const [menuSheetId, setMenuSheetId] = useState<number | null>(null);
  const [editingMenuId, setEditingMenuId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [createName, setCreateName] = useState("");
  const [createCategoryIds, setCreateCategoryIds] = useState<number[]>([]);
  const [createMin, setCreateMin] = useState("0");
  const [createTip, setCreateTip] = useState("0");
  const [createLogo, setCreateLogo] = useState("");

  const [updateName, setUpdateName] = useState("");
  const [updateCategoryIds, setUpdateCategoryIds] = useState<number[]>([]);
  const [updateMin, setUpdateMin] = useState("0");
  const [updateTip, setUpdateTip] = useState("0");
  const [updateLogo, setUpdateLogo] = useState("");

  const [groupName, setGroupName] = useState("");
  const [groupOrder, setGroupOrder] = useState("0");
  const [menuName, setMenuName] = useState("");
  const [menuDescription, setMenuDescription] = useState("");
  const [menuPrice, setMenuPrice] = useState("0");
  const [menuImageUrl, setMenuImageUrl] = useState("");
  const [menuGroupId, setMenuGroupId] = useState("");

  const [editMenuName, setEditMenuName] = useState("");
  const [editMenuDescription, setEditMenuDescription] = useState("");
  const [editMenuPrice, setEditMenuPrice] = useState("0");
  const [editMenuImageUrl, setEditMenuImageUrl] = useState("");
  const [editMenuGroupId, setEditMenuGroupId] = useState("");
  const [editMenuAvailable, setEditMenuAvailable] = useState(true);
  const [editMenuSoldOut, setEditMenuSoldOut] = useState(false);

  const selectedStore = useMemo(() => stores.find((store) => store.storeId === selectedStoreId) ?? null, [stores, selectedStoreId]);
  const realGroups = useMemo(() => (detail?.menuGroups ?? []).filter((group) => group.id > 0), [detail]);
  const cards = useMemo(() => (detail?.menuGroups ?? []).map((group) => ({
    groupId: group.id,
    groupName: group.id > 0 ? group.name : "Ungrouped",
    items: group.menus.map<MenuCardViewModel>((menu) => ({ menuId: menu.id, name: menu.name, description: menu.description, price: menu.price, imageUrl: menu.imageUrl, badges: (menu.tags ?? []).map((tag) => tag.name).slice(0, 2) }))
  })), [detail]);
  const flatMenus = useMemo(() => (detail?.menuGroups ?? []).flatMap((group) => group.menus.map((menu) => ({ menuId: menu.id, name: menu.name, description: menu.description, price: menu.price, imageUrl: menu.imageUrl, optionGroups: menu.optionGroups ?? [] }))), [detail]);
  const selectedMenu = useMemo(() => flatMenus.find((menu) => menu.menuId === menuSheetId) ?? null, [flatMenus, menuSheetId]);

  useEffect(() => { void bootstrap(); }, []);
  useEffect(() => { if (selectedStoreId) void loadDetail(selectedStoreId); }, [selectedStoreId]);
  useEffect(() => {
    if (!detail) return;
    setUpdateName(detail.name);
    setUpdateCategoryIds(detail.categories.map((category) => category.id));
    setUpdateMin(String(detail.deliveryPolicy.minOrderAmount));
    setUpdateTip(String(detail.deliveryPolicy.deliveryTip));
    setUpdateLogo(detail.images.storeLogoUrl ?? "");
  }, [detail]);

  async function bootstrap() { await Promise.all([loadCategories(), loadStores()]); }
  async function loadCategories() { try { const auth = resolveAuthContext(); setCategories(await apiRequest<StoreCategoryOption[]>("/api/v1/stores/categories", { method: "GET", token: auth.accessToken })); } catch (caught) { setError(toErrorMessage(caught)); } }
  async function loadStores() { try { const auth = resolveAuthContext(); const next = await apiRequest<OwnerStoreSummaryResponse[]>("/api/v1/owner/stores", { method: "GET", token: auth.accessToken }); setStores(next); setSelectedStoreId((prev) => (next.length === 0 ? null : prev && next.some((store) => store.storeId === prev) ? prev : next[0].storeId)); } catch (caught) { setError(toErrorMessage(caught)); } }
  async function loadDetail(storeId: number) { try { const auth = resolveAuthContext(); setDetail(await apiRequest<StoreDetailQueryResponse>(`/api/v1/stores/${storeId}`, { method: "GET", token: auth.accessToken })); } catch (caught) { setError(toErrorMessage(caught)); } }
  async function refresh() { await loadStores(); if (selectedStoreId) await loadDetail(selectedStoreId); }
  async function withMutation(run: () => Promise<void>, okMessage: string) { setLoading(true); setError(""); setSuccess(""); try { await run(); setSuccess(okMessage); await refresh(); } catch (caught) { setError(toErrorMessage(caught)); } finally { setLoading(false); } }

  function pickMenu(menu: StoreDetailQueryMenu, groupId: number) { setEditingMenuId(menu.id); setEditMenuName(menu.name); setEditMenuDescription(menu.description ?? ""); setEditMenuPrice(String(menu.price)); setEditMenuImageUrl(menu.imageUrl ?? ""); setEditMenuGroupId(groupId > 0 ? String(groupId) : ""); setEditMenuAvailable(menu.isAvailable); setEditMenuSoldOut(menu.isSoldOut); }

  async function onCreateStore(event: FormEvent<HTMLFormElement>) { event.preventDefault(); await withMutation(async () => { const auth = resolveAuthContext(); const request: CreateOwnerStoreRequest = { name: createName.trim(), categoryIds: createCategoryIds, deliveryPolicy: { minOrderAmount: toInt(createMin, "minOrderAmount"), deliveryTip: toInt(createTip, "deliveryTip") }, storeLogoUrl: normalize(createLogo) }; const created = await apiRequest<{ storeId: number }>("/api/v1/owner/stores", { method: "POST", token: auth.accessToken, body: request }); setSelectedStoreId(created.storeId); setCreateName(""); setCreateCategoryIds([]); setCreateMin("0"); setCreateTip("0"); setCreateLogo(""); }, "Store created"); }
  async function onUpdateStore(event: FormEvent<HTMLFormElement>) { event.preventDefault(); if (!selectedStoreId) return; await withMutation(async () => { const auth = resolveAuthContext(); const request: UpdateOwnerStoreRequest = { name: updateName.trim(), categoryIds: updateCategoryIds, deliveryPolicy: { minOrderAmount: toInt(updateMin, "minOrderAmount"), deliveryTip: toInt(updateTip, "deliveryTip") }, storeLogoUrl: normalize(updateLogo) }; await apiRequest(`/api/v1/owner/stores/${selectedStoreId}`, { method: "PUT", token: auth.accessToken, body: request }); }, "Store updated"); }
  async function onDeleteStore() { if (!selectedStoreId || !window.confirm("Delete this store?")) return; await withMutation(async () => { const auth = resolveAuthContext(); await apiRequest(`/api/v1/owner/stores/${selectedStoreId}`, { method: "DELETE", token: auth.accessToken }); setDetail(null); }, "Store deleted"); }
  async function onCreateMenuGroup(event: FormEvent<HTMLFormElement>) { event.preventDefault(); if (!selectedStoreId) return; await withMutation(async () => { const auth = resolveAuthContext(); const request: CreateMenuGroupRequest = { name: groupName.trim(), displayOrder: toInt(groupOrder, "displayOrder") }; await apiRequest(`/api/v1/owner/stores/${selectedStoreId}/menu-groups`, { method: "POST", token: auth.accessToken, body: request }); setGroupName(""); setGroupOrder("0"); }, "Menu group created"); }
  async function onDeleteMenuGroup(menuGroupId: number) { if (!selectedStoreId || !window.confirm("Delete this menu group and menus?")) return; await withMutation(async () => { const auth = resolveAuthContext(); await apiRequest(`/api/v1/owner/stores/${selectedStoreId}/menu-groups/${menuGroupId}`, { method: "DELETE", token: auth.accessToken }); }, "Menu group deleted"); }
  async function onUpdateMenuGroup(menuGroupId: number, currentName: string) {
    if (!selectedStoreId) return;
    const nextName = window.prompt("Menu group name", currentName);
    if (!nextName || nextName.trim() === "") return;
    const orderRaw = window.prompt("Display order", "0");
    if (orderRaw === null) return;
    await withMutation(async () => {
      const auth = resolveAuthContext();
      const request: UpdateMenuGroupRequest = { name: nextName.trim(), displayOrder: toInt(orderRaw, "displayOrder") };
      await apiRequest(`/api/v1/owner/stores/${selectedStoreId}/menu-groups/${menuGroupId}`, { method: "PUT", token: auth.accessToken, body: request });
    }, "Menu group updated");
  }
  async function onCreateMenu(event: FormEvent<HTMLFormElement>) { event.preventDefault(); if (!selectedStoreId) return; await withMutation(async () => { const auth = resolveAuthContext(); const request: CreateMenuRequest = { menuGroupId: menuGroupId.trim() === "" ? null : Number(menuGroupId), name: menuName.trim(), description: normalize(menuDescription), price: toInt(menuPrice, "price"), isAvailable: true, imageUrl: normalize(menuImageUrl), displayOrder: 0 }; await apiRequest(`/api/v1/owner/stores/${selectedStoreId}/menus`, { method: "POST", token: auth.accessToken, body: request }); setMenuName(""); setMenuDescription(""); setMenuPrice("0"); setMenuImageUrl(""); }, "Menu created"); }
  async function onUpdateMenu(event: FormEvent<HTMLFormElement>) { event.preventDefault(); if (!selectedStoreId || !editingMenuId) return; await withMutation(async () => { const auth = resolveAuthContext(); const request: UpdateMenuRequest = { menuGroupId: editMenuGroupId.trim() === "" ? null : Number(editMenuGroupId), name: editMenuName.trim(), description: normalize(editMenuDescription), price: toInt(editMenuPrice, "price"), imageUrl: normalize(editMenuImageUrl), isAvailable: editMenuAvailable, isSoldOut: editMenuSoldOut }; await apiRequest(`/api/v1/owner/stores/${selectedStoreId}/menus/${editingMenuId}`, { method: "PUT", token: auth.accessToken, body: request }); setEditingMenuId(null); }, "Menu updated"); }
  async function onDeleteMenu(menuId: number) { if (!selectedStoreId || !window.confirm("Delete this menu?")) return; await withMutation(async () => { const auth = resolveAuthContext(); await apiRequest(`/api/v1/owner/stores/${selectedStoreId}/menus/${menuId}`, { method: "DELETE", token: auth.accessToken }); if (editingMenuId === menuId) setEditingMenuId(null); }, "Menu deleted"); }

  return (
    <div className="stack">
      <section className="card stack">
        <h1 className="title">Owner Store Console</h1>
        <p className="muted" style={{ margin: 0 }}>
          List my stores, click one to view detail, then edit/create/delete store and menus.
        </p>
      </section>

      {(error || success) && (
        <section className="card stack">
          {success && <p className="success" style={{ margin: 0 }}>{success}</p>}
          {error && <p className="error" style={{ margin: 0 }}>{error}</p>}
        </section>
      )}

      <div className="grid-2">
        <section className="card stack">
          <div className="row">
            <h2 style={{ margin: 0 }}>My stores</h2>
            <button className="button secondary" style={{ width: "auto", paddingInline: 14 }} onClick={loadStores} disabled={loading}>
              Reload
            </button>
          </div>
          {stores.length === 0 && <p className="muted" style={{ margin: 0 }}>No store yet.</p>}
          {stores.map((store) => (
            <button
              key={store.storeId}
              type="button"
              className="button secondary"
              style={{ textAlign: "left", borderColor: selectedStoreId === store.storeId ? "var(--accent)" : "var(--line)" }}
              onClick={() => setSelectedStoreId(store.storeId)}
            >
              <b>{store.name}</b>
              <div className="muted" style={{ fontSize: 12 }}>#{store.storeId} · {store.status}</div>
              <div className="muted" style={{ fontSize: 12 }}>min {toWon(store.minOrderAmount)} · tip {toWon(store.deliveryTip)}</div>
            </button>
          ))}
        </section>

        <section className="card stack">
          <h2 style={{ margin: 0 }}>Create store</h2>
          <form className="stack" onSubmit={onCreateStore}>
            <input className="input" value={createName} onChange={(event) => setCreateName(event.target.value)} placeholder="Store name" required />
            <div className="grid-2">
              <input className="input" type="number" min={0} value={createMin} onChange={(event) => setCreateMin(event.target.value)} placeholder="Min order amount" />
              <input className="input" type="number" min={0} value={createTip} onChange={(event) => setCreateTip(event.target.value)} placeholder="Delivery tip" />
            </div>
            <input className="input" value={createLogo} onChange={(event) => setCreateLogo(event.target.value)} placeholder="Logo URL" />
            <div className="grid-2" style={{ gap: 8 }}>
              {categories.map((category) => (
                <label key={category.id} className="row" style={{ border: "1px solid var(--line)", borderRadius: 10, padding: "8px 10px" }}>
                  <input type="checkbox" checked={createCategoryIds.includes(category.id)} onChange={() => setCreateCategoryIds((prev) => toggle(prev, category.id))} />
                  <span>{category.name}</span>
                </label>
              ))}
            </div>
            <button className="button" disabled={loading}>Create store</button>
          </form>
        </section>
      </div>

      {selectedStore && detail && (
        <>
          <section className="card stack">
            <div className="row" style={{ justifyContent: "space-between" }}>
              <div>
                <h2 style={{ margin: 0 }}>{detail.name}</h2>
                <p className="muted" style={{ margin: "4px 0 0 0" }}>
                  min {toWon(detail.deliveryPolicy.minOrderAmount)} · tip {toWon(detail.deliveryPolicy.deliveryTip)}
                </p>
              </div>
              <div className="row">
                <span className="button secondary" style={{ width: "auto", paddingInline: 12 }}>{detail.status}</span>
                <button className="button secondary" style={{ width: "auto", paddingInline: 12 }} onClick={() => selectedStoreId && loadDetail(selectedStoreId)}>Reload detail</button>
                <button className="button danger" style={{ width: "auto", paddingInline: 12 }} onClick={onDeleteStore}>Delete store</button>
              </div>
            </div>
            {detail.images.storeLogoUrl && (
              <img src={detail.images.storeLogoUrl} alt={detail.name} style={{ width: "100%", maxHeight: 260, objectFit: "cover", borderRadius: 12 }} />
            )}
            <div className="row">
              {detail.categories.map((category) => (
                <span key={category.id} className="button secondary" style={{ width: "auto", padding: "6px 10px", fontSize: 12 }}>{category.name}</span>
              ))}
            </div>
          </section>

          <section className="card stack">
            <h2 style={{ margin: 0 }}>Update store</h2>
            <form className="stack" onSubmit={onUpdateStore}>
              <input className="input" value={updateName} onChange={(event) => setUpdateName(event.target.value)} required />
              <div className="grid-2">
                <input className="input" type="number" min={0} value={updateMin} onChange={(event) => setUpdateMin(event.target.value)} placeholder="Min order amount" />
                <input className="input" type="number" min={0} value={updateTip} onChange={(event) => setUpdateTip(event.target.value)} placeholder="Delivery tip" />
              </div>
              <input className="input" value={updateLogo} onChange={(event) => setUpdateLogo(event.target.value)} placeholder="Logo URL" />
              <div className="grid-2" style={{ gap: 8 }}>
                {categories.map((category) => (
                  <label key={category.id} className="row" style={{ border: "1px solid var(--line)", borderRadius: 10, padding: "8px 10px" }}>
                    <input type="checkbox" checked={updateCategoryIds.includes(category.id)} onChange={() => setUpdateCategoryIds((prev) => toggle(prev, category.id))} />
                    <span>{category.name}</span>
                  </label>
                ))}
              </div>
              <button className="button" disabled={loading}>Save store</button>
            </form>
          </section>

          <div className="grid-2">
            <section className="card stack">
              <h2 style={{ margin: 0 }}>Create menu group</h2>
              <form className="stack" onSubmit={onCreateMenuGroup}>
                <input className="input" value={groupName} onChange={(event) => setGroupName(event.target.value)} placeholder="Group name" required />
                <input className="input" type="number" min={0} value={groupOrder} onChange={(event) => setGroupOrder(event.target.value)} placeholder="Display order" />
                <button className="button" disabled={loading}>Create group</button>
              </form>
            </section>
            <section className="card stack">
              <h2 style={{ margin: 0 }}>Create menu</h2>
              <form className="stack" onSubmit={onCreateMenu}>
                <select className="input" value={menuGroupId} onChange={(event) => setMenuGroupId(event.target.value)}>
                  <option value="">No group</option>
                  {realGroups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}
                </select>
                <input className="input" value={menuName} onChange={(event) => setMenuName(event.target.value)} placeholder="Menu name" required />
                <input className="input" value={menuDescription} onChange={(event) => setMenuDescription(event.target.value)} placeholder="Description" />
                <input className="input" type="number" min={0} value={menuPrice} onChange={(event) => setMenuPrice(event.target.value)} placeholder="Price" />
                <input className="input" value={menuImageUrl} onChange={(event) => setMenuImageUrl(event.target.value)} placeholder="Image URL" />
                <button className="button" disabled={loading}>Create menu</button>
              </form>
            </section>
          </div>

          <section className="stack">
            {cards.map((group) => (
              <MenuGroupSection key={group.groupId} title={group.groupName} items={group.items} onSelectMenu={(menuId) => setMenuSheetId(menuId)} />
            ))}
          </section>

          <section className="card stack">
            <h2 style={{ margin: 0 }}>Menu manager</h2>
            {detail.menuGroups.map((group) => (
              <div key={group.id} className="stack" style={{ border: "1px solid var(--line)", borderRadius: 12, padding: 12 }}>
                <div className="row" style={{ justifyContent: "space-between" }}>
                  <b>{group.id > 0 ? group.name : "Ungrouped"}</b>
                  {group.id > 0 && (
                    <div className="row">
                      <button className="button secondary" style={{ width: "auto", paddingInline: 12 }} onClick={() => onUpdateMenuGroup(group.id, group.name)}>Edit group</button>
                      <button className="button danger" style={{ width: "auto", paddingInline: 12 }} onClick={() => onDeleteMenuGroup(group.id)}>Delete group</button>
                    </div>
                  )}
                </div>
                {group.menus.map((menu) => (
                  <div key={menu.id} className="row" style={{ justifyContent: "space-between", border: "1px solid var(--line)", borderRadius: 10, padding: 10 }}>
                    <div>
                      <b>{menu.name}</b>
                      <div className="muted" style={{ fontSize: 12 }}>{toWon(menu.price)} · {menu.isAvailable ? "available" : "inactive"} · {menu.isSoldOut ? "sold out" : "ready"}</div>
                    </div>
                    <div className="row">
                      <button className="button secondary" style={{ width: "auto", paddingInline: 12 }} onClick={() => pickMenu(menu, group.id)}>Edit</button>
                      <button className="button danger" style={{ width: "auto", paddingInline: 12 }} onClick={() => onDeleteMenu(menu.id)}>Delete</button>
                    </div>
                  </div>
                ))}
              </div>
            ))}
          </section>

          {editingMenuId !== null && (
            <section className="card stack">
              <h2 style={{ margin: 0 }}>Update menu</h2>
              <form className="stack" onSubmit={onUpdateMenu}>
                <select className="input" value={editMenuGroupId} onChange={(event) => setEditMenuGroupId(event.target.value)}>
                  <option value="">No group</option>
                  {realGroups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}
                </select>
                <input className="input" value={editMenuName} onChange={(event) => setEditMenuName(event.target.value)} required />
                <input className="input" value={editMenuDescription} onChange={(event) => setEditMenuDescription(event.target.value)} />
                <input className="input" type="number" min={0} value={editMenuPrice} onChange={(event) => setEditMenuPrice(event.target.value)} />
                <input className="input" value={editMenuImageUrl} onChange={(event) => setEditMenuImageUrl(event.target.value)} />
                <label className="row"><input type="checkbox" checked={editMenuAvailable} onChange={(event) => setEditMenuAvailable(event.target.checked)} /><span>available</span></label>
                <label className="row"><input type="checkbox" checked={editMenuSoldOut} onChange={(event) => setEditMenuSoldOut(event.target.checked)} /><span>sold out</span></label>
                <div className="row">
                  <button className="button" disabled={loading}>Save menu</button>
                  <button className="button secondary" type="button" onClick={() => setEditingMenuId(null)}>Cancel</button>
                </div>
              </form>
            </section>
          )}
        </>
      )}

      <MenuOptionSheet open={selectedMenu !== null} menu={selectedMenu} onClose={() => setMenuSheetId(null)} />
      <SessionPanel />
    </div>
  );
}

function resolveAuthContext(): AuthContext {
  const bundle = getTokenBundle();
  if (!bundle?.accessToken) throw new Error("Login required.");
  const payload = decodeJwtPayload(bundle.accessToken);
  const userId = payload?.user_id ?? (payload?.sub ? Number(payload.sub) : NaN);
  if (!Number.isFinite(userId) || userId <= 0) throw new Error("Invalid access token payload.");
  return { accessToken: bundle.accessToken };
}

function toInt(value: string, label: string): number { const parsed = Number(value); if (!Number.isInteger(parsed) || parsed < 0) throw new Error(`${label} must be a non-negative integer.`); return parsed; }
function normalize(value: string): string | null { const trimmed = value.trim(); return trimmed === "" ? null : trimmed; }
function toggle(list: number[], value: number): number[] { return list.includes(value) ? list.filter((id) => id !== value) : [...list, value]; }
function toWon(amount: number): string { return `${amount.toLocaleString("ko-KR")}원`; }
function toErrorMessage(caught: unknown): string { if (caught instanceof HttpError) return `${caught.code ?? "HTTP_ERROR"}: ${caught.message}`; if (caught instanceof Error) return caught.message; return "Request failed."; }
