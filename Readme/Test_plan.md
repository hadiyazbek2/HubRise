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

---

## Part 7 — HubChallenge Spec Adoption: Round 3, Layer 3 — Admin Final Review (2026-06-16)

User-confirmed scope: the **core loop only** — completion request → admin approve/reject → announcement post. The auto-generated review timeline (gap markers, stage-event interleaving), the "request more proof" soft-reject action, and the 5-day/10-day reminder cron job were explicitly left out of this round.

### 7.1 Backend — Data Model

- New model `CompletionRequest` (`community/models.py`): `user`, `challenge` FKs, `status` (pending/approved/rejected), `member_note`, `submitted_at`, `reviewed_at`, `reviewed_by`, `admin_note`, `announcement_post` (FK to the `Post` created on approval). A partial unique constraint (`condition=Q(status="pending")`) prevents two pending requests for the same `(user, challenge)` pair at the DB level — confirmed SQLite applies this migration with no prompt, since SQLite supports partial indexes.
- `Notification` (`accounts/models.py`) gained 3 new types — `completion_submitted`, `completion_approved`, `completion_rejected` — and a new nullable `challenge` FK (string-referenced as `"community.Challenge"`, same pattern already used for the existing `post` FK, so no circular-import issue).
- No changes needed to `Challenge`/`UserStageProgress`/`UserCountProgress`/`UserStreakProgress` — completion requests read those tables but don't modify them.
- Two migrations, both additive, both applied with zero interactive prompts: `community/migrations/0012_completionrequest.py`, `accounts/migrations/0006_notification_challenge_and_more.py`.

### 7.2 Backend — Views (`community/views.py`)

- `CompletionRequestSubmitView` — `POST /api/challenges/<id>/completion-request/`. Reuses the `has_completed_challenge()` helper from Round 2 (Layer 1) to verify eligibility — same function, two different consumers now. Rejects with 409 if a pending-or-approved request already exists for that `(user, challenge)`; notifies every hub admin except the submitter themselves.
- `MyCompletionRequestView` — `GET /api/challenges/<id>/completion-request/mine/` — lets the member's own UI show Pending/Approved/Rejected instead of a blind "Submit" button. Returns a bare JSON `null` if no request has ever been submitted (confirmed Android's `Response<CompletionRequest?>` parses that correctly).
- `HubCompletionRequestsListView` — `GET /api/hubs/<id>/completion-requests/`, admin-gated (creator or `HubMembership.role=admin`), lists only `pending` requests across all the hub's challenges.
- `CompletionRequestReviewView` — `PATCH /api/completion-requests/<id>/`, body `{action: "approve"|"reject", admin_note?}`:
  - Reuses `ensure_challenge_admin()` from Round 1 for the permission check.
  - Blocks re-reviewing an already-reviewed request (400).
  - **Approve** → creates a `Post` (`post_type=admin_announcement`, `challenge` set) with auto-generated celebratory content + the admin's optional note appended; links it back via `completion_request.announcement_post`; notifies the member.
  - **Reject** → enforces the spec's "minimum 20 characters" rule on `admin_note` server-side (400 if shorter); notifies the member.

### 7.3 Backend Verification

