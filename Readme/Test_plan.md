# HubRise — Test Plan & Phase 11 Design

---

## Part 1 — How to Test Notifications (Phase 9)

### Prerequisites
- Backend running: `python manage.py runserver 0.0.0.0:8000` (use the venv at `~/backend/.venv/bin/python`)
- Two accounts logged in on two devices / emulators (or one account on device + one via curl/Postman)

---

### 1.1 Follow Notification

**Trigger:** User A follows User B.

**Steps:**
1. Log in as User A on device/emulator.
2. Navigate to any post by User B → tap their avatar → opens User B's profile.
3. Tap **Follow**.
4. Switch to User B's account (second device or re-login).
5. Tap the **bell icon** on the Home screen → opens Notifications screen.

**Expected result:**
- Notification: `"@userA started following you"`
- Sender avatar shown next to the message.
- Time shown as relative ("just now", "5m", etc.).
- After opening the screen the notification is marked read (blue dot disappears on next open).

**Tapping the notification:** navigates to User A's public profile.

---

### 1.2 Like Notification

**Trigger:** User A likes a post authored by User B.

**Steps:**
1. As User A, find a post by User B on the Home feed or inside a Hub.
2. Tap the ❤️ like button.
3. Switch to User B's account.
4. Open Notifications.

**Expected result:**
- Notification: `"@userA liked your post"`
- No self-notification: if User B likes their own post, no notification is generated.

**Tapping:** navigates to User A's profile (post navigation not yet wired, coming in Phase 11).

---

### 1.3 Comment Notification

**Trigger:** User A comments on a post authored by User B.

**Steps:**
1. As User A, tap the 💬 comment button on a post by User B.
2. Type a comment and submit.
3. Switch to User B → open Notifications.

**Expected result:**
- Notification: `"@userA commented on your post"`
- No self-notification if User B comments on their own post.

---

### 1.4 Unread Count

The bell icon on the Home screen does **not** yet show a badge count (badge rendering is a future UI task). The unread count API exists at `GET /api/notifications/unread-count/` and returns `{"count": N}` — can be verified with curl:

```bash
curl -H "Authorization: Bearer <token>" http://192.168.x.x:8000/api/notifications/unread-count/
```

Reading the notifications list (`GET /api/notifications/`) auto-marks all as read in one shot.

---

### 1.5 API Endpoints (manual curl testing)

```bash
BASE=http://192.168.3.230:8000
TOKEN=<your JWT access token>

# Get notifications
curl -H "Authorization: Bearer $TOKEN" $BASE/api/notifications/

# Get unread count
curl -H "Authorization: Bearer $TOKEN" $BASE/api/notifications/unread-count/
```

---

## Part 2 — How Hub Management Works Now (Phase 10)

### 2.1 Opening a Hub

From the **Hubs** tab (bottom nav):
- **Explore tab** — shows recommended and public hubs as cards.
- **My Hubs tab** — shows hubs you have joined.
- Tapping any hub card opens **Hub Detail**.

From the **Home** feed:
- Tapping the hub name on a post navigates to that hub's detail screen.

From **Search** (magnifying glass on Home):
- Switch to the **Hubs** tab in search results → tap any result.

---

### 2.2 Hub Detail Screen (Reddit-inspired)

The screen has a `CoordinatorLayout` layout — the cover banner and hub info **scroll away** as you scroll through content, while the **tab bar stays pinned** at the top.

**Header sections (collapse on scroll):**
| Element | Description |
|---|---|
| Cover banner (180dp) | Hub cover image; grey placeholder if none |
| Back arrow | Top-left circle button, always visible |
| Settings gear | Top-right circle button — **only visible to the hub creator** |
| Hub avatar | 64dp circle with the first letter of the hub name in blue, overlaps the cover bottom edge |
| Hub name | Bold, 22sp |
| Category · N members | Subtitle line |
| Description | Up to 3 lines |
| Join / Joined button | Right-aligned; "Join Hub" (blue filled) → "Joined" (blue outline) |

**Three tabs (pinned):**

| Tab | Content |
|---|---|
| **Posts** | Paginated list of hub posts with like/comment/user tap support |
| **Challenges** | List of challenges in this hub; loads on first tab switch |
| **About** | Plain text block: full description, category, member count, visibility, creator username |

---

### 2.3 Joining & Leaving

- **Join Hub**: POST `/api/hubs/<id>/join/` — adds membership, increments `members_count`.
- **Leave Hub**: POST `/api/hubs/<id>/leave/` — removes membership, decrements count.
- Button label and style update instantly without reload.
- A user cannot join more than 20 hubs (backend enforces `MAX_HUB_MEMBERSHIPS = 20`).
- Private hubs cannot be joined without being added by the creator (backend blocks it).

---

### 2.4 Hub Settings (Creator Only)

Accessible via the **gear icon** in the cover. Only the hub creator sees this button.

**What the creator can do:**

| Action | How |
|---|---|
| Change cover image | Tap the cover area → image picker opens → select photo |
| Edit hub name | Text field pre-filled with current name |
| Edit description | Multiline text field |
| Toggle Public/Private | Switch — public hubs are visible and joinable by anyone |
| Remove a member | Members list → tap "Remove" next to any non-creator member |
| Delete the hub | Tap "Delete Hub" → confirmation dialog → hub + all posts deleted |

**API endpoints used:**

```
PATCH  /api/hubs/<id>/settings/          — update name, description, is_public, cover_image
DELETE /api/hubs/<id>/delete/            — delete hub (creator only)
GET    /api/hubs/<id>/members/           — list all members with roles
DELETE /api/hubs/<id>/members/<user_id>/ — remove a specific member
```

**Access rules (enforced server-side):**
- All settings endpoints return `403 Forbidden` if the caller is not the hub creator.
- Creator cannot remove themselves.
- Deleting a hub cascades to all posts, comments, likes, challenges, memberships.

---

### 2.5 How to Test Hub Management

**Test: Edit hub name and description**
1. Create a hub (Hubs tab → ➕ button).
2. Open the hub → tap the gear icon (top-right of cover).
3. Change the name and description → tap **Save**.
4. Navigate back — the hub detail should show updated name.

**Test: Change cover image**
1. In Hub Settings → tap the cover area.
2. Pick an image from gallery.
3. Tap Save → backend uploads via PATCH with `cover_image` multipart field.

**Test: Public → Private toggle**
1. In Hub Settings → toggle the switch off.
2. Save → the hub detail should now show "Private" badge.
3. Log in as a different user who has not joined → search for the hub → they should not be able to join.

**Test: Remove a member**
1. Have a second account join the hub.
2. As the creator, go to Hub Settings → scroll to Members.
3. Tap "Remove" next to the member → confirm dialog → member disappears from list.
4. Backend: `members_count` decremented, membership row deleted.

