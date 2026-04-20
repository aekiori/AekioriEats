"use client";

import { foodImage, formatWon } from "./store-ui-utils";

type MenuItemCardProps = {
  menuId: number;
  name: string;
  description: string | null;
  price: number;
  imageUrl: string | null;
  badges: string[];
  orderCount?: number;
  reviewCount?: number;
  likeRate?: number;
  onSelect: () => void;
};

export function MenuItemCard({
  menuId,
  name,
  description,
  price,
  imageUrl,
  badges,
  orderCount = 80 + (menuId % 70),
  reviewCount = 18 + (menuId % 43),
  likeRate = 96 + (menuId % 4),
  onSelect
}: MenuItemCardProps) {
  return (
    <button type="button" className="group w-full bg-white px-5 py-5 text-left transition hover:bg-[#fbfcff]" onClick={onSelect}>
      <div className="flex gap-4">
        <div className="min-w-0 flex-1 pt-1">
          <div className="mb-2 flex flex-wrap gap-1.5">
            {badges.slice(0, 2).map((badge) => (
              <span key={`${menuId}-${badge}`} className="rounded-md bg-[#eaf7ff] px-2 py-1 text-[11px] font-extrabold text-[#3197d6]">
                {badge}
              </span>
            ))}
          </div>
          <h3 className="line-clamp-1 text-[19px] font-extrabold tracking-[-0.04em] text-[#20242c]">{name}</h3>
          <p className="mt-2 line-clamp-2 text-[14px] leading-5 text-[#8a929e]">
            {description?.trim() || "가게에서 정성껏 준비한 메뉴입니다."}
          </p>
          <p className="mt-3 text-[22px] font-black tracking-[-0.04em] text-[#20242c]">{formatWon(price)}</p>
          <div className="mt-3 flex items-center gap-2 text-[12px] font-bold text-[#68717f]">
            <span>만족 {likeRate}%</span>
            <span className="h-1 w-1 rounded-full bg-[#c7cdd6]" />
            <span>리뷰 {reviewCount}</span>
            <span className="h-1 w-1 rounded-full bg-[#c7cdd6]" />
            <span>주문 {orderCount}</span>
          </div>
        </div>

        <div className="relative h-[118px] w-[132px] shrink-0 overflow-hidden rounded-[22px] bg-[#f2f4f7] shadow-[0_10px_26px_rgba(20,28,38,0.10)]">
          <img src={foodImage(menuId, imageUrl)} alt={name} loading="lazy" className="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
          <span className="absolute bottom-2 right-2 grid h-10 w-10 place-items-center rounded-full bg-white text-[22px] font-black text-[#45aaf2] shadow-[0_8px_20px_rgba(30,80,130,0.22)]">
            +
          </span>
        </div>
      </div>
    </button>
  );
}