Full HTTP smoke test via Django's `Client` + JWT covering the entire loop end-to-end:
1. Submitting before completion → 400.
2. Logging progress to completion via a post (Round 6's flow), then submitting → 201, correct serialized fields.
3. Duplicate pending submission → 409.
4. Member's own status check → `"pending"`.
5. Non-admin listing a hub's requests → 403; admin listing → 200 with the pending request.
6. Rejecting with a too-short note → 400 with the exact spec-mandated message.
7. Approving → 200, response includes the new `announcement_post` id; fetched that `Post` directly and confirmed `post_type="admin_announcement"` and the content matches the expected `"🎉 @username completed "Challenge"!\n\nAdmin note"` format.
8. Re-reviewing the now-approved request → 400 ("already been reviewed").
9. Member's notification feed shows the `completion_approved` entry with the correctly formatted message and `challenge_id`/`challenge_title` populated.
10. Verified zero leftover test rows after cleanup.

### 7.4 Android

**Data layer:**
- `data/model/CompletionRequest.kt` (new) — mirrors the serializer shape; `CompletionStatus` object for the 3 status string constants.
- `data/model/NotificationItem.kt` — added `challengeId`/`challengeTitle` fields + a new `NotificationType` string-constant object (replacing the magic-string `"follow"`/`"like"`/`"comment"` checks that were previously inline in `NotificationsFragment`).
- `HubApiService.kt` / `HubRepository.kt` — added `submitCompletionRequest`, `getMyCompletionRequest`, `getHubCompletionRequests`, `approveCompletionRequest`, `rejectCompletionRequest`.

**Member-facing — Challenge Detail screen:**
- New `section_completion` card in `fragment_challenge_detail.xml`, sitting between the info card and the model-specific progress section. Hidden unless the challenge is complete or a request already exists.
- `ChallengeDetailViewModel` gained `myCompletionRequest` (loaded alongside the challenge and leaderboard in `load()`) and `submitCompletionRequest()`.
- `ChallengeDetailFragment.updateCompletionSection()` renders 4 states: not-yet-complete (section hidden), complete-but-not-submitted ("🎉 Submit for Review" button), `pending` ("⏳ Pending review", no button), `approved` ("✅ Completion approved!" + admin's note if any), `rejected` (admin's note shown + "🎉 Resubmit for Review" button — submitting again is allowed since the unique constraint only blocks *pending*-or-*approved* duplicates, not rejected ones). Tapping submit opens a plain `AlertDialog` with an `EditText` for the optional note.

**Admin-facing — new Completion Requests screen:**
- `CompletionRequestsFragment` + `CompletionRequestsViewModel` + `CompletionRequestAdapter`, reachable from a new "Completion Requests" row in `HubSettingsFragment` (the admin-only area) showing a live pending-count badge (`circle_blue` drawable, reused from the Round 1 unread-notification dot).
- Each row shows the member's avatar/username, the challenge title, their note (if any), and Approve/Reject buttons. Approve asks for a simple confirm dialog (no note — kept optional per the core-loop scope); Reject opens a dialog with a required `EditText`, client-side validated to ≥20 characters before submitting (mirroring the server-side check, so the user gets instant feedback instead of waiting for a 400).
- Registered as `completionRequestsFragment` in `main_nav_graph.xml`.

**Notification tap navigation:**
- `NotificationsFragment.handleTap()` extended for the 3 new types — all three navigate to `ChallengeDetailFragment` via `challenge_id` (not to the admin review screen directly, since the notification payload doesn't carry a `hub_id` — a deliberate scope cut; the admin can reach the review screen from there via Hub Settings).

**Verified:** `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL, no new warnings. `python manage.py check` → no issues.

### 7.5 What's Explicitly Deferred (per the round-3 scoping decision)

- The auto-generated review **timeline** (chronological feed of the member's posts + stage-completion events + "14-day gap here" markers + summary card) — admins currently just see the completion request itself (member note + which challenge), not their full activity history.
- **"Request more proof"** soft-reject action — only hard Approve/Reject exist this round.
- The **5-day/10-day reminder notification cron job** for un-reviewed requests.
- Support action buttons on the announcement post (Mental Support / Physical Support / Gift) and the separate "Celebrate" confetti reaction — the announcement post is a plain text post for now, validatable/likeable/commentable like any other post but with no bespoke UI.
- A "current rank" indicator and the Template System remain deferred from earlier rounds too.

---

## Part 8 — HubChallenge Spec Adoption: Round 4, Template System (2026-06-16)

User-confirmed scope: **core only** — 15 official templates + a picker step in Create Challenge. Community template submissions (the 50+ members / 70%+ completion-rate eligibility check + moderation queue) were explicitly left out.

**Adaptation note:** the spec's templates apply at the *hub* level (a hub IS the challenge in the original design). In our system a hub can hold many challenges, each with its own progress model, so templates were adapted to apply at *challenge* creation time instead — consistent with every other adaptation made in Rounds 1–3.

### 8.1 Backend — Data Model

- `ChallengeTemplate` (`community/models.py`): `name`, `category` (8 choices: fitness/finance/learning/reading/mindfulness/creative/career/other), `progress_model`, `description`, `is_official`, `created_by` (nullable — official templates have none), `use_count`, plus flattened default-config fields for count (`count_target`, `count_unit_label`, `count_entry_increment`) and streak (`streak_target_days`, `streak_frequency`, `streak_grace_days`). Stage-based templates instead get a separate `TemplateStage` model (mirrors `ChallengeStage` but decoupled from any live challenge) since a list of ordered stages doesn't fit as flat columns.
- Two migrations: `0013_challengetemplate_templatestage.py` (schema, auto-generated, no prompts) and a **hand-written data migration** `0014_seed_official_templates.py` using `RunPython` + `apps.get_model()` (the correct Django pattern for data migrations — avoids importing the live model class, which could drift from the migration's expectations as the schema evolves later). The reverse migration deletes all `is_official=True` rows, making this fully reversible.
- All 15 templates from the spec were seeded, with their model/category mapping preserved exactly: stage-based (Learn Python from Zero — 8 stages, Lose 10% Body Fat — 6 stages, Run a Half Marathon — 7 stages, Start a Business (MVP) — 6 stages, Learn a New Language (A2) — 5 stages, Read the Entire Quran — 30 stages generated as "Juz 1".."Juz 30"), count-based (Read 12 Books in a Year, Earn Your First $1,000, Write a Novel/NaNoWriMo, Publish 30 Social Media Posts, Save $500 Emergency Fund), streak-based (Meditate Every Day, Build a 30-Day Coding Habit, Digital Detox, Complete a 75-Day Hard).

### 8.2 Backend — Views

- `TemplateListView` — `GET /api/templates/`, filterable by `category`, `progress_model`, and free-text `search` (matches name or description). No pagination — 15 fixed rows, returned as a plain array, matching the pattern already used for `ChallengeLeaderboardView`/`PostValidationsListView`'s `validators` list.
- `ChallengeListCreateView.create()` extended to accept an optional `template_id`. The key design rule, taken directly from the spec ("creates a fully editable copy... does NOT link them to the template"): **template values are defaults, not overrides** — if the request explicitly provides `title`/`description`/`stages`/`count_config`/`streak_config`, those win; only missing fields fall back to the template's values. `progress_model` is the one exception that the template *can* dictate when the client doesn't specify one. On successful creation with a template, `ChallengeTemplate.use_count` is atomically incremented (`F("use_count") + 1`) inside the same `transaction.atomic()` block as the rest of the creation.

### 8.3 Backend Verification

Full HTTP smoke test:
1. `GET /api/templates/` → all 15.
2. Filter by `category=fitness` → exactly the 3 fitness templates. Filter by `progress_model=stage` → exactly the 6 stage templates. Search `"python"` → exactly 1 match.
3. Applied the "Learn Python from Zero" stage template with **no overrides** → new challenge got the template's title and all 8 stages, confirmed via `GET /api/challenges/<id>/`.
4. Applied the same template again **with a custom title override** ("My Python Journey") → title override respected, stages still copied from the template (since stages weren't explicitly overridden).
5. Applied a count template and a streak template, both with no overrides → correct `target_count`/`unit_label` and `target_days`/`grace_days` copied.
6. Confirmed `use_count` incremented correctly across multiple applications (2 for the template applied twice, 1 each for the others).
7. **Confirmed the "copy, don't link" rule**: the new challenge's stages are independent rows (editing/deleting them would never touch the template), and the original template's `stages.count()` was unchanged (still 8) after being applied twice.
8. Verified zero leftover test data after cleanup, and the 15 official templates remained untouched at exactly 15.

### 8.4 Android

- `data/model/ChallengeTemplate.kt` (new) — `ChallengeTemplate`, `TemplateStage` (a different class from `ChallengeStageStatus`, no naming collision), and a `TemplateCategory` object with the 8 category constants + a `label()` helper for display strings.
- `CreateChallengeRequest` — added `templateId: Int?`.
- `HubApiService.kt`/`HubRepository.kt` — added `getTemplates(category, progressModel, search)`.
- New `bottom_sheet_template_picker.xml` + `item_template_picker_row.xml` — same manual-row-inflation pattern as the existing hub/challenge pickers (not a RecyclerView, for consistency with this codebase's established style for small, bottom-sheet-hosted lists). Each row shows a model-specific emoji (🏆 stage / 📊 count / 🔥 streak), the template name, "`Category` · Used `N` times", and a description preview. A "✏️ Start from Scratch" row at the top clears any applied template.
- `CreateChallengeFragment.kt`:
  - New "📋 Start from a Template" row added above the title field in `bottom_sheet_create_challenge.xml`, showing the applied template's name once one is picked.
  - `applyTemplate()` pre-fills the title, description, auto-selects the right model chip, and pre-fills that model's section: for stage templates it clears `containerStages` and re-adds one row per template stage (title + proof-type spinner pre-set); for count/streak it fills the existing target/unit/days/frequency/grace fields. The user can still edit anything afterward before submitting — the template only seeds the form, exactly matching the backend's "defaults, not overrides" behavior.
  - `addStageRow()` gained optional `prefillTitle`/`prefillProofType` parameters to support this (previously only used for the empty "+ Add Stage" button).
  - On submit, `selectedTemplateId` is included in the request whenever a template was applied (even if the user edited the pre-filled fields), so `use_count` still increments and the source template is recorded server-side for analytics, regardless of how much the user customized the form.

**Verified:** `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL, no new warnings. `python manage.py check` → no issues.

### 8.5 What's Explicitly Deferred (per the round-4 scoping decision)

- **Community template submissions** — any hub admin meeting the spec's 50+ members / 70%+ completion-rate threshold submitting one of their own challenges as a public template (`is_official=false`, pending review) is not implemented. The `created_by` field on `ChallengeTemplate` exists and is nullable specifically to support this later without a schema change.
- A moderation/review queue for community-submitted templates (would mirror the Round 3 admin-review pattern).
- Template category filter chips in the Android picker UI — the backend supports `?category=` filtering, but the picker currently shows all 15 unfiltered; adding category chips would be a pure-UI follow-up with no backend changes needed.

### 8.6 Spec Coverage Summary

With this round, all four major pieces of `HubChallenge_Spec_v1.pdf` have a working implementation in HubRise, each adapted from hub-level to challenge-level to fit this app's "one hub, many challenges" model:

| Spec Section | Status |
|---|---|
| Progress Bar System (3 models: Stage/Count/Streak) | ✅ Round 1 (Foundation) |
| Layer 1 — Peer Micro-Validation | ✅ Round 2 |
| "Progress only via posts" (this app's own requirement, not in the original spec) | ✅ Round 2.5 |
| Layer 3 — Admin Final Review (core loop) | ✅ Round 3 |
| Template System (official templates) | ✅ Round 4 (this round) |
| Layer 3 — review timeline, reminders, "request more proof" | ⏸ deferred |
| Template System — community submissions | ⏸ deferred |
| Announcement post support buttons / Celebrate reaction | ⏸ deferred |

---

## Part 9 — Two User-Reported Fixes (2026-06-16)

Two concerns raised after testing Round 3/Round 4: (1) the admin's Completion Requests screen could only ever show pending requests, with no way to review past decisions; (2) count-based challenges always treated a logged amount as additive, which doesn't fit goals where the member reports a cumulative number they already track elsewhere (e.g. "I've now run 40km total" should set the total to 40, not add 40 to whatever was logged before).

### 9.1 Fix 1 — Admin can see reviewed history, not just pending

**Backend:** `HubCompletionRequestsListView.get_queryset()` now reads a `?status=` query param (default `"pending"`, so the existing Hub Settings badge-count call is unaffected). Pass `?status=approved`, `?status=rejected`, or `?status=all` to see the rest.

**Android:**
- `HubApiService.getHubCompletionRequests()` / `HubRepository.getHubCompletionRequests()` gained a `status` parameter (default `"pending"`).
- `CompletionRequestsFragment` now has a `TabLayout` with Pending / Approved / Rejected tabs, reloading the list via `viewModel.load(hubId, status)` on tab change.
- `CompletionRequestAdapter` / `item_completion_request.xml` gained a second display mode: pending items still show the Approve/Reject buttons (`row_actions`); approved/rejected items instead show a read-only `row_outcome` — a colored status chip ("✅ Approved by @admin" / "❌ Rejected by @admin", new `success_chip_bg`/`error_chip_bg` drawables + a new `success_bg` color) plus the admin's note if one was given.

**Verified via HTTP smoke test:** default view shows only pending; after approving, the default view is empty (the request left pending status) while `?status=approved` now shows it; `?status=all` shows everything; `?status=rejected` correctly stays empty when nothing's been rejected.

### 9.2 Fix 2 — `is_cumulative` toggle for count-based challenges

Given the genuine ambiguity in how "amount" should behave (matches the original spec's wording either way, depending on the goal type), the user chose to make it a per-challenge admin choice rather than a global behavior change.

**Backend:**
- `ChallengeCountConfig` gained `is_cumulative` (Boolean, default `False` — preserves existing behavior for every challenge created before this fix). Migration `0015_challengecountconfig_is_cumulative.py`, purely additive.
- `ChallengeListCreateView.create()` reads `is_cumulative` from the client's `count_config` payload (defaults `False`).
- `CreatePostView._create_count_post()` now branches: `is_cumulative=False` (default) → `progress.current_count += amount` (unchanged, original spec-matching behavior). `is_cumulative=True` → `progress.current_count = amount` (the logged value **becomes** the new total). The auto-generated post content also changes wording accordingly: "📈 Logged +X — Y/Z" (additive) vs "📈 Now at Y/Z" (cumulative).
- `ChallengeDetailView` now includes `is_cumulative` in the `count_config` block of its response so the Android app can read it.

**Verified via HTTP smoke test, both modes side by side:**
- Additive challenge: log `3`, then log `40` → final total `43` (unchanged, confirms no regression).
- Cumulative challenge: log `3` (total → 3), then log `40` (total → exactly `40`, matching the user's reported expectation, not `43`). Confirmed `is_complete` flips to `true` and the API response carries `is_cumulative: true`.

**Android:**
- `CountConfig` (response) and `CountConfigInput` (request) both gained `isCumulative: Boolean`.
- `bottom_sheet_create_challenge.xml` — new switch in the count config section: "Entries report a running total", with explanatory subtext ("On: each entry IS the new total... Off: each entry adds up...").
- `CreateChallengeFragment.kt` wires the switch into `CountConfigInput.isCumulative`.
- `CreatePostFragment`/`CreatePostViewModel` — the picker's challenge list (`GET /api/hubs/<id>/challenges/`) doesn't include `count_config` (only the detail endpoint does), so selecting a count challenge now triggers a detail fetch (`loadCountConfig()`, mirroring the existing `loadCurrentStage()` pattern for stage challenges) purely to learn `is_cumulative`. The amount field's hint then reads either "Your new total so far (optional)" or "Amount to add (optional, defaults to the challenge's step size)".

**Verified:** `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL, no new warnings. `python manage.py check` → no issues.

---

## Part 10 — Announcement Support Buttons: Mental / Physical / Gift (2026-06-16)

User-reported bug + feature request: the "I believe this" (Layer 1 peer-validation) button was incorrectly showing on `admin_announcement` posts in the Android app, even though the backend's `PostValidateView` already rejected validation attempts on non-challenge-post-types. On announcement posts the spec calls for three different support actions instead: **Mental Support** (emoji + comment), **Physical Support** (free-text offer), and **Gift** (opens the completing member's wishlist link).

### 10.1 Bug fix — validate button visibility

`PostAdapter.applyValidateState()` previously gated visibility only on `post.challenge == null`, which is insufficient since announcement posts also carry a `challenge` reference. Fixed to additionally require `post.postType in PostType.CHALLENGE_TYPES` (stage_proof/count_entry/streak_checkin) — matching the backend's existing `Post.CHALLENGE_POST_TYPES` rule exactly.

### 10.2 Gift button — new `wishlist_url` field

**Backend:**
- `UserProfile.wishlist_url` (`URLField`, blank-default) — migration `0007_userprofile_wishlist_url.py`, purely additive.
- `UserPublicProfileView` GET/PATCH now read/write `wishlist_url`.
- `PostSerializer` gained `author_wishlist_url` (read-only `SerializerMethodField`) so the Android app learns a post author's wishlist link directly from the post payload, without an extra profile fetch.

**Verified via HTTP smoke test:** PATCH sets `wishlist_url` on a member's profile and it's readable via GET; an approved completion's announcement post correctly carries `author_wishlist_url`; the pre-existing rule rejecting `/validate/` on announcement posts (400, "Validation is only available for challenge-related posts") still holds — confirming the backend was already correct and only the Android UI had the bug.

### 10.3 Mental / Physical Support — reuse existing Comment infrastructure

Rather than building new backend models/endpoints, Mental and Physical support both reuse the existing `Comment` creation flow (`POST /api/posts/<id>/comments/`), which already notifies the post author (`Notification.TYPE_COMMENT`) and matches the spec's literal wording ("Mental Support (emoji + comment)"). Mental Support prefixes the comment with a ❤️ emoji (added automatically even if the optional note is left blank); Physical Support posts the free-text offer as-is (skipped if left blank). No new backend code was needed for this part.

### 10.4 Android

- `Post.kt` gained `authorWishlistUrl`. `UserPublicProfile.kt`/`UpdateProfileRequest.kt`/`UpdateProfileResponse.kt` all gained `wishlistUrl`. `UserRepository.updateProfile()` gained a `wishlistUrl` parameter.
- `fragment_edit_profile.xml` — new "Wishlist link" text field; `EditProfileFragment`/`EditProfileViewModel`/`ProfileFragment` thread the value through (read from arguments, saved via PATCH).
- `item_post.xml` — new `row_support` row (Mental/Physical/Gift, each an emoji + label in a `step_chip_bg`-styled tile), hidden by default and shown only when `post.postType == PostType.ANNOUNCEMENT`.
- `PostAdapter` gained `onMentalSupportClick`/`onPhysicalSupportClick`/`onGiftClick` callbacks and an `applySupportRow()` bind method mirroring the existing `applyValidateState()` pattern.
- New `utils/PostSupportHelper.kt` — a small per-fragment helper (takes a `Fragment` in its constructor) centralizing the three actions: `showMentalSupportDialog()`/`showPhysicalSupportDialog()` (AlertDialog + EditText, same pattern as `ChallengeDetailFragment`'s completion-request dialog) call `CommentRepository.createComment()`; `handleGiftClick()` opens `post.authorWishlistUrl` via `Intent.ACTION_VIEW`, or toasts "hasn't added a wishlist link yet" when blank. Wired into all four fragments that instantiate `PostAdapter` — `HomeFragment`, `HubDetailFragment`, `ProfileFragment`, `UserProfileFragment` — since announcement posts (authored by the completing member) can surface in the main feed, a hub feed, or a profile's post history alike.

**Verified:** `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL, no new warnings. `python manage.py check` → no issues. Backend HTTP smoke test passed (§10.2).
