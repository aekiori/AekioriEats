import Link from "next/link";
import { SessionPanel } from "@/components/SessionPanel";

export default function HomePage() {
  return (
    <div className="stack">
      <section className="card stack">
        <h1 className="title">Auth/User/Store/Order MVP</h1>
        <p className="muted" style={{ margin: 0 }}>
          Quick UI to test signup, login, profile, store register, and order create.
        </p>
        <div className="row">
          <Link href="/auth/signup" className="button" style={{ width: "auto", paddingInline: 18 }}>
            Go to sign up
          </Link>
          <Link href="/auth/login" className="button secondary" style={{ width: "auto", paddingInline: 18 }}>
            Go to login
          </Link>
          <Link href="/me" className="button secondary" style={{ width: "auto", paddingInline: 18 }}>
            View profile
          </Link>
          <Link href="/store/owner" className="button secondary" style={{ width: "auto", paddingInline: 18 }}>
            Register store
          </Link>
          <Link href="/store/detail" className="button secondary" style={{ width: "auto", paddingInline: 18 }}>
            View menu detail
          </Link>
          <Link href="/order/create" className="button secondary" style={{ width: "auto", paddingInline: 18 }}>
            Create order
          </Link>
        </div>
      </section>
      <SessionPanel />
    </div>
  );
}
