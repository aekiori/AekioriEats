import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./lib/**/*.{js,ts,jsx,tsx,mdx}"
  ],
  theme: {
    extend: {
      colors: {
        eats: {
          bg: "#f6f7fb",
          card: "#ffffff",
          line: "#eceff4",
          text: "#14171f",
          muted: "#7d8596",
          brand: "#1f66ff",
          brandSoft: "#e8f0ff",
          danger: "#e5484d",
          badge: "#f5f6fa"
        }
      },
      boxShadow: {
        eats: "0 4px 20px rgba(15, 23, 42, 0.06)"
      },
      fontFamily: {
        sans: ["Pretendard", "Noto Sans KR", "Segoe UI", "sans-serif"]
      }
    }
  },
  plugins: []
};

export default config;
