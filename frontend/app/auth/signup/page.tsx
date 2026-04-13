"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { apiRequest, HttpError } from "@/lib/http";
import { setTokenBundle } from "@/lib/auth-storage";
import { AuthTokenResponse, EmailExistsResponse } from "@/lib/types";

export default function SignupPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [emailCheck, setEmailCheck] = useState<string>("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function handleCheckEmail() {
    if (!email.trim()) {
      setEmailCheck("Enter email first.");
      return;
    }

    setLoading(true);
    setEmailCheck("");
    setError("");
    try {
      const data = await apiRequest<EmailExistsResponse>(
        `/api/v1/auth/email/exists?email=${encodeURIComponent(email.trim())}`
      );
      setEmailCheck(data.exists ? "Email already exists." : "Email is available.");
    } catch (caught) {
      setEmailCheck(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (password !== passwordConfirm) {
      setError("Password confirmation does not match.");
      return;
    }

    setLoading(true);
    try {
      const data = await apiRequest<AuthTokenResponse>("/api/v1/auth/signup", {
        method: "POST",
        body: { email, password }
      });

      setTokenBundle(data);
      setSuccess("Signup complete. Logged in.");
      setTimeout(() => router.push("/me"), 350);
    } catch (caught) {
      setError(toErrorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="grid-2">
      <section className="card stack">
        <h1 style={{ margin: 0 }}>Sign up</h1>
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
          <div className="row">
            <button type="button" className="button secondary" onClick={handleCheckEmail} disabled={loading}>
              Check duplicate email
            </button>
            {emailCheck && <span className={emailCheck.includes("available") ? "success" : "muted"}>{emailCheck}</span>}
          </div>
          <div>
            <div className="label">Password (8~72 chars)</div>
            <input
              className="input"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              minLength={8}
              maxLength={72}
              required
            />
          </div>
          <div>
            <div className="label">Confirm password</div>
            <input
              className="input"
              type="password"
              value={passwordConfirm}
              onChange={(event) => setPasswordConfirm(event.target.value)}
              minLength={8}
              maxLength={72}
              required
            />
          </div>
          <button className="button" type="submit" disabled={loading}>
            Sign up and login
          </button>
          {error && <p className="error" style={{ margin: 0 }}>{error}</p>}
          {success && <p className="success" style={{ margin: 0 }}>{success}</p>}
        </form>
      </section>

      <section className="card stack">
        <h2 style={{ margin: 0 }}>Notes</h2>
        <p className="muted" style={{ margin: 0 }}>
          On success, tokens are saved in localStorage and redirected to `/me`.
        </p>
        <p className="muted" style={{ margin: 0 }}>
          If you already have an account, go to <Link href="/auth/login">login</Link>.
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
