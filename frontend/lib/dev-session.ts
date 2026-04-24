"use client";

import { getTokenBundle, setTokenBundle } from "@/lib/auth-storage";
import { apiRequest, HttpError } from "@/lib/http";
import { AuthTokenResponse } from "@/lib/types";

const DEV_EMAIL = "order-tester@example.com";
const DEV_PASSWORD = "Passw0rd!";

let ensureDevSessionPromise: Promise<AuthTokenResponse> | null = null;

export async function ensureDevSession(): Promise<AuthTokenResponse> {
  const existingBundle = getTokenBundle();
  if (existingBundle?.accessToken) {
    return existingBundle;
  }

  if (ensureDevSessionPromise) {
    return ensureDevSessionPromise;
  }

  ensureDevSessionPromise = (async () => {
    try {
      return setTokenBundle(await login());
    } catch (caught) {
      if (!(caught instanceof HttpError) || caught.status >= 500) {
        throw caught;
      }

      return setTokenBundle(
        await apiRequest<AuthTokenResponse>("/api/v1/auth/signup", {
          method: "POST",
          body: {
            email: DEV_EMAIL,
            password: DEV_PASSWORD
          }
        })
      );
    }
  })();

  try {
    return await ensureDevSessionPromise;
  } finally {
    ensureDevSessionPromise = null;
  }
}

export function getDevCredentials() {
  return {
    email: DEV_EMAIL,
    password: DEV_PASSWORD
  };
}

async function login(): Promise<AuthTokenResponse> {
  return apiRequest<AuthTokenResponse>("/api/v1/auth/login", {
    method: "POST",
    body: {
      email: DEV_EMAIL,
      password: DEV_PASSWORD
    }
  });
}
