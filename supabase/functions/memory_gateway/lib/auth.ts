import { createRemoteJWKSet, errors as joseErrors, jwtVerify } from "jose";
import { UnauthorizedError, UNAUTHORIZED_MESSAGES } from "./errors.js";

const UUID_REGEX =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const BEARER_REGEX = /^Bearer\s+(.+)$/i;

export interface VerifiedJwt {
  sub: string;
}

let jwksResolver: ReturnType<typeof createRemoteJWKSet> | null = null;
let jwksUrlForResolver: string | null = null;

function getJwksResolver(supabaseUrl: string): ReturnType<typeof createRemoteJWKSet> {
  const url = `${supabaseUrl}/auth/v1/.well-known/jwks.json`;
  if (jwksResolver === null || jwksUrlForResolver !== url) {
    jwksResolver = createRemoteJWKSet(new URL(url));
    jwksUrlForResolver = url;
  }
  return jwksResolver;
}

export async function verifyJwt(rawHeader: string | null): Promise<VerifiedJwt> {
  if (!rawHeader || !BEARER_REGEX.test(rawHeader)) {
    throw new UnauthorizedError(UNAUTHORIZED_MESSAGES.MISSING_OR_INVALID_HEADER);
  }
  const token = rawHeader.replace(/^Bearer\s+/i, "").trim();
  if (token.length === 0) {
    throw new UnauthorizedError(UNAUTHORIZED_MESSAGES.MISSING_OR_INVALID_HEADER);
  }

  const urlEnv = process.env.SUPABASE_URL;
  if (!urlEnv) {
    throw new UnauthorizedError(UNAUTHORIZED_MESSAGES.TOKEN_VERIFICATION_FAILED);
  }

  const issuer = `${urlEnv}/auth/v1`;
  let payload: Record<string, unknown>;

  try {
    payload = (await jwtVerify(token, getJwksResolver(urlEnv), { issuer }))
      .payload as Record<string, unknown>;
  } catch (jwksErr) {
    const secretEnv = process.env.SUPABASE_JWT_SECRET;
    if (!secretEnv) {
      throw new UnauthorizedError(UNAUTHORIZED_MESSAGES.TOKEN_VERIFICATION_FAILED);
    }
    try {
      payload = (
        await jwtVerify(token, new TextEncoder().encode(secretEnv), { issuer })
      ).payload as Record<string, unknown>;
    } catch (hsErr) {
      if (hsErr instanceof joseErrors.JOSEError || hsErr instanceof Error) {
        throw new UnauthorizedError(UNAUTHORIZED_MESSAGES.TOKEN_VERIFICATION_FAILED);
      }
      if (jwksErr instanceof joseErrors.JOSEError || jwksErr instanceof Error) {
        throw new UnauthorizedError(UNAUTHORIZED_MESSAGES.TOKEN_VERIFICATION_FAILED);
      }
      throw new UnauthorizedError(UNAUTHORIZED_MESSAGES.TOKEN_VERIFICATION_FAILED);
    }
  }

  const sub = payload["sub"];
  if (typeof sub !== "string" || !UUID_REGEX.test(sub)) {
    throw new UnauthorizedError(UNAUTHORIZED_MESSAGES.INVALID_SUBJECT);
  }
  if (payload["role"] !== "authenticated") {
    throw new UnauthorizedError(UNAUTHORIZED_MESSAGES.INVALID_ROLE);
  }
  return { sub };
}
