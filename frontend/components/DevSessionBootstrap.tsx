"use client";

import { useEffect } from "react";
import { ensureDevSession } from "@/lib/dev-session";

export function DevSessionBootstrap() {
  useEffect(() => {
    void ensureDevSession().catch(() => {
      // 개별 화면에서 다시 보여준다.
    });
  }, []);

  return null;
}
