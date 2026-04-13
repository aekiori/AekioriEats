"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { apiRequest, HttpError } from "@/lib/http";
import { setTokenBundle } from "@/lib/auth-storage";
import { AuthTokenResponse } from "@/lib/types";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);

    try {
      const data = await apiRequest<AuthTokenResponse>("/api/v1/auth/login", {
        method: "POST",
        body: { email, password }
      });
      setTokenBundle(data);
      setSuccess("Login complete.");
      setTimeout(() => router.push("/me"), 250);
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="grid-2">
      <section className="card stack">
        <h1 style={{ margin: 0 }}>Login</h1>
        <form className="stack" onSubmit={handleSubmit}>
          <div>
            <div className="label">Email</div>
            <input
              className="input"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@example.com"
              required
            />
          </div>
          <div>
            <div className="label">Password</div>
            <input
              className="input"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </div>
          <button className="button" type="submit" disabled={loading}>
            Login
          </button>
          {error && <p className="error" style={{ margin: 0 }}>{error}</p>}
          {success && <p className="success" style={{ margin: 0 }}>{success}</p>}
        </form>
      </section>

      <section className="card stack">
        <h2 style={{ margin: 0 }}>Notes</h2>
        <p className="muted" style={{ margin: 0 }}>
          On success, tokens are stored and `/me` loads User API.
        </p>
        <p className="muted" style={{ margin: 0 }}>
          No account yet? Go to <Link href="/auth/signup">sign up</Link>.
        </p>
      </section>
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
