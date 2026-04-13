import { NextRequest } from "next/server";

const BACKEND_BASE_URL = process.env.BACKEND_BASE_URL ?? "http://localhost:8088";

type RouteContext = {
  params: {
    path?: string[];
  };
};

async function handleProxy(request: NextRequest, { params }: RouteContext): Promise<Response> {
  const base = BACKEND_BASE_URL.endsWith("/")
    ? BACKEND_BASE_URL.slice(0, -1)
    : BACKEND_BASE_URL;
  const path = (params.path ?? []).join("/");
  const targetUrl = new URL(`${base}/${path}`);
  targetUrl.search = request.nextUrl.search;

  const headers = new Headers(request.headers);
  headers.delete("host");
  headers.delete("connection");
  headers.delete("content-length");

  const method = request.method.toUpperCase();
  const hasBody = method !== "GET" && method !== "HEAD";

  const upstreamResponse = await fetch(targetUrl.toString(), {
    method,
    headers,
    body: hasBody ? await request.arrayBuffer() : undefined,
    redirect: "manual"
  });

  const responseHeaders = new Headers(upstreamResponse.headers);
  responseHeaders.delete("transfer-encoding");

  return new Response(upstreamResponse.body, {
    status: upstreamResponse.status,
    statusText: upstreamResponse.statusText,
    headers: responseHeaders
  });
}

export async function GET(request: NextRequest, context: RouteContext): Promise<Response> {
  return handleProxy(request, context);
}

export async function POST(request: NextRequest, context: RouteContext): Promise<Response> {
  return handleProxy(request, context);
}

export async function PUT(request: NextRequest, context: RouteContext): Promise<Response> {
  return handleProxy(request, context);
}

export async function PATCH(request: NextRequest, context: RouteContext): Promise<Response> {
  return handleProxy(request, context);
}

export async function DELETE(request: NextRequest, context: RouteContext): Promise<Response> {
  return handleProxy(request, context);
}

