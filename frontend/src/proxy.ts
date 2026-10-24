import { randomBytes } from "node:crypto";
import { NextRequest, NextResponse } from "next/server";
import { checkoutCookieMaxAge, checkoutCookieName, checkoutTokenPattern } from "@/lib/checkout";

export function proxy(request: NextRequest) {
  const existing = request.cookies.get(checkoutCookieName)?.value;
  if (existing && checkoutTokenPattern.test(existing)) return NextResponse.next();

  // REVIEW: Issue the identity on the page request, before any order is written.
  // If the later save succeeds but its response is lost, the browser already has
  // the same identity for its retry. Creating the cookie after saving is too late.
  const token = randomBytes(32).toString("hex");
  request.cookies.set(checkoutCookieName, token);

  // REVIEW: Forward the new cookie to this request's Server Component/Action too;
  // otherwise it would only become readable on the browser's next request.
  const response = NextResponse.next({ request: { headers: request.headers } });
  response.cookies.set(checkoutCookieName, token, {
    httpOnly: true,
    sameSite: "lax",
    // REVIEW: Local Docker uses HTTP, including production builds. Secure cookies
    // must be enabled for HTTPS, rather than being tied only to NODE_ENV.
    secure: request.nextUrl.protocol === "https:",
    path: "/checkout",
    maxAge: checkoutCookieMaxAge,
  });
  return response;
}

export const config = { matcher: "/checkout/:path*" };