**Test: Delete hub**
1. In Hub Settings → scroll to "Danger Zone" → tap "Delete Hub".
2. Confirm dialog → hub is deleted → app navigates back to the Hubs list.
3. Verify the hub no longer appears in Explore or My Hubs.

**API curl test:**
```bash
# Patch hub settings
curl -X PATCH -H "Authorization: Bearer $TOKEN" \
  -F "name=New Name" -F "description=Updated" -F "is_public=true" \
  $BASE/api/hubs/1/settings/

# List members
curl -H "Authorization: Bearer $TOKEN" $BASE/api/hubs/1/members/

# Remove a member
curl -X DELETE -H "Authorization: Bearer $TOKEN" $BASE/api/hubs/1/members/5/

# Delete hub
curl -X DELETE -H "Authorization: Bearer $TOKEN" $BASE/api/hubs/1/delete/
```

---

## Part 3 — Phase 11 Design: Challenges

### 3.1 What Exists Already

The data model and basic API are already in place from earlier phases:

**Backend models:**
- `Challenge` — belongs to a Hub, has `title`, `description`, `target_count`, `is_main`, `ends_at`, `created_by`.
- `ChallengeProgress` — tracks `current_count` per user per challenge (increments toward `target_count`).

**Backend API (already working):**
```
GET    /api/hubs/<id>/challenges/        — list challenges in a hub
POST   /api/hubs/<id>/challenges/        — create a challenge (members only; is_main = admin only)
POST   /api/challenges/<id>/progress/   — increment user's progress count by 1
```

**Android (already working):**
- `Challenge.kt` data model.
- `HubChallengeAdapter` — renders challenge cards in Hub Detail's Challenges tab.
- `HubDetailViewModel.loadChallenges()` — fetches and displays them.

**What is missing** is the full interactive challenge experience: creating challenges from the app, tracking progress visually, and a leaderboard.

---

### 3.2 Phase 11 Goals

Phase 11 adds the complete challenge flow:

1. **Create a challenge** — hub members can create challenges from inside the hub.
2. **Progress tracking** — users can log progress (tap "+1") with a visual progress bar.
3. **Challenge detail screen** — shows the description, progress bar, leaderboard of top participants, and end date.
4. **Leaderboard** — ranked list of members by `current_count` for a given challenge.
5. **"Main Challenge" card on Hub Detail header** — a pinned progress card visible before the tabs.

---

### 3.3 What Needs to Be Built

#### Backend additions

| What | How |
|---|---|
| `ChallengeLeaderboardView` | GET `/api/challenges/<id>/leaderboard/` — returns top 20 participants ordered by `current_count` desc, with username and avatar |
| `ChallengeDetailView` | GET `/api/challenges/<id>/` — returns a single challenge with the caller's own progress embedded |
| `ChallengeDeleteView` | DELETE `/api/challenges/<id>/` — only creator or hub admin |
| Enrich `ChallengeProgressView` | Already exists; just needs to return richer data (leaderboard rank) |

No new database migrations needed — the models already cover everything.

#### Android data layer

| What | How |
|---|---|
| `ChallengeProgress.kt` model | `{ current_count, target_count, completed, rank }` |
| `LeaderboardEntry.kt` model | `{ user_id, username, avatar_url, current_count }` |
| `HubApiService` additions | GET challenge detail, GET leaderboard, POST progress, DELETE challenge |
| `HubRepository` additions | `getChallengeDetail()`, `getLeaderboard()`, `logProgress()`, `deleteChallenge()` |

#### Android UI — new screens

**A. Create Challenge bottom sheet (or dialog fragment)**
- Launched from Hub Detail Challenges tab with a "+" FAB (visible to hub members).
- Fields: Title (required), Description, Target count (number picker), End date (optional date picker), "Set as main challenge" toggle (admin only).
- On submit: POST `/api/hubs/<id>/challenges/`.

**B. Challenge Detail Fragment**
- Navigated to by tapping a challenge card in the Challenges tab.
- Layout:
  - Header: title (bold), hub name chip, end date.
  - Progress section: circular or linear progress bar showing `current_count / target_count`, percentage label, "+1 Progress" button.
  - Leaderboard section: ranked list — avatar + username + count + a medal emoji for top 3.
  - Delete button (visible only to challenge creator or hub admin).

**C. Main Challenge card in Hub Detail**
- A horizontal card pinned just above the tab bar (inside the collapsing section).
- Shows: challenge title, user's personal progress bar (`X / target`), "+1" button.
- Only shown if the hub has a challenge with `is_main = true`.

---

### 3.4 Data Flow

```
User taps "+1 Progress" on a challenge
  ↓
POST /api/challenges/<id>/progress/
  ↓
Backend: ChallengeProgress.current_count += 1 (capped at target_count)
  ↓
Response: { current_count, target_count, completed }
  ↓
Android: update progress bar and count label in real time
  ↓
If completed: show a "🎉 Challenge complete!" toast
```

```
User opens Challenge Detail
  ↓
GET /api/challenges/<id>/          → title, description, target, ends_at, user's own progress
GET /api/challenges/<id>/leaderboard/ → top 20 users by count
  ↓
Render: progress bar + leaderboard list
```

---

### 3.5 Access Rules

| Action | Who can do it |
|---|---|
| View challenges | Any user with hub access (public hub: anyone; private: members) |
| Create challenge | Hub members |
| Set as "main" challenge | Hub admin / creator only |
| Log progress | Hub members |
| Delete challenge | Challenge creator or hub admin |
| View leaderboard | Same as view challenges |

---

### 3.6 Screen Navigation

```
Hub Detail → Challenges tab
  ├── Tap a challenge card → Challenge Detail Fragment
  │     ├── Tap "+1" → progress update (in-place, no navigation)
  │     ├── Leaderboard list → tap user → User Profile Fragment
  │     └── Delete → confirm dialog → pop back to Challenges tab
  └── Tap "+" FAB → Create Challenge bottom sheet
        └── Submit → challenge appears at top of list
```

---

### 3.7 Files to Create / Modify

**Backend (`community/`):**
- `views.py` — add `ChallengeDetailView`, `ChallengeLeaderboardView`, `ChallengeDeleteView`
- `urls.py` — register new endpoints

**Android data layer:**
- `data/model/ChallengeProgress.kt` — new
- `data/model/LeaderboardEntry.kt` — new
- `data/api/HubApiService.kt` — add 3 new methods
- `data/repository/HubRepository.kt` — add 3 new methods

