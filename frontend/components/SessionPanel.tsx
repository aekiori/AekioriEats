"use client";

import { useEffect, useMemo, useState } from "react";
import { apiRequest, HttpError } from "@/lib/http";
import { clearTokenBundle, decodeJwtPayload, getTokenBundle, setTokenBundle } from "@/lib/auth-storage";
import { AuthTokenResponse, JwtPayload } from "@/lib/types";

export function SessionPanel() {
  const [payload, setPayload] = useState<JwtPayload | null>(null);
  const [message, setMessage] = useState<string>("");
  const [error, setError] = useState<string>("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const bundle = getTokenBundle();
    setPayload(bundle ? decodeJwtPayload(bundle.accessToken) : null);
  }, []);

  const hasSession = useMemo(() => payload !== null, [payload]);

  async function refreshToken() {
    setLoading(true);
    setError("");
    setMessage("");
    try {
      const bundle = getTokenBundle();
      if (!bundle?.refreshToken) {
        setError("No refresh token in storage.");
        return;
      }
      const response = await apiRequest<AuthTokenResponse>("/api/v1/auth/refresh", {
        method: "POST",
        body: { refreshToken: bundle.refreshToken }
      });
      const next = setTokenBundle(response);
      setPayload(decodeJwtPayload(next.accessToken));
      setMessage("Token refresh complete.");
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function logout() {
    setLoading(true);
    setError("");
    setMessage("");
    try {
      const bundle = getTokenBundle();
      if (bundle?.refreshToken) {
        await apiRequest<void>("/api/v1/auth/logout", {
          method: "POST",
          body: { refreshToken: bundle.refreshToken }
        });
      }
      clearTokenBundle();
      setPayload(null);
      setMessage("Logout complete.");
    } catch (caught) {
      clearTokenBundle();
      setPayload(null);
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="card stack">
      <h3 style={{ margin: 0 }}>Session status</h3>
      {!hasSession ? (
        <p className="muted" style={{ margin: 0 }}>
          Not logged in yet.
        </p>
      ) : (
        <>
          <p className="muted" style={{ margin: 0 }}>
            user_id: <b>{payload?.user_id ?? payload?.sub ?? "unknown"}</b> / role:{" "}
            <b>{payload?.role ?? "unknown"}</b>
          </p>
          <div className="row">
            <button className="button secondary" onClick={refreshToken} disabled={loading}>
              Refresh token
            </button>
            <button className="button danger" onClick={logout} disabled={loading}>
              Logout
            </button>
          </div>
          <pre className="pre">{JSON.stringify(payload, null, 2)}</pre>
        </>
      )}
      {message && <p className="success" style={{ margin: 0 }}>{message}</p>}
      {error && <p className="error" style={{ margin: 0 }}>{error}</p>}
    </section>
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
