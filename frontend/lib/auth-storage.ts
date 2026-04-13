import { JwtPayload, TokenBundle } from "./types";

const STORAGE_KEY = "aekiori.auth.tokens";

function isBrowser(): boolean {
  return typeof window !== "undefined";
}

export function getTokenBundle(): TokenBundle | null {
  if (!isBrowser()) {
    return null;
  }

  const raw = window.localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as TokenBundle;
  } catch {
    window.localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

export function setTokenBundle(bundle: Omit<TokenBundle, "issuedAt">): TokenBundle {
  const value: TokenBundle = {
    ...bundle,
    issuedAt: Date.now()
  };

  if (isBrowser()) {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(value));
  }

  return value;
}

export function clearTokenBundle(): void {
  if (!isBrowser()) {
    return;
  }
  window.localStorage.removeItem(STORAGE_KEY);
}

export function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const payloadPart = token.split(".")[1];
    if (!payloadPart) {
      return null;
    }
    const normalized = payloadPart.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=");
    const decoded = atob(padded);
    return JSON.parse(decoded) as JwtPayload;
  } catch {
    return null;
  }
}

