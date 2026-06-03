import { afterAll, beforeAll, describe, expect, it } from "vitest";
import { SignJWT } from "jose";
import { verifyJwt } from "../lib/auth.js";
import { UnauthorizedError, UNAUTHORIZED_MESSAGES } from "../lib/errors.js";

const TEST_SECRET = "test-jwt-secret-must-be-long-enough-32+";
const TEST_SUPABASE_URL = "https://test-project.supabase.co";
const VALID_SUB = "11111111-1111-4111-8111-111111111111";

let savedSecret: string | undefined;
let savedUrl: string | undefined;

beforeAll(() => {
  savedSecret = process.env.SUPABASE_JWT_SECRET;
  savedUrl = process.env.SUPABASE_URL;
  process.env.SUPABASE_JWT_SECRET = TEST_SECRET;
  process.env.SUPABASE_URL = TEST_SUPABASE_URL;
});

afterAll(() => {
  if (savedSecret === undefined) delete process.env.SUPABASE_JWT_SECRET;
  else process.env.SUPABASE_JWT_SECRET = savedSecret;
  if (savedUrl === undefined) delete process.env.SUPABASE_URL;
  else process.env.SUPABASE_URL = savedUrl;
});

async function mintToken(opts: { sub?: string; role?: string; secret?: string }) {
  const builder = new SignJWT({
    role: opts.role ?? "authenticated",
    aud: "authenticated",
    ...(opts.sub !== undefined ? { sub: opts.sub } : {}),
  })
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt()
    .setIssuer(`${TEST_SUPABASE_URL}/auth/v1`)
    .setExpirationTime("1h");
  if (opts.sub !== undefined) builder.setSubject(opts.sub);
  return builder.sign(new TextEncoder().encode(opts.secret ?? TEST_SECRET));
}

describe("verifyJwt", () => {
  it("rejects missing headers", async () => {
    await expect(verifyJwt(null)).rejects.toThrow(UnauthorizedError);
    await expect(verifyJwt(null)).rejects.toMatchObject({
      publicMessage: UNAUTHORIZED_MESSAGES.MISSING_OR_INVALID_HEADER,
    });
  });

  it("rejects invalid roles", async () => {
    const token = await mintToken({ sub: VALID_SUB, role: "service_role" });
    await expect(verifyJwt(`Bearer ${token}`)).rejects.toMatchObject({
      publicMessage: UNAUTHORIZED_MESSAGES.INVALID_ROLE,
    });
  });

  it("accepts valid Supabase-shaped tokens", async () => {
    const token = await mintToken({ sub: VALID_SUB });
    await expect(verifyJwt(`Bearer ${token}`)).resolves.toEqual({ sub: VALID_SUB });
  });
});
