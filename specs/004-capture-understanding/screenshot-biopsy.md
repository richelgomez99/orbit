# Screenshot Biopsy Protocol

## Purpose

Validate whether Active Intent cleanup is a strong enough mainstream wedge before expanding architecture beyond the local-first Basic loop.

The research question is not "can Orbit classify screenshots?" The question is:

> Can Orbit find unresolved saved intent, extract the completion key, and help the user close the loop?

## Recruit Criteria

Recruit 15-20 Android users who match at least one criterion:

- They have 500+ screenshots in Gallery.
- They say they screenshot everything.
- They use screenshots for shopping, receipts, recipes, travel, tickets, chat reminders, or read-later behavior.
- They feel their Gallery or saved-content pile is hard to manage.

Avoid shame language in recruiting. Use phrases like screenshot pile, unfinished saves, open loops, phone clutter, and things saved for later.

## Session Flow

1. Ask the user to screen-share or export their last 100 screenshots.
2. Label each screenshot with the worksheet fields below.
3. Ask the user whether Orbit's inferred category and completion key feel trustworthy.
4. Offer a simulated Active Intent action: archive, resolve, add to list, copy code, create reminder, save place, or mark not interested.
5. Record whether the user takes an action.

## Worksheet Fields

| Field | Values / Notes |
|-------|----------------|
| Screenshot ID | Local session ID only, no personal filename required |
| Has intent? | yes / no / unclear |
| Still active? | active / resolved / expired / stale / unclear |
| Category | buy later, recipe, QR/barcode, receipt/order, event/ticket/reservation, coupon/promo, read/watch later, place/travel, gift idea, chat action, maybe old/inactive, unknown |
| Completion key needed | Product identity, ingredients, decoded payload, order ID, date/time/location, coupon code/expiry, canonical URL, place identity, requested action, other |
| Completion key extractable? | yes / partial / no |
| Evidence source | foreground app, URL/deeplink, OCR text, visible logo, visual inference, user explanation |
| Trust level | high / medium / low / would not trust |
| Best action | archive, resolve, add to list, copy code, create reminder, save place, mark not interested, escalate, none |
| User action taken? | yes / no |
| Notes | What confused or reassured the user |

## Pass Thresholds

- At least 40% of screenshots have recoverable intent.
- At least 25% are still active or unresolved.
- Orbit can extract a useful completion key for 60%+ of active screenshots.
- At least 50% of users say the cleanup view makes Gallery feel less overwhelming.
- At least 30% take a real action: archive, resolve, add to list, copy code, create reminder, save place, or mark not interested.

## Red Flags

- Users reject the Active Intent grouping as creepy or judgmental.
- Completion keys are mostly missing for active screenshots.
- Users want search far more than cleanup.
- Users expect auto-delete or auto-action, which Orbit should not provide by default.
- Most active screenshots require cloud VLM to become useful, weakening the Basic local wedge.

## Output

Write results back to `specs/004-capture-understanding/quickstart.md` after the device/user session:

- participant count
- screenshots reviewed
- category distribution
- active/unresolved percentage
- completion key extraction rate
- actions taken
- trust notes
- top extraction failures
- product decision: continue, narrow, or reframe