**Android UI:**
- `ui/hubs/ChallengeDetailFragment.kt` — new
- `ui/hubs/ChallengeDetailViewModel.kt` — new
- `ui/hubs/CreateChallengeFragment.kt` — new bottom sheet
- `ui/hubs/LeaderboardAdapter.kt` — new
- `res/layout/fragment_challenge_detail.xml` — new
- `res/layout/bottom_sheet_create_challenge.xml` — new
- `res/layout/item_leaderboard_entry.xml` — new
- `res/navigation/main_nav_graph.xml` — add `challengeDetailFragment`
- `ui/hubs/HubDetailFragment.kt` — add FAB and main challenge card
- `ui/hubs/HubChallengeAdapter.kt` — add tap navigation to detail

---

### 3.8 Implementation Log — what was actually built (2026-06-16)

Phase 11 is now implemented end-to-end. A few details differ slightly from the original design above — noted here so it's easy to track what changed.

**Deviations from the original design:**
- No separate `ChallengeProgress.kt` / standalone progress model was created. Instead, `current_count`, `completed`, and `can_manage` were added directly as optional fields on the existing `Challenge.kt` model — they're only populated by the challenge-detail endpoint, left at defaults (0/false) everywhere else (list views, create response).
- `ChallengeDetailView` and `ChallengeDeleteView` were **merged into one `APIView`** with `get()` and `delete()` methods, both mapped to `GET/DELETE /api/challenges/<id>/` — simpler than two separate view classes hitting the same URL.
- Permission helper `ensure_challenge_admin(user, challenge)` was added — returns true if the user is the challenge creator OR a `HubMembership` with `role=admin` on that hub (the hub creator is auto-admin on hub creation, so creators are covered automatically).
- A `MainChallenge` ("Main Challenge" card on Hub Detail) was treated as in-scope and fully wired, not deferred — it pulls from `Hub.mainChallenge` (already returned by `HubSerializer.main_challenge`, just needed `id` added to `MainChallengeDetailSerializer` so the app can call the progress endpoint).

**Backend — `community/serializers.py`:**
- `MainChallengeDetailSerializer` — added `id` field (was missing; needed so the app can POST progress for the main challenge).
- `ChallengeSerializer` — added `hub_name` and `created_by_username` read-only fields.

**Backend — `community/views.py`:**
- `ensure_challenge_admin(user, challenge)` — new helper function.
- `ChallengeDetailView` — new `APIView`, `get()` returns challenge + `current_count`/`completed`/`can_manage` for the caller, `delete()` enforces `ensure_challenge_admin`.
- `ChallengeLeaderboardView` — new `APIView`, `get()` returns top 20 by `current_count` (only entries with `current_count__gt=0`), with 1-indexed `rank`.
- `ChallengeProgressView` — unchanged, already existed.

**Backend — `community/urls.py`:**
- Added `path("challenges/<int:id>/", ChallengeDetailView.as_view())`
- Added `path("challenges/<int:id>/leaderboard/", ChallengeLeaderboardView.as_view())`

**Android data layer:**
- `data/model/Challenge.kt` — added `hubName`, `createdByUsername`, `currentCount`, `completed`, `canManage` to `Challenge`; added new data classes `LeaderboardEntry`, `ChallengeProgressResponse`, `CreateChallengeRequest` (kept `HubMember` in the same file as before).
- `data/model/Hub.kt` — added `mainChallenge: MainChallenge?` field + new `MainChallenge` data class (`id`, `title`, `targetCount`, `userProgress`).
- `data/api/HubApiService.kt` — added `createChallenge`, `getChallenge`, `deleteChallenge`, `getLeaderboard`, `logChallengeProgress`.
- `data/repository/HubRepository.kt` — added matching repository methods for all of the above.

**Android UI — new files:**
- `ui/hubs/LeaderboardAdapter.kt` + `res/layout/item_leaderboard_entry.xml` — ranked list, 🥇🥈🥉 for top 3, tap → user profile.
- `ui/hubs/ChallengeDetailViewModel.kt` — loads challenge + leaderboard, exposes `logProgress()` and `deleteChallenge()`.
- `ui/hubs/ChallengeDetailFragment.kt` + `res/layout/fragment_challenge_detail.xml` — title/description/end-date card, progress bar + "+1 Progress" button, leaderboard list, "Delete Challenge" button (visible only when `canManage` is true).
- `ui/hubs/CreateChallengeFragment.kt` (BottomSheetDialogFragment) + `res/layout/bottom_sheet_create_challenge.xml` — title/description/target fields, "Set as main challenge" switch (only shown if `canSetMain` passed in, currently wired to `hub.isCreator`). On success, uses `parentFragmentManager.setFragmentResult(RESULT_KEY, ...)` to tell the host to refresh.
- `res/drawable/ic_add.xml` — plus icon for the FAB.

**Android UI — modified files:**
- `ui/hubs/HubChallengeAdapter.kt` — constructor now takes `onChallengeClick: (Challenge) -> Unit`; item layout (`item_hub_challenge_detail.xml`) made clickable with a ripple foreground.
- `ui/hubs/HubDetailViewModel.kt` — added `logMainChallengeProgress()` which posts progress for `hub.mainChallenge.id` and updates the LiveData in place.
- `ui/hubs/HubDetailFragment.kt`:
  - Challenge tap now navigates to `challengeDetailFragment` with a `challengeId` bundle arg.
  - FAB (`fab_create_challenge`) shown only when the Challenges tab is active **and** the user is a hub member (`hub.isMember`); opens `CreateChallengeFragment` as a bottom sheet.
  - Listens for `CreateChallengeFragment.RESULT_KEY` via `childFragmentManager.setFragmentResultListener` and reloads the challenges list on success.
  - Renders the main-challenge card (`card_main_challenge`) when `hub.mainChallenge != null`: progress bar, "X / Y" label, "+1" button that calls `viewModel.logMainChallengeProgress()`. The "+1" button hides itself once `completed` or if the user isn't a member.
- `res/layout/fragment_hub_detail.xml`:
  - Added `card_main_challenge` section inside the scrolling header (below the description).
  - Added `fab_create_challenge` (`FloatingActionButton`, bottom-end, `app:backgroundTint="@color/blue_primary"`), hidden by default.
- `res/navigation/main_nav_graph.xml` — added `challengeDetailFragment` destination.

**Access rules as implemented:**
| Action | Enforcement |
|---|---|
| View challenge / leaderboard | `ensure_hub_access` (same rule as hub posts) |
| Create challenge | Must be a hub member (pre-existing rule, unchanged) |
| Set `is_main` | Must be hub admin (pre-existing rule, unchanged) |
| Log progress | Must be a hub member (pre-existing rule, unchanged) |
| Delete challenge | `ensure_challenge_admin` — creator of the challenge OR hub admin |

