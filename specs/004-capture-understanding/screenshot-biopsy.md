# Screenshot Biopsy Protocol

## Purpose

Validate whether Active Intent cleanup is a strong enough mainstream wedge before expanding architecture beyond the Basic local loop.

The research question is:

> Can Orbit find unresolved saved intent, extract the completion key, and help the user close the loop?

## Recruit Criteria

Use 15-20 Android users when available. For internal dogfood, start with the S24 screenshot pile; Tab S9 verification is intentionally skipped for this slice.

A useful participant has at least one of these traits:

- 500+ screenshots in Gallery.
- Screenshots shopping, receipts, recipes, travel, tickets, chats, or read-later items.
- Feels their saved-content pile is hard to manage.

Avoid shame language. Use phrases like screenshot pile, unfinished saves, open loops, phone clutter, and things saved for later.

## Worksheet

| Field | Values / Notes |
| --- | --- |
| Screenshot ID | Local session ID only |
| Has intent? | yes / no / unclear |
| Still active? | active / resolved / expired / stale / unclear |
| Category | buy later, recipe, QR/barcode, receipt/order, event/ticket/reservation, coupon/promo, read/watch later, place/travel, gift idea, chat action, maybe old/inactive, unknown |
| Completion key needed | product identity, ingredients, decoded payload, order ID, date/time/location, coupon code/expiry, canonical URL, place identity, requested action, other |
| Completion key extractable? | yes / partial / no |
| Evidence source | foreground app, URL/deeplink, OCR text, visible logo, local regex, user explanation |
| Trust level | high / medium / low / would not trust |
| Best action | archive, resolve, add to list, copy code, create reminder, save place, mark not interested, escalate, none |
| User action taken? | yes / no |
| Notes | What confused or reassured the user |

## Pass Thresholds

- At least 40% of screenshots have recoverable intent.
- At least 25% are still active or unresolved.
- Orbit can extract a useful completion key or honest missing-key state for 60%+ of active screenshots.
- At least 50% of users say the cleanup view makes Gallery feel less overwhelming.
- At least 30% take a real action: archive, resolve, add to list, copy code, create reminder, save place, or mark not interested.

## Red Flags

- Users reject grouping as creepy or judgmental.
- Completion keys are mostly missing for active screenshots.
- Users want search far more than cleanup.
- Users expect auto-delete or auto-action.
- Most active screenshots require cloud VLM to become useful.

## Output

Record results in `quickstart.md` after device/user sessions:

- participant count
- screenshots reviewed
- category distribution
- active/unresolved percentage
- completion key extraction rate
- actions taken
- trust notes
- top extraction failures
- product decision: continue, narrow, or reframe
