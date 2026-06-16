# HubRise Step 3 Plan — Hubs Tab (Browse, Detail, Create, Join/Leave + Create Post)

## Context

Step 2 delivered the navigation shell and a working home feed. Step 3 closes the core loop:
**browse hubs → join → see posts in hub → create posts**.
Explore stays a placeholder. The Hubs tab (currently "Coming Soon") is the focus.

Backend is fully ready — all endpoints documented in `Backend_structure.md` already exist:
- `GET /api/hubs/` (includes `is_member` per hub)
- `GET /api/hubs/recommended/`
- `GET /api/hubs/{id}/`
- `POST /api/hubs/`
- `POST /api/hubs/{id}/join/` and `/leave/`
- `GET /api/hubs/{id}/posts/`
- `POST /api/posts/`

---

## Part A — Hubs Tab (replaces placeholder)

### What it shows
Two sections in one scrollable screen:

**Section 1 — "My Hubs"**
Hubs where `is_member == true` from `GET /api/hubs/`. If empty, show inline prompt: "You haven't joined any hubs yet."

**Section 2 — "Recommended for You"**
From `GET /api/hubs/recommended/`. If the user has no interests set, show all hubs instead (`GET /api/hubs/`).

A "Create Hub" FAB (bottom-right, blue, + icon) is always visible.

### Layout: `fragment_hubs.xml`
- `CoordinatorLayout` root
- `AppBarLayout` with toolbar: title "Hubs" + search icon (right)
- `SwipeRefreshLayout` wrapping `RecyclerView`
- `FloatingActionButton` (create hub, pinned bottom-right)
- Empty state (only shown when both sections are empty)

### Hub card: `item_hub_card.xml`
- Cover image (16:9, Coil, rounded corners, fallback = solid blue gradient)
- Hub name (bold 16sp)
- Interest/category chip (small, rounded)
- Member count (e.g. "142 members")
- Join / Joined toggle button (right side)
  - "Join" = outlined blue button
  - "Joined" = filled blue button (tap to leave, with confirm dialog)

### Section header: `item_section_header.xml`
- Simple `TextView` label ("My Hubs", "Recommended for You")

### RecyclerView adapter: `HubsAdapter`
- Two view types: `HEADER` and `HUB_CARD`
- Submits a flat list: `[Header("My Hubs"), hub1, hub2, ..., Header("Recommended"), hub3, hub4, ...]`

### ViewModel: `HubsViewModel`
```
loadHubs()
  → GET /api/hubs/            (all hubs, split by is_member)
  → GET /api/hubs/recommended/  (for the "Recommended" section)

toggleJoin(hub: Hub)
  → POST /api/hubs/{id}/join/   or  /leave/
  → updates the hub in the list in-place (no full reload)
```
LiveData: `myHubs: List<Hub>`, `recommended: List<Hub>`, `isLoading`, `error`

### Fragment: `HubsFragment`
- Observes ViewModel, builds the flat adapter list from both LiveData
- FAB click → navigate to `createHubFragment`
- Hub card click → navigate to `hubDetailFragment` passing `hubId`
- Join/Leave button click → `viewModel.toggleJoin(hub)`

---

## Part B — Hub Detail Screen

### Layout: `fragment_hub_detail.xml`
- Full-width cover image (220dp tall) with a back arrow overlaid (top-left)
- Hub name (bold 20sp) + privacy badge (Public / Private chip)
- Description text (expandable — "Show more" after 3 lines)
- Stats row: **X Members  ·  X Posts**
- "Join Hub" / "Leave Hub" button — full-width, sticky above the tab bar
- `TabLayout` with two tabs: **Posts** | **Members**
- `RecyclerView` below tabs (reuses `PostAdapter` for Posts; new `MemberAdapter` for Members)

### ViewModel: `HubDetailViewModel`
```
loadHub(hubId)      → GET /api/hubs/{id}/
loadPosts(hubId)    → GET /api/hubs/{id}/posts/
toggleJoin(hubId)   → POST /api/hubs/{id}/join/ or /leave/
```
LiveData: `hub: Hub`, `posts: List<Post>`, `isMember: Boolean`, `isLoading`

### Navigation
- Destination `hubDetailFragment` receives `hubId: Int` as argument (Safe Args or Bundle)
- Back arrow pops back to `hubsFragment`
- After joining a hub, home feed should refresh on next visit

---

## Part C — Create Hub Screen

### Layout: `fragment_create_hub.xml`
- Top bar: "Create Hub" title + "Create" text button (right, enabled only when name filled)
- Cover image picker (tap to open gallery; 16:9 preview slot with "Add cover photo" placeholder)
- Hub Name field (required, max 50 chars, character counter)
- Description field (optional, max 300 chars, multiline)
- Category row: horizontal chips of interest names (tap to select one; fetches from `GET /api/hubs/` interest list or hardcoded if no endpoint)
- Privacy toggle: **Public** / **Private** (RadioGroup or toggle chips)