**Verified:** `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL (no new warnings). `python manage.py check` → no issues. No new migrations were needed — `Challenge` and `ChallengeProgress` models already existed from an earlier phase.

---

### 3.9 Post-implementation bugfixes (2026-06-16, same day)

Three bugs surfaced during manual testing after the Phase 11 build above. All three are fixed now.

**Bug A — `POST /api/hubs/<id>/challenges/` returned 400 Bad Request**
- Cause: `ChallengeSerializer.Meta.read_only_fields` did not include `"hub"`. The `hub` model field is a required FK (no `null=True`), so DRF demanded it in the request body — but `ChallengeListCreateView.create()` already supplies `hub=hub` explicitly when saving. The client never sends `hub`, so validation failed every time.
- Fix: `backend/community/serializers.py` — added `"hub"` to `read_only_fields` in `ChallengeSerializer`.

**Bug B — Challenges tab always showed "No challenges yet" + a Gson error toast (`Expected BEGIN_ARRAY but was BEGIN_OBJECT`)**
- Cause: `ChallengeListCreateView` uses `pagination_class = DefaultPagination`, so `GET /api/hubs/<id>/challenges/` actually returns a paginated object (`{"count":.., "results": [...]}`), not a raw JSON array. The Android `HubApiService.getHubChallenges()` was declared as `Response<List<Challenge>>`, so Gson failed to parse the object as an array and the repository silently returned an empty list (visible as the "No challenges yet" + a parse-error toast).
- Fix:
  - `data/api/HubApiService.kt` — `getHubChallenges()` now returns `Response<PaginatedResponse<Challenge>>`.
  - `data/repository/HubRepository.kt` — `getHubChallenges()` now reads `r.body()?.results`.
  - (`getLeaderboard()` was not affected — `ChallengeLeaderboardView` is a plain `APIView` with no pagination, so it already returns a raw list correctly.)

**Bug C — "Create Challenge" bottom sheet had no reachable submit button**
- Cause: `bottom_sheet_create_challenge.xml` had no scroll container. When the keyboard opened (e.g. typing into "Target count"), the sheet's wrap_content layout got compressed and the "Create Challenge" button at the bottom was pushed off-screen with no way to scroll to it. The button existed in code but was practically unreachable.
- Fix:
  - `res/layout/bottom_sheet_create_challenge.xml` — wrapped all content in a `NestedScrollView`.
  - `ui/hubs/CreateChallengeFragment.kt` — added `onStart()` override that forces the sheet to `BottomSheetBehavior.STATE_EXPANDED` with `skipCollapsed = true`, and sets `SOFT_INPUT_ADJUST_RESIZE` on the dialog window so the keyboard resizes the sheet instead of just covering it.

**Verified:** `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL (one pre-existing-style deprecation warning on `SOFT_INPUT_ADJUST_RESIZE`, no functional issue). `python manage.py check` → no issues.

---

### 3.10 Hub Detail header redesign — avatar removed, Join button clipping fixed (2026-06-16)

User feedback: the Join/Joined button was visually cut off at the top, and the circular hub-initial avatar wasn't wanted.

**Cause of the clipping:** the original header row had a 64dp avatar `FrameLayout` with `layout_marginTop="-32dp"` (deliberately overlapping the cover image above it, Reddit-style) sitting next to the Join/Leave button, which itself had `layout_marginTop="-8dp"`. The negative margins pulled both views up and out of the row's natural bounds, clipping the button against the LinearLayout's top edge.

**Fix — `res/layout/fragment_hub_detail.xml`:**
- Removed the avatar `FrameLayout` (`hub_avatar_bg` background + `tv_hub_initial` text) entirely.
- Header row is now just: `tv_hub_name` (`layout_weight="1"`) + `btn_join_leave`, both `gravity="center_vertical"`, no negative margins.
- Added `paddingTop="14dp"` to the hub-info container to keep normal spacing under the cover now that there's no overlapping avatar.

**Fix — `ui/hubs/HubDetailFragment.kt`:**
- Removed `tvHubInitial` field, its `findViewById`, and the line setting its text from `hub.name.firstOrNull()`.

---

## Part 4 — HubChallenge Spec Adoption: Foundation Round (2026-06-16)

The user supplied `HubChallenge_Spec_v1.pdf`, a full product spec for a 3-model progress system (Stage/Count/Streak) plus a 2-layer verification system (peer validation + admin review) and a 15-template hub creation wizard. Given the size, scope for **this round** was scoped down to:

> **Foundation**: data model for all 3 progress types, progress-logging APIs for each, hub creation flow updated so admins pick a model, model-aware Android UI. **No verification layer (Layer 1/3), no templates** — those are explicitly deferred to later rounds.

