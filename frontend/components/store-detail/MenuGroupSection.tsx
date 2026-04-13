"use client";

import { MenuItemCard } from "./MenuItemCard";

export type MenuCardViewModel = {
  menuId: number;
  name: string;
  description: string | null;
  price: number;
  imageUrl: string | null;
  badges: string[];
};

type MenuGroupSectionProps = {
  title: string;
  items: MenuCardViewModel[];
  onSelectMenu: (menuId: number) => void;
};

export function MenuGroupSection({ title, items, onSelectMenu }: MenuGroupSectionProps) {
  if (items.length === 0) {
    return null;
  }

  return (
    <section className="overflow-hidden rounded-3xl border border-[#e9edf4] bg-eats-card shadow-eats">
      <header className="border-b border-eats-line px-4 py-4 md:px-5">
        <h2 className="text-[20px] font-bold text-eats-text">{title}</h2>
      </header>
      <div>
        {items.map((item, index) => (
          <div key={`${title}-${item.menuId}`} className={index === items.length - 1 ? "[&>button]:border-b-0" : ""}>
            <MenuItemCard
              menuId={item.menuId}
              name={item.name}
              description={item.description}
              price={item.price}
              imageUrl={item.imageUrl}
              badges={item.badges}
              onSelect={() => onSelectMenu(item.menuId)}
            />
          </div>
        ))}
      </div>
    </section>
  );
}