### ViewModel: `CreateHubViewModel`
```
createHub(name, description, categoryId, isPublic)
  → POST /api/hubs/
  → on success: emit hubId, navigate to HubDetailFragment for the new hub
```

### Notes
- Cover image upload is optional in this step — skip if `POST /api/hubs/` doesn't support multipart. Send `null` for `cover_image_url`.
- Auto-join after creation (backend already does this, `members_count` starts at 1).

---

## Part D — Create Post Screen (replaces placeholder)

The center "+" nav button leads here.

### Layout: `fragment_create_post.xml`
- Top bar: "New Post" title + "Post" text button (right, enabled only when hub selected + content filled)
- **Hub selector row** — shows "Post to: [Hub Name]" with a chevron; tapping opens a bottom sheet
  - Bottom sheet lists hubs the user is a member of (from `GET /api/hubs/` filtered by `is_member`)
  - If no hubs joined: shows "Join a hub first" with a link to Hubs tab
- Content text field (multiline, 500 char max, character counter, autofocus)
- Attach image button (bottom of screen, icon + "Add photo" label)
- Image preview strip (horizontal, shown when an image is attached)

### ViewModel: `CreatePostViewModel`
```
loadJoinedHubs()   → GET /api/hubs/ filtered by is_member == true
createPost(hubId, content, mediaUri?)  → POST /api/posts/
  → on success: pop to home, trigger homeViewModel.refresh()
```

---

## Data Layer

### `Hub` data class
```kotlin
data class Hub(
    val id: Int,
    val name: String,
    val description: String,
    @SerializedName("category_name") val categoryName: String?,
    @SerializedName("members_count") val membersCount: Int,
    @SerializedName("cover_image_url") val coverImageUrl: String?,
    @SerializedName("is_public") val isPublic: Boolean,
    @SerializedName("is_member") val isMember: Boolean = false
)
```

### `HubApiService`
```kotlin
GET  api/hubs/                    → PaginatedResponse<Hub>
GET  api/hubs/recommended/        → PaginatedResponse<Hub>
GET  api/hubs/{id}/               → Hub
POST api/hubs/                    → Hub  (body: CreateHubRequest)
POST api/hubs/{id}/join/          → JoinLeaveResponse
POST api/hubs/{id}/leave/         → JoinLeaveResponse
GET  api/hubs/{id}/posts/         → PaginatedResponse<Post>
POST api/posts/                   → Post (body: CreatePostRequest)
```

### New request/response models
```kotlin
data class CreateHubRequest(val name: String, val description: String,
    val category: Int?, @SerializedName("is_public") val isPublic: Boolean)

data class CreatePostRequest(val hub: Int, val content: String,
    @SerializedName("media_url") val mediaUrl: String? = null)

data class JoinLeaveResponse(val detail: String,
    @SerializedName("members_count") val membersCount: Int)
```

### `HubRepository`
- Injects bearer token (same pattern as `PostRepository`)
- All hub + create-post calls

---

## Navigation Changes (`main_nav_graph.xml`)

Add destinations:
- `hubDetailFragment` — argument: `hubId: Int`
- `createHubFragment`
- Update `createPostFragment` — full implementation (no args needed)

Add actions:
- `hubsFragment → hubDetailFragment`
- `hubsFragment → createHubFragment`
- `createHubFragment → hubDetailFragment` (after creation)

---

## File Structure After Step 3

```
ui/
  hubs/
    HubsFragment.kt           (rewritten from placeholder)
    HubsViewModel.kt          (new)
    HubsAdapter.kt            (new — handles HEADER + HUB_CARD view types)
    HubDetailFragment.kt      (new)
    HubDetailViewModel.kt     (new)
    MemberAdapter.kt          (new)
    CreateHubFragment.kt      (new)
    CreateHubViewModel.kt     (new)
  create/
    CreatePostFragment.kt     (rewritten from placeholder)
    CreatePostViewModel.kt    (new)
data/
  api/
    HubApiService.kt          (new)
  model/
    Hub.kt                    (new)
    CreateHubRequest.kt       (new)
    CreatePostRequest.kt      (new)
    JoinLeaveResponse.kt      (new)
  repository/
    HubRepository.kt          (new)
res/
  layout/
    fragment_hubs.xml         (rewritten)
    fragment_hub_detail.xml   (new)
    fragment_create_hub.xml   (new)
    fragment_create_post.xml  (rewritten)
    item_hub_card.xml         (new)
    item_section_header.xml   (new)
  navigation/
    main_nav_graph.xml        (updated)
```

---

## Verification Checklist

1. Hubs tab shows "My Hubs" and "Recommended" sections
2. Joining a hub updates the card to "Joined" without a full reload
3. Tapping a hub card opens Hub Detail with posts and member count
4. Join/Leave button on detail screen updates immediately
5. Create Hub form validates, posts to backend, opens the new hub detail
6. Create Post shows only joined hubs in the picker
7. Posting successfully creates a post and the home feed shows it on next load
8. Back navigation from all new screens returns to Hubs tab
