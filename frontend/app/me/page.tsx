"use client";

import { useEffect, useState } from "react";
import { SessionPanel } from "@/components/SessionPanel";
import { decodeJwtPayload, getTokenBundle } from "@/lib/auth-storage";
import { ensureDevSession } from "@/lib/dev-session";
import { apiRequest, HttpError } from "@/lib/http";
import { UserDetailResponse } from "@/lib/types";

export default function MePage() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [user, setUser] = useState<UserDetailResponse | null>(null);

  useEffect(() => {
    void loadMyProfile();
  }, []);

  async function loadMyProfile() {
    setLoading(true);
    setError("");
    try {
      await ensureDevSession();
      const bundle = getTokenBundle();
      if (!bundle?.accessToken) {
        throw new Error("Login required.");
      }

      const payload = decodeJwtPayload(bundle.accessToken);
      const userId = payload?.user_id ?? (payload?.sub ? Number(payload.sub) : NaN);

      if (!Number.isFinite(userId) || Number(userId) <= 0) {
        setError("Failed to parse user_id from access token.");
        setUser(null);
        return;
      }

      const profile = await apiRequest<UserDetailResponse>(`/api/v1/users/${Number(userId)}`, {
        method: "GET",
        token: bundle.accessToken
      });
      setUser(profile);
    } catch (caught) {
      setUser(null);
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="stack">
      <section className="card stack">
        <h1 style={{ margin: 0 }}>My profile</h1>
        <p className="muted" style={{ margin: 0 }}>
          Fetch profile through Gateway + JWT: <code>/api/v1/users/&lt;userId&gt;</code>.
        </p>
        <div className="row">
          <button className="button secondary" onClick={loadMyProfile} disabled={loading}>
            Refresh
          </button>
        </div>
        {loading && <p className="muted" style={{ margin: 0 }}>Loading...</p>}
        {error && <p className="error" style={{ margin: 0 }}>{error}</p>}
        {user && <pre className="pre">{JSON.stringify(user, null, 2)}</pre>}
      </section>

      <SessionPanel />
    </div>
  );
}

function toErrorMessage(caught: unknown): string {
  if (caught instanceof HttpError) {
    return `${caught.code ?? "HTTP_ERROR"}: ${caught.message}`;
  }
  if (caught instanceof Error) {
    return caught.message;
  }
  return "Request failed.";
}
