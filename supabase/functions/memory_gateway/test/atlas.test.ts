import { describe, expect, it, vi } from "vitest";
import {
  buildMemoryDocument,
  searchMemoryItems,
  tombstoneMemoryItem,
  upsertMemoryItem,
} from "../lib/atlas.js";
import type { Collection } from "mongodb";
import type { MemoryIndexItem, MemoryIndexItemInput } from "../types.js";

const item: MemoryIndexItemInput = {
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
};

describe("atlas helpers", () => {
  it("stamps authenticated userId and clears tombstone on document build", () => {
    const doc = buildMemoryDocument("user-1", item, 123);
    expect(doc.userId).toBe("user-1");
    expect(doc.envelopeId).toBe("env-1");
    expect(doc.tombstonedAt).toBeNull();
  });

  it("upserts by userId + envelopeId", async () => {
    const updateOne = vi.fn().mockResolvedValue({ upsertedId: "new-id" });
    const collection = { updateOne } as unknown as Collection<MemoryIndexItem>;
    const result = await upsertMemoryItem(collection, "user-1", item, 123);
    expect(result.upserted).toBe(true);
    expect(updateOne).toHaveBeenCalledWith(
      { userId: "user-1", envelopeId: "env-1" },
      { $set: expect.objectContaining({ userId: "user-1", envelopeId: "env-1" }) },
      { upsert: true },
    );
  });

  it("tombstones only the authenticated user's matching item", async () => {
    const updateOne = vi.fn().mockResolvedValue({ matchedCount: 1, modifiedCount: 1 });
    const collection = { updateOne } as unknown as Collection<MemoryIndexItem>;
    const result = await tombstoneMemoryItem(collection, "user-1", "env-1", 456);
    expect(result).toEqual({ matched: true, modified: true });
    expect(updateOne).toHaveBeenCalledWith(
      { userId: "user-1", envelopeId: "env-1" },
      { $set: { tombstonedAt: 456, updatedAtMillis: 456 } },
    );
  });

  it("search filters by user and excludes tombstoned records", async () => {
    const toArray = vi.fn().mockResolvedValue([
      buildMemoryDocument("user-1", item, 123),
    ]);
    const limit = vi.fn().mockReturnValue({ toArray });
    const sort = vi.fn().mockReturnValue({ limit });
    const find = vi.fn().mockReturnValue({ sort });
    const collection = { find } as unknown as Collection<MemoryIndexItem>;

    const results = await searchMemoryItems(collection, "user-1", "startup");
    expect(results[0]?.envelopeId).toBe("env-1");
    expect(find).toHaveBeenCalledWith(
      expect.objectContaining({
        userId: "user-1",
        $or: [{ tombstonedAt: null }, { tombstonedAt: { $exists: false } }],
      }),
    );
  });
});
