import { NextRequest, NextResponse } from 'next/server';

const PUBLIC_PATHS = ['/login', '/customer'];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Allow public routes, static assets, and API routes
  if (PUBLIC_PATHS.some((p) => pathname.startsWith(p))) {
    return NextResponse.next();
  }

  const token = request.cookies.get('bc_token')?.value;
  if (!token) {
    const url = request.nextUrl.clone();
    url.pathname = '/login';
    return NextResponse.redirect(url);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon\\.ico|api/).*)'],
};
