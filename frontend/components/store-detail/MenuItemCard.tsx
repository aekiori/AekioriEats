"use client";

type MenuItemCardProps = {
  menuId: number;
  name: string;
  description: string | null;
  price: number;
  imageUrl: string | null;
  badges: string[];
  onSelect: () => void;
};

export function MenuItemCard({ menuId, name, description, price, imageUrl, badges, onSelect }: MenuItemCardProps) {
  return (
    <button
      type="button"
      className="group w-full border-b border-eats-line px-4 py-4 text-left transition-colors hover:bg-[#fafbff] md:px-5"
      onClick={onSelect}
    >
      <div className="flex items-center gap-4">
        <div className="min-w-0 flex-1">
          <div className="mb-1 flex flex-wrap items-center gap-2">
            {badges.map((badge) => (
              <span
                key={`${menuId}-${badge}`}
                className="rounded-full border border-[#dce1eb] bg-eats-badge px-2 py-[2px] text-[11px] font-medium text-[#636d82]"
              >
                {badge}
              </span>
            ))}
          </div>
          <h3 className="line-clamp-1 text-[18px] font-bold leading-snug text-eats-text">{name}</h3>
          <p className="mt-1 line-clamp-2 text-[15px] font-normal leading-5 text-eats-muted">
            {description?.trim() || "메뉴 설명이 아직 등록되지 않았어."}
          </p>
          <p className="mt-2 text-[28px] font-medium leading-none tracking-tight text-eats-text">{toWon(price)}</p>
        </div>

        <div className="relative aspect-[4/3] w-[132px] shrink-0 overflow-hidden rounded-2xl border border-[#e3e7f0] bg-white">
          {imageUrl ? (
            <img src={imageUrl} alt={name} loading="lazy" className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-[12px] font-medium text-[#9ba5ba]">
              NO IMAGE
            </div>
          )}
        </div>
      </div>
    </button>
  );
}

function toWon(price: number): string {
  return `${price.toLocaleString("ko-KR")}원`;
}