The existing simple Challenge data (`target_count` + flat `ChallengeProgress`) was **wiped**, not migrated (user's choice — it was only dev/test data).

### 4.1 Backend — New Data Model

`community/models.py` — `Challenge` lost `target_count`, gained `progress_model` (`stage`/`count`/`streak`). Three new config + progress table pairs were added, one per model:

| Model A — Stage | Model B — Count | Model C — Streak |
|---|---|---|
| `ChallengeStage` (order_index, title, description, proof_type, proof_prompt, is_milestone) | `ChallengeCountConfig` (target_count, unit_label, entry_increment, require_proof_per_entry) | `ChallengeStreakConfig` (target_days, frequency, grace_days, require_proof) |
| `UserStageProgress` (status: not_started/in_progress/completed, proof_post FK, completed_at) | `UserCountProgress` (current_count, is_complete, completed_at) | `UserStreakProgress` (current_streak, longest_streak, total_checkins, checkin_calendar JSON, last_checkin_date, is_complete) |

No `hub_enrollment_id` concept from the spec was carried over — hub membership (`HubMembership`) already serves as the enrollment record, so progress rows just FK directly to `(user, challenge)`.

**Deliberate scope cut vs. the spec:** stage completion goes straight to `completed` status (no `pending_review` admin gate) since Layer 3 review doesn't exist yet this round. The `pending_review`/`rejected` enum values from the spec were dropped from `UserStageProgress.STATUS_CHOICES` entirely (only `not_started`/`in_progress`/`completed` exist) — they'll be reintroduced when Layer 3 is built, since adding enum values later is non-breaking.

Migration: `community/migrations/0010_challengecountconfig_challengestage_and_more.py` — generated cleanly with no interactive prompts (old `Challenge`/`ChallengeProgress` rows were deleted via shell *before* editing models.py, so Django never had to resolve a NOT-NULL-without-default conflict).

### 4.2 Backend — Serializers (`community/serializers.py`)

- New module-level helper `get_challenge_progress(challenge, user) -> (percent: int, summary: str)` — computes a model-agnostic percent/summary pair regardless of which of the 3 models the challenge uses. Used by `ChallengeSerializer`, `MainChallengeDetailSerializer`, and indirectly by the list view.
- `ChallengeSerializer` — `target_count` field replaced with `progress_model`, `summary`, `percent_complete` (both computed via the helper above).
- `MainChallengeDetailSerializer` (nested inside `HubSerializer.main_challenge`) — reworked the same way; also gained an `id` field it was previously missing (needed so the app can call progress endpoints for the hub's main challenge).
- Formatting note: `get_challenge_progress`'s count-model branch explicitly casts `Decimal` values to `float()` before using Python's `:g` format spec — `Decimal` and `float` don't behave the same way under `:g` (a `Decimal('3.00')` formats as `"3.00"`, not `"3"`), so without the cast the summary read "3.00/10 books" instead of "3/10 books". Caught via direct testing, fixed before merging.

### 4.3 Backend — Views (`community/views.py`)

- `ChallengeListCreateView.create()` — now branches on `progress_model` and validates the model-specific payload (`stages: [...]` / `count_config: {...}` / `streak_config: {...}`) **before** creating any database row. This was a deliberate fix for a transaction bug found during implementation: the first draft created the `Challenge` row, then validated the nested config and returned an error `Response` on failure — but inside a `@transaction.atomic` block, an early `return` does **not** roll back (only an unhandled exception does), so a bad request would have silently committed an orphan `Challenge` with no stages/config attached. Fixed by validating everything into local variables first, then doing all the `.create()` calls together inside a single `with transaction.atomic():` block.
- `ChallengeDetailView.get()` — now branches on `progress_model` and attaches `stages` (with the current user's per-stage status) or `count_config`+`my_progress` or `streak_config`+`my_progress` to the response.
- `ChallengeLeaderboardView` — generalized to rank by whatever each model considers "score": completed-stage count, `current_count`, or `total_checkins`. Response field renamed `current_count` → `score` (now generic).
- Three new progress-action views, one per model:
  - `StageCompleteView` — `POST /api/challenges/<id>/stages/<stage_id>/complete/`. Enforces sequential completion (rejects with 400 if an earlier stage isn't completed yet, using a single Django `.exclude(progress__user=..., progress__status=...)` join — verified this produces the correct "stages not yet completed by this user" set, not an over-broad exclude).
  - `CountLogView` — `POST /api/challenges/<id>/count/log/`. Optional `{"amount": N}` body; defaults to the config's `entry_increment`. Allows overshooting the target (e.g. 11/10 books) rather than capping, matching how real progress logging apps behave.
  - `StreakCheckinView` — `POST /api/challenges/<id>/streak/checkin/`. Rejects a second check-in on the same day (400). Breaks the streak (resets to 1, not 0, since the check-in itself counts) if the gap since the last check-in exceeds `1 + grace_days`.
- Old `ChallengeProgressView` (the simple "+1" endpoint from the pre-spec implementation) was deleted entirely, along with its URL.

### 4.4 Backend — URLs (`community/urls.py`)

Added: `challenges/<id>/stages/<stage_id>/complete/`, `challenges/<id>/count/log/`, `challenges/<id>/streak/checkin/`. Removed: `challenges/<id>/progress/` (obsolete).

### 4.5 Backend Verification

1. `python manage.py check` → no issues.
2. Direct-ORM simulation of all 3 models via `manage.py shell` (create challenge + config, set progress, call `get_challenge_progress()`) → correct percent/summary for all 3, including the Decimal formatting fix.
3. **Full HTTP-level smoke test** using Django's `Client` with a real JWT (`RefreshToken.for_user(user).access_token`, since this API is JWT-only — `DEFAULT_AUTHENTICATION_CLASSES` has no `SessionAuthentication`, so `client.force_login()` alone doesn't work for these endpoints). Exercised, against the real URL-routed views:
   - Create stage/count/streak challenge → 201 for all 3.
   - Get detail for all 3 → correct nested `stages`/`count_config`/`streak_config` + `my_progress`.
   - Complete a stage → 200, `completed_stages: 1`.
   - Log a count entry (`amount: 3`) → 200, `current_count: 3.0`.
   - Streak check-in → 200, `current_streak: 1`, `checkin_calendar: ["2026-06-16"]`.
   - Leaderboard for all 3 models → correctly ranked.
   - Invalid `progress_model` → 400.
   - Stage challenge with an empty `stages: []` list → 400, **and confirmed `Challenge.objects.count()` was unchanged before/after** (the orphan-row transaction fix holds).
   - Confirmed `Decimal` fields (`target_count`, `current_count`) serialize as JSON **numbers** (`10.0`, `3.0`), not strings — important since Android's Gson expects `Double`, not `String`, for `CountConfig.targetCount`.
   - All test users/hubs/challenges cleaned up afterward; verified zero leftover rows.

### 4.6 Android — Data Layer

- `data/model/Challenge.kt` — full rewrite. `Challenge` now carries `progressModel`, `summary`, `percentComplete`, plus nullable detail-only fields: `stages: List<ChallengeStageStatus>?`, `countConfig: CountConfig?`, `streakConfig: StreakConfig?`, `myProgress: MyProgress?`. New types: `ChallengeStageStatus`, `CountConfig`, `StreakConfig`, `MyProgress` (a flexible holder — only the fields relevant to the challenge's actual model are populated), `StageInput`/`CountConfigInput`/`StreakConfigInput` (request-side), `StageCompleteResponse`/`CountLogResponse`/`StreakCheckinResponse`. Added `object ProgressModel` and `object StageStatus` as string-constant namespaces (avoids magic strings scattered across the UI layer).
- `data/model/Hub.kt` — `MainChallenge` reworked to carry `progressModel`/`summary`/`percentComplete` instead of the old `targetCount`/`userProgress` pair, matching the new `MainChallengeDetailSerializer` shape.
- `data/api/HubApiService.kt` / `data/repository/HubRepository.kt` — replaced `logChallengeProgress()` with three model-specific calls: `completeStage()`, `logCountEntry()` (optional amount), `streakCheckin()`. Added `createChallenge` request now carries `progressModel` + the one relevant nested config.

### 4.7 Android — UI

**Create Challenge bottom sheet** (`CreateChallengeFragment.kt` + `bottom_sheet_create_challenge.xml`, full rewrite):
- 3 selectable "chips" (Stages / Count / Streak) toggle which config section is visible, styled via `btn_primary` (selected) vs `step_chip_bg` (unselected) background swap in code — no custom `ChipGroup` widget needed.
- Stage config: dynamic "+ Add Stage" rows (`item_stage_input.xml`) — each row has a title field + a proof-type spinner (Any/Photo/Video/Text/Number) + a remove (✕) button. Starts with one row pre-added for convenience. Description/proof-prompt/is-milestone per stage were **cut from the create form** for this round (backend supports them; the form doesn't expose them yet) — a deliberate scope trim to keep the form shippable.
- Count config: target count, unit label, require-proof switch.
- Streak config: target days, frequency spinner (Daily/Weekly), grace days, require-proof switch.
- Validation happens per-model at submit time before building the `CreateChallengeRequest`.

**Challenge Detail screen** (`ChallengeDetailFragment.kt`/`ChallengeDetailViewModel.kt` + `fragment_challenge_detail.xml`, full rewrite):
- Three mutually-exclusive sections (`section_count`/`section_stage`/`section_streak`), only one visible depending on `challenge.progressModel`:
  - **Count**: progress bar, "X/Y unit" label, optional custom-amount field, "Log Entry (+N)" button (label shows the config's `entry_increment` if it isn't 1).
  - **Stage**: `StageProgressAdapter` (new) renders the ordered stage list — completed stages show a ✓, the first not-yet-completed stage is the only one with an enabled "Mark Complete" button (enforces the same sequential rule the backend enforces), later stages show "Locked" and are dimmed.
  - **Streak**: current/longest streak badges, progress bar, a **4×7 calendar heatmap** (`CalendarHeatmapAdapter`, new — `RecyclerView` + `GridLayoutManager(7)`, last 28 days, green = checked in) , "Check In Today" button that disables itself once `myProgress.lastCheckinDate` equals today (`java.time.LocalDate` — safe to use directly, no desugaring needed, since `minSdk = 27` already covers the java.time APIs added in API 26).
- Leaderboard section unchanged in shape, just reads `LeaderboardEntry.score` (generic) instead of `currentCount`.
- Delete button unchanged (still gated on `canManage`).

**Hub Detail main-challenge card** (`HubDetailFragment.kt`/`HubDetailViewModel.kt`):
- Now reads `hub.mainChallenge.percentComplete`/`summary` directly rather than computing percent client-side from `targetCount`/`userProgress` (which no longer exist).
- The card's action button is model-aware: for a **count** main challenge it stays an inline "+1" button (calls `logMainChallengeProgress()`, which now hits `logCountEntry()`); for **stage** or **streak** main challenges the button becomes "View" and navigates to the full `ChallengeDetailFragment` instead — a deliberate scope cut, since rendering a stage list or a calendar heatmap inline in the hub header card isn't practical, and the hub's `MainChallengeDetailSerializer` doesn't (yet) carry enough state (e.g. "checked in today?") to drive a one-tap streak action from the card alone.

**Other adapter updates:**
- `HubChallengeAdapter.kt` (the challenge list inside the hub's Challenges tab) — now shows `"<ModelLabel>: <summary> (<percent>%)"` instead of `"Target: <target_count>"`.
- `LeaderboardAdapter.kt` — reads `entry.score` (generic `Double`), formats as an integer when it's a whole number (covers stage/streak which are always whole numbers) or with decimals otherwise (covers fractional count entries like 2.5 km).

### 4.8 Android Verification

`./gradlew :app:compileDebugKotlin` → **BUILD SUCCESSFUL**. Only pre-existing warnings plus one new (harmless, expected) deprecation warning on `WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE` in `CreateChallengeFragment.kt` (still the correct API for dialog windows; no non-deprecated replacement exists for this case).

### 4.9 What's Explicitly Deferred (not part of this round)

- **Layer 1 — Peer micro-validation** ("I believe this" reactions, weighted validator scoring, Trusted badge).
- **Layer 3 — Admin final review** (completion requests, auto-generated review timeline, approve/reject/request-more-proof, auto-generated announcement post, 5-day/10-day reminder cron).
- **Template system** (15 official templates, hub-creation wizard with template picker, community template submissions).
- Per-stage `description`/`proof_prompt`/`is_milestone` input in the create-challenge form (backend already supports them; just not exposed in the Android form yet).
- A "current rank" indicator for the logged-in user when they're outside the leaderboard's top 20.

These map directly onto Section 2 ("Verification System") and the Template System portion of Section 1 in `HubChallenge_Spec_v1.pdf` — next rounds should pick up from there.

**Verified:** `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL, no new warnings.

### 4.10 Post-round UI fixes (2026-06-16, same day)

User feedback after testing: text fields looked pale/washed-out, and the "Create Challenge" button was invisible against the bottom sheet's background.

**Cause 1 — pale fields:** `themes.xml` already defines a proper app-wide field style, `HubRiseTextInput` (intense `blue_primary` border, visible `text_secondary` hint color), wired up via `textInputStyle` in `Base.Theme.HubRise` so it applies automatically to any `TextInputLayout`. But every `TextInputLayout` built across the last two rounds (`bottom_sheet_create_challenge.xml`, `item_stage_input.xml`, `fragment_edit_profile.xml`, `fragment_hub_settings.xml`, `fragment_challenge_detail.xml`) explicitly set `style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"` — an explicit `style=` always wins over the theme default, so all of them were silently bypassing `HubRiseTextInput` and falling back to the much paler generic Material style.
- Fix: replaced all 12 occurrences with `style="@style/HubRiseTextInput"` across those 5 files.

**Cause 2 — invisible Create Challenge button:** `btn_create` in `bottom_sheet_create_challenge.xml` is the only `<Button>` in the entire app that lives inside a `BottomSheetDialogFragment`. `BottomSheetDialogFragment` doesn't resolve theme color attributes (`?attr/colorPrimary` etc.) the same way a regular fragment hosted by the main Activity does, so the auto-inflated `MaterialButton`'s attribute-driven background resolution went wrong and it rendered blending into the sheet's white background instead of the intended blue `btn_primary` drawable.
- Fix: replaced `android:background="@drawable/btn_primary"` with `app:backgroundTint="@color/blue_primary"` + `app:cornerRadius="14dp"` — this hardcodes the actual color resource directly into `MaterialButton`'s own background-tinting mechanism, bypassing theme-attribute resolution entirely. Added `xmlns:app` to the layout's root (was missing, needed for `app:backgroundTint`).

**Verified:** `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL, no new warnings.

---

## Part 5 — HubChallenge Spec Adoption: Round 2, Layer 1 — Peer Micro-Validation (2026-06-16)

Scope for this round (user-confirmed): **Layer 1 only** ("I believe this" peer validation). Layer 3 (admin review) and the template system remain deferred.

**Key design decision, clarified by the user mid-implementation:** validation should apply **only to posts related to a challenge**, not to general/regular hub posts. Our app's progress actions (stage complete, count log, streak check-in) didn't previously create any `Post` at all — they were pure data mutations on the progress tables. To give Layer 1 something concrete to attach to (and to match the spec's own model, where "each log entry is a post"), this round made every progress action **auto-create a `Post`** in the hub feed, tagged with the challenge it belongs to. Validation is then restricted to those auto-generated posts.

### 5.1 Backend — Models

- `Hub` — added `peer_validation_threshold` (Decimal, default `3.0`) — the weighted score a post needs to reach to become Trusted.
- `Post` — added 3 new `post_type` values (`stage_proof`, `count_entry`, `streak_checkin`, collected in `Post.CHALLENGE_POST_TYPES`), a nullable `challenge` FK, a nullable `linked_stage` FK (stage_proof only), and the Layer 1 fields: `weighted_validation_score`, `is_trusted`, `trusted_at`.
- New `PostValidation` model: `post` FK, `validator` FK, `weight` (Decimal 0.5/1.0/1.5), unique constraint on `(post, validator)`.
- Migration `0011_hub_peer_validation_threshold_post_challenge_and_more.py` — purely additive (new nullable fields / fields with defaults / new table), no data wipe needed, generated with no interactive prompts.

### 5.2 Backend — Progress actions now create posts

- `StageCompleteView` — on first completion of a stage, creates a `stage_proof` post (`"✅ Completed stage N: Title — Challenge"`). Re-completing an already-completed stage does **not** create a duplicate (guarded by the existing `if progress.status != COMPLETED` check).
- `CountLogView` — **every** log entry creates a `count_entry` post (repeatable, matching the spec: "each log entry is a post").
- `StreakCheckinView` — **every** check-in creates a `streak_checkin` post (repeatable, one per day).
- All three responses now include `post_id` so the Android app could (in a future round) deep-link to the created post; not currently used for navigation, just available.

### 5.3 Backend — Validation views (`community/views.py`)

- `has_completed_challenge(user, challenge)` — model-aware helper: checks `UserCountProgress.is_complete` / `UserStreakProgress.is_complete` / (stages completed == total stages) depending on `challenge.progress_model`. Used for the 1.5x weight tier. Important nuance verified by testing: this checks completion of **that same challenge** the post belongs to, not "any challenge in the hub" — matching the spec's "they can recognize genuine progress" (on *that* challenge specifically).
- `PostValidateView`:
  - `POST /api/posts/<id>/validate/` — rejects with 400 if the post isn't challenge-related, 403 if it's the user's own post or they're not a hub member, 409 if already validated. Computes weight: membership `< 7 days` old → 0.5x; `has_completed_challenge` → 1.5x; else → 1.0x. Recalculates `weighted_validation_score` (DB `Sum` aggregate over all validations), flips `is_trusted`/`trusted_at` once the hub's `peer_validation_threshold` is reached.
  - `DELETE /api/posts/<id>/validate/` — removes the validator's vote, recalculates the score, un-sets `is_trusted` if the score drops back below threshold (but `trusted_at` is left untouched once set, preserving "first crossed" semantics per the spec).
- `PostValidationsListView` — `GET /api/posts/<id>/validations/` — returns `{total_score, is_trusted, validators: [{user_id, username, avatar_url, weight}]}`. Built but **no Android UI consumes it yet** this round (scope cut — see 5.6).
- `PostSerializer` — added `challenge`, `challenge_title`, `is_trusted`, `validations_count`, `validated_by_me`.

### 5.4 Backend Verification

`python manage.py check` → clean. Full HTTP smoke test via Django's `Client` + JWT (same approach as Round 1):
1. Logged a count entry → confirmed it auto-created a `count_entry` post.
2. Self-validation → 403. Validating a regular (non-challenge) post → 400.
3. New member (`joined_at` < 7 days) validates → weight 0.5 ✓.
4. **First pass caught a test-setup mistake, not a code bug**: a validator who completed a *different* challenge in the same hub got weight 1.0, not 1.5 — re-read the code and confirmed this is correct, since `has_completed_challenge` checks completion of the post's *own* challenge, not any challenge in the hub. Re-ran with the validator completing the *same* challenge as the post → weight 1.5 confirmed ✓.
5. Stacked 4 validators (weights 1.5 + 1.0 + 1.0 = 3.5) → confirmed `is_trusted` flips to `true` exactly when the running total crosses the hub's `3.0` threshold.
6. `DELETE` un-validate → score recalculated correctly.
7. `GET /validations/` → correct validator list + total score.
8. Confirmed via `GET /api/posts/feed/` that `is_trusted`/`validations_count`/`validated_by_me` serialize correctly on the real feed endpoint.
9. Verified no leftover test data (the 6 posts found in the DB afterward were the user's own real posts from manual app testing, not test artifacts).

### 5.5 Android

- `Post.kt` — added `challenge`, `challengeTitle`, `isTrusted`, `validationsCount`, `validatedByMe`; new `PostType` object (string constants + `CHALLENGE_TYPES` set); new `ValidateResponse` model.
- `FeedApiService.kt` / `PostRepository.kt` — added `validatePost()`/`unvalidatePost()` (`POST`/`DELETE /api/posts/<id>/validate/`).
- `PostAdapter.kt` — new `currentUserId` constructor param (to disable validation on the viewer's own posts) and `onValidateClick` callback. The validate row (`row_validate` in `item_post.xml`) is hidden entirely for non-challenge posts; for challenge posts it shows the shield-check icon (`ic_shield_check.xml`, new) tinted gray (not yet validated by me) / blue (validated by me) / green with "Trusted" label (post crossed the threshold), with a validator count next to it otherwise.
- `item_post.xml` — added the `row_validate` segment to the action row, right-aligned via a weight-1 spacer `View`.
- Wired into **both** places `PostAdapter` is used:
  - `HomeViewModel`/`HomeFragment` — `toggleValidate()` added alongside the existing `toggleLike()`, same optimistic-update pattern.
  - `HubDetailViewModel`/`HubDetailFragment` — added a `PostRepository` instance alongside the existing `HubRepository` (validation lives in `PostRepository`, everything else hub-related lives in `HubRepository`), same `toggleValidate()` pattern.
  - Both fragments now read `currentUserId` from `UserPreferences` (same pattern already used in `CommentsBottomSheetFragment`) and pass it into `PostAdapter`.
- Added `HubDetailFragment.onResume()` → calls `viewModel.loadPosts(hubId)`. Without this, returning to the hub after completing a stage / logging a count entry / checking in on the separate `ChallengeDetailFragment` screen wouldn't show the newly auto-created post until the hub was re-opened from scratch. (Double-load on first open is an accepted pre-existing pattern in this codebase — `NotificationsFragment` already does the same thing.)

**Verified:** `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL, no new warnings.

### 5.6 What's Explicitly Deferred (not part of this round)

- **Layer 3** — admin completion review, timeline, announcement posts (per the round-2 scoping decision).
- **Template system.**
- A dedicated "who validated this post" screen/bottom sheet — the `GET /api/posts/<id>/validations/` endpoint exists and is fully tested, but no Android screen calls it yet. Currently the validator count is the only UI signal; tapping it doesn't drill into the list.
- `linked_stage` proof attachment (photo/video evidence for `stage_proof` posts) — the FK exists on `Post`/`UserStageProgress`, but no upload UI was built; auto-generated posts currently have text content only.
- A real-time/poll-based score update if someone else validates a post while it's on screen — relies on the existing pull-to-refresh / re-navigation patterns already in the app, no WebSocket/polling added.

---

## Part 6 — Progress-Only-Via-Posts: Removing Direct Progress Buttons (2026-06-16)

User feedback: when a user wants to increase a count entry, complete a stage, or check in on a streak, there should be **no direct button** that does it instantly. Instead, the only way to make progress is to **create a post** and attach it to a specific challenge — submitting that post *is* the progress action.

This round removed every direct "+1" / "Mark Complete" / "Check In Today" button in the app and moved all of that logic into the existing Create Post screen.

### 6.1 Backend

- `CreatePostView` (`POST /api/posts/`) now branches on an optional `challenge` field in the request body:
  - No `challenge` → behaves exactly as before (regular post via `CreatePostSerializer`).
  - `challenge` present → routes to one of three new private helper methods (`_create_stage_post`, `_create_count_post`, `_create_streak_post`) that perform the same progress mutation the old endpoints did (sequential stage enforcement, count increment, streak check-in with grace-day logic), then create the `Post` tagged with that challenge. The post's `content` is whatever the user typed; if left blank, the same auto-generated description used in Round 5 is used as a fallback.
  - `stage` (required for stage challenges) and `amount` (optional for count challenges, defaults to `entry_increment`) are read directly from the request body.
- **Deleted entirely**, including their URL routes: `StageCompleteView`, `CountLogView`, `StreakCheckinView`. These were the old direct-action endpoints; with progress only happening through `POST /api/posts/`, keeping them around would have been dead, unreachable-by-the-app code (and a second, inconsistent way to mutate progress).
- No model/migration changes needed — same `Challenge`/`UserStageProgress`/`UserCountProgress`/`UserStreakProgress`/`Post` tables from Rounds 1 and 5.

**Verified via HTTP smoke test:**
- Confirmed the 3 deleted endpoints now return `404`.
- Regular (non-challenge) post creation still works unaffected.
- Created stage/count/streak posts through `/api/posts/` for all 3 models — content, auto-generated fallback text, sequential stage enforcement, and duplicate-checkin/repeat-stage rejection (`400`) all verified.
- Confirmed `GET /api/challenges/<id>/` and `GET /api/hubs/<id>/` (`main_challenge`) reflect the updated progress immediately after a progress post is created.

### 6.2 Android — Create Post screen gains a challenge picker

- `CreatePostRequest` — added optional `challenge`, `stage`, `amount` fields.
- `CreatePostViewModel`:
  - `availableChallenges` — loaded automatically whenever a hub is selected (cleared when hub changes or "Personal Post" is chosen).
  - `selectedChallenge` / `selectChallenge()` / `clearChallenge()`.
  - `currentStage` — for stage-based challenges, automatically resolved as "the first stage with status != completed" by fetching the full challenge detail when a stage challenge is selected. The user never manually picks a stage — there's nothing to pick since the backend enforces strict sequential order anyway.
  - `preselect(hubId, challengeId)` — new, used when the screen is opened from a Challenge Detail / Hub Detail "+ Add Progress Post" entry point instead of from scratch.
  - `createPost(content, amount)` — content is allowed to be blank when a challenge is selected (backend auto-generates a description); still required for regular posts.
- New bottom sheet `bottom_sheet_challenge_picker.xml` + `item_challenge_picker_row.xml`, mirroring the existing hub-picker pattern exactly (same `BottomSheetDialog` + manual row-inflation approach, not a RecyclerView, for consistency with `showHubPickerBottomSheet()`).
- `fragment_create_post.xml` — added a "Challenge:" selector row (visible only when the selected hub actually has challenges) and an "Amount (optional)" field (visible only when the selected challenge is count-based).
- `CreatePostFragment`:
  - Reads `hubId`/`challengeId` from arguments for pre-selection; if `hubId` was passed in, treats this as "launched from elsewhere" and does a plain `popBackStack()` on success (returns to the caller) instead of the default `popBackStack(R.id.homeFragment, false)`.
  - Post button enablement and the `et_content` hint text both now branch on whether a challenge is selected.

### 6.3 Android — removing the direct-action entry points

- **`ChallengeDetailFragment`** — the count section's "Log Entry" button, the streak section's "Check In Today" button, and each stage row's action button (`StageProgressAdapter`) no longer call a progress-mutation method directly. All three now call a single `navigateToCreatePost()` that opens `CreatePostFragment` with `hubId`/`challengeId` pre-filled. Button labels changed to "+ Add Progress Post" everywhere (the streak button still shows "Checked in today ✓" and disables itself client-side if `lastCheckinDate` is today, to avoid a pointless round trip that would just 400 on submit). Added `onResume()` to reload the challenge + leaderboard, so progress made via the post screen shows up immediately on return.
- **`HubDetailFragment`** — the main-challenge card's action button used to be a direct "+1" for count challenges (calling `HubDetailViewModel.logMainChallengeProgress()`, which hit the now-deleted `count/log/` endpoint) and only navigated to `ChallengeDetailFragment` for stage/streak. Unified: **all three models now just navigate to `ChallengeDetailFragment`** — there's no special-cased inline action left on this card.
- **Removed as dead code**, since nothing calls them anymore: `HubDetailViewModel.logMainChallengeProgress()` (+ its private `formatNumber()` helper), `ChallengeDetailViewModel.completeStage()`/`logCountEntry()`/`checkinStreak()`, `HubRepository.completeStage()`/`logCountEntry()`/`streakCheckin()`, the three corresponding `HubApiService` endpoint declarations, and the now-unreferenced `StageCompleteResponse`/`CountLogResponse`/`StreakCheckinResponse` data classes.

**Verified:** `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL, no new warnings. `python manage.py check` → no issues.

### 6.4 Net effect

There is now exactly **one** way to make challenge progress anywhere in the app: open the post composer (via the bottom-nav "+", or via a "+ Add Progress Post" entry point that pre-fills the hub/challenge for you), optionally attach a challenge, write a note, and submit. The submission itself is the progress action — there is no separate confirmation step and no standalone increment/checkin/complete button left anywhere.
