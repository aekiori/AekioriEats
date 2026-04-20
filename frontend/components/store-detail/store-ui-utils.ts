export const formatWon = (amount: number): string => `${amount.toLocaleString("ko-KR")}원`;

export const fallbackFoodImages = [
  "https://images.unsplash.com/photo-1626645738196-c2a7c87a8f58?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1562967916-eb82221dfb92?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1606755962773-d324e2a13086?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1569058242567-93de6f36f8eb?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=900&q=80"
];

export function foodImage(seed: number, imageUrl?: string | null): string {
  if (imageUrl && imageUrl.startsWith("http") && !imageUrl.includes("cdn.aekiori.dev")) return imageUrl;
  return fallbackFoodImages[Math.abs(seed) % fallbackFoodImages.length];
}

export type StoreOperatingHourLike = {
  dayOfWeek: number;
  openTime: string | null;
  closeTime: string | null;
};

export function isStoreOpenNow(hours: StoreOperatingHourLike[], now: Date = new Date()): boolean {
  const currentDay = toDomainDayOfWeek(now);
  const yesterday = currentDay === 1 ? 7 : currentDay - 1;
  const currentMinutes = now.getHours() * 60 + now.getMinutes();

  return hours.some((hour) => {
    if (!hour.openTime || !hour.closeTime) return false;

    const openMinutes = toMinutes(hour.openTime);
    const closeMinutes = toMinutes(hour.closeTime);

    if (openMinutes === closeMinutes) return true;

    if (closeMinutes < openMinutes) {
      if (hour.dayOfWeek === currentDay && currentMinutes >= openMinutes) return true;
      if (hour.dayOfWeek === yesterday && currentMinutes < closeMinutes) return true;
      return false;
    }

    return hour.dayOfWeek === currentDay && currentMinutes >= openMinutes && currentMinutes < closeMinutes;
  });
}

function toDomainDayOfWeek(date: Date): number {
  const jsDay = date.getDay();
  return jsDay === 0 ? 7 : jsDay;
}

function toMinutes(time: string): number {
  const [hour = "0", minute = "0"] = time.split(":");
  return Number(hour) * 60 + Number(minute);
}
