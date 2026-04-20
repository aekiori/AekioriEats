"use client";

import { MenuItemCard } from "./MenuItemCard";

export type MenuCardViewModel = {
  menuId: number;
  name: string;
  description: string | null;
  price: number;
  imageUrl: string | null;
  badges: string[];
  orderCount?: number;
  reviewCount?: number;
  likeRate?: number;
};

type MenuGroupSectionProps = {
  title: string;
  items: MenuCardViewModel[];
  onSelectMenu: (menuId: number) => void;
};

export function MenuGroupSection({ title, items, onSelectMenu }: MenuGroupSectionProps) {
  if (items.length === 0) return null;

  return (
    <section className="bg-white" id={`menu-group-${title}`}>
      <div className="px-5 pb-2 pt-7">
        <p className="text-[13px] font-semibold uppercase tracking-[0.22em] text-[#9aa3ad]">Menu</p>
        <h2 className="mt-1 text-[22px] font-extrabold tracking-[-0.04em] text-[#20242c]">{title}</h2>
      </div>
      <div className="divide-y divide-[#edf0f4]">
        {items.map((item) => (
          <MenuItemCard
            key={`${title}-${item.menuId}`}
            menuId={item.menuId}
            name={item.name}
            description={item.description}
            price={item.price}
            imageUrl={item.imageUrl}
            badges={item.badges}
            orderCount={item.orderCount}
            reviewCount={item.reviewCount}
            likeRate={item.likeRate}
            onSelect={() => onSelectMenu(item.menuId)}
          />
        ))}
      </div>
    </section>
  );
}
