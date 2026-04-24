import type { Metadata } from "next";
import Link from "next/link";
import { DevSessionBootstrap } from "@/components/DevSessionBootstrap";
import "./globals.css";

export const metadata: Metadata = {
  title: "AekioriEats Frontend MVP",
  description: "Auth/User/Store/Order integration test client"
};

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>
        <DevSessionBootstrap />
        <header className="topbar">
          <div className="container topbar-inner">
            <Link href="/" className="brand">
              AekioriEats
            </Link>
            <nav className="nav">
              <Link href="/auth/signup">Sign up</Link>
              <Link href="/auth/login">Login</Link>
              <Link href="/me">My profile</Link>
              <Link href="/store/owner">Store owner</Link>
              <Link href="/store/detail">Store detail</Link>
              <Link href="/order/create">Create order</Link>
              <Link href="/payment/checkout">Payment</Link>
            </nav>
          </div>
        </header>
        <main className="container page">{children}</main>
      </body>
    </html>
  );
}
