"use client";

import { useEffect, useMemo, useState } from "react";
import { StoreDetailQueryOptionGroup } from "@/lib/types";

type OptionMenuItem = {
  menuId: number;
  name: string;
  description: string | null;
  price: number;
  imageUrl: string | null;
  optionGroups: StoreDetailQueryOptionGroup[];
};

type MenuOptionSheetProps = {
  open: boolean;
  menu: OptionMenuItem | null;
  onClose: () => void;
};

type SelectedMap = Record<string, string[]>;

export function MenuOptionSheet({ open, menu, onClose }: MenuOptionSheetProps) {
  const [selected, setSelected] = useState<SelectedMap>({});
  const [quantity, setQuantity] = useState(1);

  useEffect(() => {
    if (!menu) {
      setSelected({});
      setQuantity(1);
      return;
    }
    const initial: SelectedMap = {};
    menu.optionGroups.forEach((group, index) => {
      if (group.isRequired && group.options.length > 0) {
        initial[groupKey(group, index)] = [group.options[0].name];
      }
    });
    setSelected(initial);
    setQuantity(1);
  }, [menu]);

  const extraPrice = useMemo(() => {
    if (!menu) return 0;
    return menu.optionGroups.reduce((sum, group, index) => {
      const picked = selected[groupKey(group, index)] ?? [];
      return sum + group.options.filter((option) => picked.includes(option.name)).reduce((acc, option) => acc + option.extraPrice, 0);
    }, 0);
  }, [menu, selected]);

  const total = useMemo(() => (menu ? (menu.price + extraPrice) * quantity : 0), [menu, extraPrice, quantity]);

  if (!open || !menu) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 p-0 md:items-center md:p-4">
      <div className="h-[88vh] w-full max-w-[680px] overflow-hidden rounded-t-3xl bg-white shadow-2xl md:h-auto md:max-h-[92vh] md:rounded-3xl">
        <div className="flex items-center justify-between border-b border-eats-line px-5 py-4">
          <div>
            <h3 className="text-[22px] font-bold text-eats-text">{menu.name}</h3>
            <p className="mt-1 text-[14px] text-eats-muted">{menu.description || "옵션을 선택해줘."}</p>
          </div>
          <button type="button" onClick={onClose} className="rounded-full border border-[#dde3ef] px-3 py-1 text-sm font-medium text-[#59637a]">
            닫기
          </button>
        </div>

        <div className="max-h-[calc(88vh-170px)] overflow-y-auto px-5 py-4 md:max-h-[60vh]">
          <div className="mb-4 rounded-2xl bg-[#f8faff] p-4">
            <p className="text-sm text-eats-muted">기본 가격</p>
            <p className="mt-1 text-[24px] font-bold text-eats-text">{toWon(menu.price)}</p>
          </div>

          {menu.optionGroups.map((group, index) => {
            const key = groupKey(group, index);
            const current = selected[key] ?? [];
            return (
              <section key={key} className="mb-4 rounded-2xl border border-[#e8ecf4] bg-white p-4">
                <div className="mb-2 flex items-center gap-2">
                  <p className="text-[15px] font-bold text-eats-text">{group.name}</p>
                  <span className="rounded-full bg-eats-brandSoft px-2 py-[2px] text-[11px] font-semibold text-eats-brand">
                    {group.isRequired ? "필수" : "선택"}
                  </span>
                </div>

                <div className="space-y-2">
                  {group.options.map((option) => (
                    <label key={`${key}-${option.name}`} className="flex cursor-pointer items-center justify-between rounded-xl border border-[#edf1f7] px-3 py-2">
                      <div className="flex items-center gap-2">
                        <input
                          type={group.isMultiple ? "checkbox" : "radio"}
                          name={key}
                          checked={current.includes(option.name)}
                          disabled={!option.isAvailable}
                          onChange={() => {
                            setSelected((prev) => {
                              const exists = prev[key] ?? [];
                              if (!group.isMultiple) return { ...prev, [key]: [option.name] };
                              if (exists.includes(option.name)) return { ...prev, [key]: exists.filter((name) => name !== option.name) };
                              if (exists.length >= group.maxSelectCount) return prev;
                              return { ...prev, [key]: [...exists, option.name] };
                            });
                          }}
                        />
                        <span className={`text-sm ${option.isAvailable ? "text-eats-text" : "text-[#a5afc0]"}`}>{option.name}</span>
                      </div>
                      <span className="text-sm font-medium text-eats-text">{option.extraPrice > 0 ? `+${toWon(option.extraPrice)}` : "무료"}</span>
                    </label>
                  ))}
                </div>
              </section>
            );
          })}
        </div>

        <div className="border-t border-eats-line bg-white px-5 py-4">
          <div className="mb-3 flex items-center justify-between">
            <p className="text-sm text-eats-muted">수량</p>
            <div className="inline-flex items-center overflow-hidden rounded-full border border-[#dbe1ed]">
              <button type="button" className="px-3 py-1 text-lg text-[#60708d]" onClick={() => setQuantity((prev) => Math.max(1, prev - 1))}>-</button>
              <span className="min-w-10 text-center text-sm font-semibold text-eats-text">{quantity}</span>
              <button type="button" className="px-3 py-1 text-lg text-[#60708d]" onClick={() => setQuantity((prev) => Math.min(20, prev + 1))}>+</button>
            </div>
          </div>
          <button type="button" className="w-full rounded-2xl bg-eats-brand px-4 py-3 text-[16px] font-bold text-white">
            담기 {toWon(total)}
          </button>
        </div>
      </div>
    </div>
  );
}

function groupKey(group: StoreDetailQueryOptionGroup, index: number): string {
  return `${group.name}-${index}`;
}

function toWon(price: number): string {
  return `${price.toLocaleString("ko-KR")}원`;
}
