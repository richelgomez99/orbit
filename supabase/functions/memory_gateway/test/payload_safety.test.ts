import { describe, expect, it } from "vitest";
import { containsBannedKey, MemoryGatewayRequestSchema } from "../lib/schemas.js";

const RID = "550e8400-e29b-41d4-a716-446655440000";

function validUpsert(extraItem: Record<string, unknown> = {}) {
  return {
    type: "memory_upsert",
    requestId: RID,
    payload: {
      item: {
        envelopeId: "env-1",
        schemaVersion: 1,
        kind: "REGULAR",
        dayLocal: "2026-05-30",
        createdAtMillis: 1780150000000,
        intent: "READ_OR_WATCH_LATER",
        contentType: "image",
        title: "Startup event flyer",
        summary: "Saved event details for later.",
        sourceAppLabel: "Chrome",
        appCategory: "browser",
        tags: ["event", "startup"],
        evidence: [
          {
            kind: "OCR_HINT",
            label: "Clue",
            excerpt: "Demo day, June 4",
            source: "understanding",
          },
        ],
        compactText: "Startup event flyer Demo day, June 4",
        ...extraItem,
      },
    },
  };
}

describe("payload safety", () => {
  it("accepts compact memory upsert payloads", () => {
    expect(MemoryGatewayRequestSchema.safeParse(validUpsert()).success).toBe(true);
  });

  it("rejects raw OCR bodies", () => {
    const parsed = MemoryGatewayRequestSchema.safeParse(validUpsert({ rawOcr: "full body" }));
    expect(parsed.success).toBe(false);
  });

  it("rejects nested token/key shaped fields", () => {
    expect(containsBannedKey({ nested: [{ refreshToken: "secret" }] })).toBe("refreshToken");
    expect(containsBannedKey({ nested: { apiKey: "secret" } })).toBe("apiKey");
  });

  it("enforces capped excerpts", () => {
    const parsed = MemoryGatewayRequestSchema.safeParse(
      validUpsert({
        evidence: [
          {
            kind: "OCR_HINT",
            label: "Clue",
            excerpt: "x".repeat(241),
            source: "understanding",
          },
        ],
      }),
    );
    expect(parsed.success).toBe(false);
  });
});
