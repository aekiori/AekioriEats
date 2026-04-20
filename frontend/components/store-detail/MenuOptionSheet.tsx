"use client";

import { useEffect, useMemo, useState } from "react";
import { CartOption } from "@/lib/cart-storage";
import { StoreDetailQueryOptionGroup } from "@/lib/types";
import { foodImage, formatWon } from "./store-ui-utils";

type OptionMenuItem = {
  menuId: number;
  name: string;
  description: string | null;
  price: number;
  imageUrl: string | null;
  optionGroups: StoreDetailQueryOptionGroup[];
};

export type AddMenuToCartPayload = {
  menuId: number;
  menuName: string;
  basePrice: number;
  unitPrice: number;
  quantity: number;
  imageUrl: string | null;
  options: CartOption[];
};

type MenuOptionSheetProps = {
  open: boolean;
  menu: OptionMenuItem | null;
  onClose: () => void;
  onAddToCart?: (payload: AddMenuToCartPayload) => void;
};

type SelectedMap = Record<string, string[]>;

export function MenuOptionSheet({ open, menu, onClose, onAddToCart }: MenuOptionSheetProps) {
  const [selected, setSelected] = useState<SelectedMap>({});
  const [quantity, setQuantity] = useState(1);

  useEffect(() => {
    if (!menu) {
      setSelected({});
      setQuantity(1);
      return;
    }

    const initial: SelectedMap = {};
    buildOptionGroups(menu.optionGroups).forEach((group, index) => {
      if (group.isRequired && group.options.length > 0) {
        initial[groupKey(group, index)] = [group.options[0].name];
      }
    });
    setSelected(initial);
    setQuantity(1);
  }, [menu]);

  const optionGroups = useMemo(() => (menu ? buildOptionGroups(menu.optionGroups) : []), [menu]);

  const selectedOptions = useMemo<CartOption[]>(() => {
    if (!menu) return [];

    return optionGroups.flatMap((group, index) => {
      const picked = selected[groupKey(group, index)] ?? [];
      return group.options
        .filter((option) => picked.includes(option.name))
        .map((option) => ({
          groupName: group.name,
          optionName: option.name,
          extraPrice: option.extraPrice
        }));
    });
  }, [menu, optionGroups, selected]);

  const extraPrice = useMemo(() => selectedOptions.reduce((sum, option) => sum + option.extraPrice, 0), [selectedOptions]);
  const unitPrice = menu ? menu.price + extraPrice : 0;
  const total = unitPrice * quantity;

  if (!open || !menu) return null;

  function handleAddToCart() {
    if (!menu || !onAddToCart) return;
    onAddToCart({
      menuId: menu.menuId,
      menuName: menu.name,
      basePrice: menu.price,
      unitPrice,
      quantity,
      imageUrl: menu.imageUrl,
      options: selectedOptions
    });
  }

  return (
    <div className="fixed inset-0 z-50 flex justify-center bg-[#10151f]/70 backdrop-blur-sm">
      <div className="relative h-dvh w-full max-w-[430px] overflow-hidden bg-white shadow-2xl">
        <div className="absolute left-0 right-0 top-0 z-20 flex items-center justify-between px-4 pt-5 text-white">
          <button type="button" onClick={onClose} className="grid h-10 w-10 place-items-center rounded-full bg-black/28 text-[28px] leading-none backdrop-blur-md">
            ‹
          </button>
          <div className="flex gap-2">
            <button type="button" className="grid h-10 w-10 place-items-center rounded-full bg-black/28 text-[18px] backdrop-blur-md">공유</button>
            <button type="button" className="grid h-10 w-10 place-items-center rounded-full bg-black/28 text-[18px] backdrop-blur-md">찜</button>
          </div>
        </div>

        <div className="h-full overflow-y-auto pb-[112px]">
          <div className="relative h-[310px] overflow-hidden bg-[#111827]">
            <img src={foodImage(menu.menuId, menu.imageUrl)} alt={menu.name} className="h-full w-full object-cover" />
            <div className="absolute inset-0 bg-gradient-to-b from-black/25 via-black/0 to-black/35" />
            <span className="absolute bottom-5 right-5 rounded-full bg-black/55 px-3 py-1 text-[12px] font-bold text-white">1 / 3</span>
          </div>

          <section className="rounded-t-[30px] bg-white px-5 pb-6 pt-7 shadow-[0_-16px_36px_rgba(17,24,39,0.12)]">
            <h1 className="text-[25px] font-black tracking-[-0.05em] text-[#20242c]">{menu.name}</h1>
            <p className="mt-4 text-[15px] leading-6 text-[#7e8794]">
              {menu.description || "가게에서 정성껏 준비한 메뉴입니다. 취향에 맞게 옵션을 골라 담아보세요."}
            </p>
            <div className="mt-4 flex flex-wrap gap-2">
              <span className="rounded-full bg-[#f3f5f8] px-3 py-1 text-[12px] font-bold text-[#6e7784]">매장 · 원산지 정보</span>
              <span className="rounded-full bg-[#fff5d8] px-3 py-1 text-[12px] font-black text-[#9a6900]">만족 97%</span>
              <span className="rounded-full bg-[#edf8ff] px-3 py-1 text-[12px] font-black text-[#2a8fc7]">리뷰 47</span>
            </div>
          </section>

          <section className="border-y-[10px] border-[#f4f5f7] bg-white px-5 py-5">
            <div className="flex items-center justify-between">
              <p className="text-[18px] font-black text-[#20242c]">가격</p>
              <p className="text-[20px] font-black tracking-[-0.04em] text-[#20242c]">{formatWon(menu.price)}</p>
            </div>
            <div className="mt-8 flex items-center justify-between">
              <p className="text-[18px] font-black text-[#20242c]">수량</p>
              <div className="flex items-center gap-4">
                <button type="button" className="grid h-12 w-12 place-items-center rounded-full border border-[#dfe4ea] text-[22px] text-[#9aa3ad]" onClick={() => setQuantity((prev) => Math.max(1, prev - 1))}>-</button>
                <span className="min-w-4 text-center text-[20px] font-black text-[#20242c]">{quantity}</span>
                <button type="button" className="grid h-12 w-12 place-items-center rounded-full border border-[#dfe4ea] text-[22px] text-[#5d6570]" onClick={() => setQuantity((prev) => Math.min(20, prev + 1))}>+</button>
              </div>
            </div>
          </section>

          {optionGroups.map((group, index) => {
            const key = groupKey(group, index);
            const current = selected[key] ?? [];
            return (
              <section key={key} className="border-b-[10px] border-[#f4f5f7] bg-white">
                <div className="flex items-center justify-between bg-[#f7f8fa] px-5 py-4">
                  <div>
                    <h2 className="text-[18px] font-black tracking-[-0.04em] text-[#20242c]">{group.name}</h2>
                    <p className="mt-1 text-[12px] font-bold text-[#9aa3ad]">
                      {group.isMultiple ? `최대 ${group.maxSelectCount}개 선택` : "하나만 선택"}
                    </p>
                  </div>
                  <span className="text-[13px] font-black text-[#d85b50]">{group.isRequired ? "필수 선택" : "선택"}</span>
                </div>
                <div className="divide-y divide-[#edf0f4] px-5">
                  {group.options.map((option) => (
                    <label key={`${key}-${option.name}`} className="flex cursor-pointer items-center justify-between py-5">
                      <div className="flex items-center gap-4">
                        <span className={`grid h-8 w-8 place-items-center rounded-full border-2 ${current.includes(option.name) ? "border-[#4aaaf0] bg-[#4aaaf0]" : "border-[#d3dae3] bg-white"}`}>
                          {current.includes(option.name) && <span className="h-3 w-3 rounded-full bg-white" />}
                        </span>
                        <input
                          className="sr-only"
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
                        <span className="text-[17px] font-bold text-[#333946]">{option.name}</span>
                      </div>
                      <span className="text-[15px] font-extrabold text-[#67707e]">{option.extraPrice > 0 ? `+${formatWon(option.extraPrice)}` : "무료"}</span>
                    </label>
                  ))}
                </div>
              </section>
            );
          })}
        </div>

        <div className="absolute bottom-0 left-0 right-0 bg-white/92 px-5 pb-5 pt-3 shadow-[0_-12px_30px_rgba(17,24,39,0.12)] backdrop-blur-xl">
          <button type="button" onClick={handleAddToCart} className="h-16 w-full rounded-none bg-[#52aef5] text-[18px] font-black text-white shadow-[0_10px_24px_rgba(82,174,245,0.34)]">
            배달 카트에 담기 · {formatWon(total)}
          </button>
        </div>
      </div>
    </div>
  );
}

const defaultOptionGroup: StoreDetailQueryOptionGroup = {
  name: "기본 선택",
  isRequired: true,
  isMultiple: false,
  minSelectCount: 1,
  maxSelectCount: 1,
  options: [
    { name: "기본", extraPrice: 0, isAvailable: true },
    { name: "소스 많이", extraPrice: 0, isAvailable: true },
    { name: "소스 적게", extraPrice: 0, isAvailable: true }
  ]
};

function buildOptionGroups(groups: StoreDetailQueryOptionGroup[]): StoreDetailQueryOptionGroup[] {
  return groups.length > 0 ? groups : [defaultOptionGroup];
}

function groupKey(group: StoreDetailQueryOptionGroup, index: number): string {
  return `${group.name}-${index}`;
}