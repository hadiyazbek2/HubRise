# HubRise Step 2 Plan — Home Screen + Bottom Navigation

## Context

Step 1 (full auth flow: Login + 3-step Signup) is complete and building. The app now needs the main shell — the experience users land on after login. Both Option 1 (Home feed) and Option 2 (Explore) share the same bottom navigation bar, so the right approach is to build the **navigation shell + Home screen first**, then add Explore in Step 3. Home is the primary landing screen; for brand-new users with no hub memberships, it shows an empty state that pushes them toward Explore.

---

## Part A — Quick Fix (Back Button Icon)

**Problem:** All 3 signup screens use `@android:drawable/ic_menu_close_clear_cancel` (an X icon) for the back button. It should be a left-pointing back arrow.

**Fix:**
1. Create `app/src/main/res/drawable/ic_back_arrow.xml` — a vector drawable of a left arrow (24×24dp, white or primary color path).
2. In all 3 layout files, change `android:src="@android:drawable/ic_menu_close_clear_cancel"` → `android:src="@drawable/ic_back_arrow"`.

Files to change:
- `app/src/main/res/layout/signup_step1_screen.xml` (line 31)
- `app/src/main/res/layout/signup_step2_screen.xml` (line 31)
- `app/src/main/res/layout/signup_step3_screen.xml` (line 31)

No Activity code changes needed (the click listeners already call `finish()`).

---

## Part B — Step 2: Home Screen + Bottom Navigation

### Recommended Approach
**Fragments + Navigation Component + BottomNavigationView** inside `MainActivity`. This gives deep-link support, back-stack management, and clean separation of each tab.

### What Gets Built

#### 1. Navigation Infrastructure
- Add Navigation Component dependency to `libs.versions.toml` + `app/build.gradle.kts`
- Create `res/navigation/main_nav_graph.xml` with 5 destinations:
  - `HomeFragment`, `ExploreFragment`, `CreatePostFragment`, `HubsFragment`, `ProfileFragment`
- Rewrite `activity_main.xml`:
  - `FragmentContainerView` (NavHostFragment) fills most of the screen
  - `BottomNavigationView` pinned to bottom with 5 items
  - Center item (Create/+) styled larger, elevated, blue filled circle (like TikTok)
- Create `res/menu/bottom_nav_menu.xml` with 5 items + icons
- Create 5 placeholder Fragment classes under `ui/home/`, `ui/explore/`, `ui/create/`, `ui/hubs/`, `ui/profile/`
- Update `MainActivity.kt` to wire `NavController` ↔ `BottomNavigationView`
- Update `AndroidManifest.xml`: add `MainActivity` as exported launcher, remove launcher from `LoginActivity`; add logic to `MainActivity` to redirect to `LoginActivity` if no token

#### 2. Home Screen — Post Feed
**Layout:** `fragment_home.xml`
- `SwipeRefreshLayout` wrapping a `RecyclerView` (vertical, LinearLayoutManager)
- Empty-state view (hidden by default): illustration + "Join a hub to see posts" message + "Explore Hubs" button
- Top bar with app logo left + notification bell icon right

**Post card layout:** `item_post.xml`
- Author row: circular avatar (Coil) + username + hub name chip + timestamp + overflow menu
- Content text (max 3 lines, "see more" toggle)
- Optional media image (16:9, rounded corners, Coil)
- Action row: Like (heart) + count, Comment + count, Share button

**Data Layer:**
- `Post` data class (id, authorUsername, authorAvatarUrl, hubName, content, mediaUrl, createdAt, likesCount, commentsCount, isLiked)
- `FeedApiService.kt` — Retrofit interface: `GET /api/posts/feed/` + `GET /api/posts/global/` (both return `List<Post>`)
- `PostRepository.kt` — fetches feed, handles auth header injection
- `HomeViewModel.kt` — LiveData `posts`, `isLoading`, `isEmpty`; calls repository; exposes `refresh()`
- `PostAdapter.kt` — RecyclerView adapter with DiffUtil; handles like toggle locally

#### 3. Auth Guard in MainActivity
On `onCreate`, check `UserPreferences.hasToken()`. If false, redirect to `LoginActivity` and finish. After successful login/signup, `navigateToHome()` in `SignupStep3ProfileSetupActivity` should `startActivity(MainActivity)` + `finish()`.

---

## File Structure After Step 2

```
ui/
  auth/          (unchanged)
  home/
    HomeFragment.kt
    HomeViewModel.kt
  explore/
    ExploreFragment.kt        (placeholder)
  create/
    CreatePostFragment.kt     (placeholder)
  hubs/
    HubsFragment.kt           (placeholder)
  profile/
    ProfileFragment.kt        (placeholder)
data/
  api/
    FeedApiService.kt         (new)
  model/
    Post.kt                   (new)
  repository/
    PostRepository.kt         (new)
res/
  layout/
    activity_main.xml         (rewritten)
    fragment_home.xml         (new)
    item_post.xml             (new)
  navigation/
    main_nav_graph.xml        (new)
  menu/
    bottom_nav_menu.xml       (new)
  drawable/
    ic_back_arrow.xml         (new — Part A fix)
    ic_nav_home.xml           (new)
    ic_nav_explore.xml        (new)
    ic_nav_create.xml         (new)
    ic_nav_hubs.xml           (new)
    ic_nav_profile.xml        (new)
    ic_add_circle_filled.xml  (new — center FAB create button)
```

---

## Backend Changes Needed (tell the user to add to Django)

The backend currently only has auth endpoints. Step 2 requires:

### New Models

**Hub**
```python
id, name, description, category (ForeignKey Interest),
members_count, cover_image_url, is_public, created_by, created_at
```

**Post**
```python
id, author (FK User), hub (FK Hub), content (TextField),
media_url (optional), created_at, updated_at
```

**PostLike**
```python
user (FK), post (FK), created_at — unique_together(user, post)
```

**HubMembership**
```python
user (FK), hub (FK), joined_at — unique_together(user, hub)
```

### New API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/posts/feed/` | Posts from hubs the auth user is a member of (paginated, newest first) |
| GET | `/api/posts/global/` | All public posts (paginated) |
| GET | `/api/hubs/` | List all hubs (filterable by `?interest=`) |
| GET | `/api/hubs/recommended/` | Hubs matching the auth user's interests |
| POST | `/api/hubs/{id}/join/` | Join a hub (creates HubMembership) |
| POST | `/api/hubs/{id}/leave/` | Leave a hub |
| GET | `/api/hubs/{id}/posts/` | Posts within a specific hub |
| POST | `/api/posts/` | Create a new post |
| POST | `/api/posts/{id}/like/` | Toggle like on a post |

### Auth
All new endpoints require the `Authorization: Bearer <access_token>` header.

---

## Verification

1. After login/signup → app lands on `MainActivity` with Home tab selected
2. If no token on launch → redirected to `LoginActivity`
3. Bottom nav switches between tabs; back stack is maintained per-tab
4. Home shows loading spinner, then posts list (or empty state)
5. Pull-to-refresh reloads the feed
6. Center "+" button is visually distinct (filled blue circle, larger)
7. Back arrow on signup screens points left (not X)
