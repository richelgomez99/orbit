#!/usr/bin/env node

import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

const GATEWAY_DIR = resolve(import.meta.dirname, "..");
const FIXTURE_PATH = resolve(GATEWAY_DIR, "test/fixtures/retrieval-eval.jsonl");

const ITEMS = new Map([
  ["eval-qr-customers", item("eval-qr-customers", "How to get first 1000 customers QR code", "Context note says this is the QR code for first 1000 customers.")],
  ["eval-dentist-update", item("eval-dentist-update", "Dentist appointment update", "Dental office message about rescheduling the appointment after the original slot changed.")],
  ["eval-package-pickup", item("eval-package-pickup", "Package pickup", "Remember to reschedule a package pickup.")],
  ["eval-startup-event", item("eval-startup-event", "Startup event ticket", "Startup event ticket for Monday founder office hours. Venue doors open at 6pm.")],
  ["eval-flight-receipt", item("eval-flight-receipt", "Flight receipt", "Flight receipt: NYC to San Francisco, confirmation ORB123.")],
  ["eval-recipe", item("eval-recipe", "Miso salmon recipe", "Recipe to try: miso salmon with ginger rice.")],
]);

function item(id, title, text) {
  return {
    id,
    title,
    text,
    createdAtMillis: 1780150000000,
  };
}

function terms(query) {
  return query.toLowerCase().split(/[^a-z0-9]+/).filter((term) => term.length >= 3);
}

function lexicalScore(item, query) {
  const haystack = `${item.title} ${item.text}`.toLowerCase();
  const title = item.title.toLowerCase();
  let score = 0;
  for (const term of new Set(terms(query))) {
    if (haystack.includes(term)) score += 1;
    if (title.includes(term)) score += 2;
  }
  if (haystack.includes(terms(query).join(" "))) score += 3;
  return score;
}

function mergeHybrid(query, lexicalIds, vectorEntries) {
  const byId = new Map();
  for (const id of lexicalIds) {
    const found = ITEMS.get(id);
    if (found) byId.set(id, { item: found, semanticScore: undefined });
  }
  for (const vector of vectorEntries) {
    const found = ITEMS.get(vector.id);
    if (found) byId.set(vector.id, { item: found, semanticScore: vector.score });
  }
  return [...byId.values()]
    .map(({ item, semanticScore }) => ({
      item,
      score: (semanticScore ?? 0) * 10 + lexicalScore(item, query),
    }))
    .filter((entry) => entry.score > 0)
    .sort((a, b) => b.score - a.score || b.item.createdAtMillis - a.item.createdAtMillis);
}

if (!existsSync(FIXTURE_PATH)) {
  throw new Error(`Missing fixture: ${FIXTURE_PATH}`);
}

const cases = readFileSync(FIXTURE_PATH, "utf8")
  .split(/\r?\n/)
  .filter(Boolean)
  .map((line) => JSON.parse(line));

let passed = 0;
const failures = [];
for (const testCase of cases) {
  const results = mergeHybrid(testCase.query, testCase.lexical ?? [], testCase.vector ?? []);
  const top = results[0]?.item.id ?? null;
  if (top === testCase.expectedTop) {
    passed += 1;
  } else {
    failures.push({ query: testCase.query, expectedTop: testCase.expectedTop, actualTop: top });
  }
}

const report = {
  ok: failures.length === 0,
  passed,
  total: cases.length,
  failures,
};
console.log(JSON.stringify(report, null, 2));
if (failures.length > 0) process.exit(1);
