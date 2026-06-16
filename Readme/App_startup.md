# HubRise — App Startup Document
*The single source of truth for what we are building, what exists today, and how every future feature must be built.*

---

## 1. Vision & Core Concept

HubRise is a goal-oriented social network. The defining idea: people join **Hubs** that share a real goal — run a marathon, read 12 books this year, ship a side project — and they hold each other accountable through progress updates, short videos, and evidence-backed achievement verification.

It is not a generic social feed. Every interaction is anchored to a goal or a hub. Even personal posts are tied to a user's broader story of chasing something.

### What makes it different from existing apps
| App | What it does well | What HubRise adds |
|-----|-------------------|-------------------|
| Instagram | Beautiful personal feed | Goal structure, hub accountability |
| TikTok | Short-video discovery | Goal-tagged videos, hub context |
| Strava | Fitness goal tracking | Any goal type, social richness, short video proof |
| Discord | Community groups | Structured goals + progress tracking + achievement broadcast |

---

## 2. App Overview

### Elevator Pitch
HubRise is a social network where **goals are the product.** Users join goal-specific groups called Hubs, document their journey through posts and short videos, track measurable challenges, and earn verified achievements broadcast to the whole community. Think Instagram accountability meets TikTok discovery, wrapped around a goal-tracking engine.

### Feature Summary

| Feature | Description | Status |
|---------|-------------|--------|
| **Hubs** | Goal-focused community groups with a main challenge + sub-challenges | ✅ Core built |
| **Home Feed** | Instagram-style feed of posts from joined hubs + hub announcements | ✅ Core built |
| **Explore / Shorts** | TikTok-style vertical video feed — "For You" global, "Your Hubs" personal | 🔲 Android stub |
| **Create Post** | Text + image/video posts, progress updates tied to challenges | ✅ Core built |
| **Challenge Tracking** | Per-user progress on hub challenges, visualized as progress bars | ✅ Core built |
| **Achievement System** | Admin-verified completion of challenges, broadcast as hub events + profile badges | 🔲 Not built |
| **Profile** | Posts, progress, achievements, liked posts, edit info | ✅ Partial |
| **Comments** | Threaded replies on posts | 🔲 Not built |
| **Search** | Find hubs and users by name or interest | 🔲 Not built |
| **Notifications** | In-app alerts for likes, comments, achievements, joins | 🔲 Not built |
| **Social Auth** | Google / Facebook / Apple login | ✅ Prototype |
| **User Follow** | Follow other users across hubs | 🔲 Not built |

---

### User Flow (Full Journey)

```
[ First Open ]
      │
      ▼
  Not logged in?
      │ Yes
      ▼
  Login Screen ──── "New here?" ──► Signup Step 1 (email + password)
                                         │
                                    Step 2 (username + full name + DOB)
                                         │
                                    Step 3 (interests + profile picture)
                                         │
                                    ◄────────────────────────────────
      │
      ▼
  MainActivity (bottom nav)
  ┌──────────────────────────────────────────────┐
  │  HOME │ EXPLORE │  (+)  │  HUBS │  PROFILE  │
  └──────────────────────────────────────────────┘

[ HOME ]
  • Paginated feed: posts from joined hubs
  • Like / Comment / Share on posts
  • Hub announcement cards (admin reposts)
  • Tap post → Post Detail (with comments)
  • Tap username → User Profile
  • Tap hub name → Hub Detail

[ EXPLORE ]
  • "For You" tab: full-screen vertical video scroll (public hubs)
  • "Your Hubs" tab: same, only from joined hubs
  • Like / Comment / Share from video overlay
  • Tap hub name badge → Hub Detail
  • Tap username → User Profile

[ CREATE (+) ]
  • Pick: New Post / Progress Update / New Hub
  • New Post: select hub → write content → attach media → post
  • Progress Update: select challenge → "+1" → auto-post with progress snapshot
  • New Hub: name, description, category, cover image, main challenge

[ HUBS ]
  • Tab 1 — Your Hubs:
      List of joined hubs with cover, name, member count, progress bar
      Tap → Hub Detail
  • Tab 2 — Explore Hubs:
      Search + interest filter
      Recommended section
      Cards with "Join" button

[ HUB DETAIL ]
  • Header: cover, name, category, member count, join/leave button
  • Tabs: Posts | Challenges | Members | About
  • Posts tab: hub's post feed with like/comment
  • Challenges tab: main challenge + sub-challenges, user's progress bars
  • If user is admin: "Admin" button → edit hub / repost / approve achievements

[ PROFILE ]
  • Own profile: photo, name, username, hub count, edit button
  • Tabs: Posts | Progress | Achievements | Liked
  • Achievements tab: verified goals with badge UI
  • Settings: edit profile, logout, notifications, privacy
  • Other user's profile: same view, with Follow button instead of Edit

[ ACHIEVEMENT FLOW ]
  1. User's challenge progress reaches target_count
  2. Achievement created (pending verification)
  3. Hub admin reviews user's posts as evidence
  4. Admin approves → achievement marked verified
  5. Hub Event broadcast: "🏆 [User] completed [Challenge]!"
  6. Event appears in all members' Home feeds + hub announcements
  7. Badge appears on user's Profile > Achievements
```

---

## 3. Suggestions from Code Review

These are improvements and design ideas based on what's been built so far and where the app is heading.

### Suggestion 1 — Post Type Flag (Do This Before Comments)
Right now all posts are the same type. Before adding comments, achievements, and hub announcements, add a `post_type` field to `Post`:
- `regular` — standard post
- `progress_update` — logged from a challenge (links to a challenge)
- `achievement_broadcast` — auto-generated when an achievement is verified
- `admin_announcement` — admin repost / pinned message

This avoids creating separate models for each type and keeps the feed unified.

### Suggestion 2 — Token Auto-Refresh in Retrofit
The Android `RetrofitClient.kt` currently doesn't handle 401 responses. When the 30-minute access token expires, all API calls will fail silently. Add an `Authenticator` to Retrofit that calls `/api/auth/token/refresh/` automatically. This is critical for good UX.

### Suggestion 3 — Media as File Fields, Not URLs
Currently `Post.media_url` and `Hub.cover_image_url` are `URLField`s. This means the Android app would have to upload media somewhere else and pass the URL. Change them to `FileField` and add multipart endpoints — the same pattern already used for `UserProfile.profile_picture`. This should happen before Explore (short videos) is built.

### Suggestion 4 — Hub Admin Role on Membership
`HubMembership` currently has no `role` field. Add `role` with choices `member`/`admin`/`moderator` before building any admin features. When a hub is created, the creator gets `role='admin'`. This unlocks: edit hub, approve achievements, repost, set main challenge.

### Suggestion 5 — `comments_count` on Post
The Android `Post.kt` model already has `commentsCount` field, but the backend `PostSerializer` doesn't return it. Add a `comments_count = serializers.IntegerField(source='comments.count', read_only=True)` to `PostSerializer` as soon as the `Comment` model exists. Keep the field in the Android model — it's correct to plan ahead here.

### Suggestion 6 — Hub Member Cap
Consider limiting a user to a maximum of 20 joined hubs. This keeps the hub concept meaningful (you're genuinely committed to these goals, not hoarding memberships) and makes the "Your Hubs" feed more focused.

### Suggestion 7 — "Personal Hub" for Free Posts
The current model requires every post to belong to a hub. Some users will want to post general life content not tied to a goal. A clean solution: auto-create a private "Personal" hub for every user on signup. Posts there are personal posts. This keeps the data model consistent (posts always have a hub) while allowing personal expression.

### Suggestion 8 — Explore Algorithm Starting Point
For the Explore feed, don't try to build a recommendation algorithm immediately. Start with: "video posts from public hubs, ordered by created_at DESC, filtered to hubs matching user's interests." Add engagement scoring later. The TikTok-style UI will be impressive even with a simple chronological feed.

### Suggestion 9 — Verification Evidence Window
For the achievement verification system, consider automatically gathering the last N posts by the user in the relevant hub as "evidence." Display these to the admin in the verification screen so they don't have to scroll the whole hub feed manually. The backend can add a `/api/achievements/{id}/evidence/` endpoint that returns the user's posts in that hub near the completion time.

### Suggestion 10 — Database: Move to PostgreSQL Early
SQLite works fine for development but has quirks with concurrent writes and JSONField behavior. Since you're using Django's ORM throughout, switching to PostgreSQL is just a settings change + new migration. Do this before Phase 3 (Explore) so you never have to debug SQLite-specific weirdness under real load.

---

## 5. Tech Stack

### Backend
| Layer | Technology |
|-------|-----------|
| Framework | Django 6.x + Django REST Framework 3.17 |
| Auth | JWT via `djangorestframework-simplejwt` (access 30 min, refresh 7 days) |
| Database (dev) | SQLite |
| Database (prod) | PostgreSQL (to migrate later) |
| Media files | Django media serving (dev), cloud storage (prod — TBD) |
| Social auth | Google, Facebook, Apple — id_token decode (prototype, no sig verification yet) |

### Android
| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| Architecture | MVVM + Repository pattern |
| Navigation | Android Navigation Component (single-activity) |
| Networking | Retrofit 2 + Gson |
| Auth storage | DataStore (UserPreferences) |
| Image loading | Glide (assumed, not yet verified) |
| UI | XML layouts |

---

## 6. Current Database Schema

### App: `accounts`

**`Interest`**
| Field | Type | Notes |
|-------|------|-------|
| id | BigAutoField PK | |
| name | CharField(100) | unique |

**`UserProfile`** (1:1 with Django User)
| Field | Type | Notes |
|-------|------|-------|
| id | BigAutoField PK | |
| user | OneToOne → User | CASCADE |
| full_name | CharField(255) | |
| date_of_birth | DateField | |
| phone_number | CharField(30) | optional |
| profile_picture | FileField | upload_to=`profile_pictures/` |
| interests | M2M → Interest | |
| updated_at | DateTimeField | auto_now |

**`SocialAccount`**
| Field | Type | Notes |
|-------|------|-------|
| id | BigAutoField PK | |
| user | FK → User | CASCADE |
| provider | CharField(20) | `google` / `facebook` / `apple` |
| provider_user_id | CharField(255) | unique per provider |
| email | EmailField | |
| extra_data | JSONField | raw token claims |
| created_at / updated_at | DateTimeField | |

---

### App: `community`

**`Hub`**
| Field | Type | Notes |
|-------|------|-------|
| id | BigAutoField PK | |
| name | CharField(150) | |
| description | TextField | |
| category | FK → Interest | PROTECT |
| members_count | PositiveIntegerField | denormalized counter |
| cover_image_url | URLField | |
| is_public | BooleanField | default True |
| created_by | FK → User | CASCADE |
| created_at | DateTimeField | |

**`Post`**
| Field | Type | Notes |
|-------|------|-------|
| id | BigAutoField PK | |
| author | FK → User | CASCADE |
| hub | FK → Hub | CASCADE — **every post belongs to a hub** |
| content | TextField | |
| media_url | URLField | optional |
| is_public | BooleanField | default True |
| created_at / updated_at | DateTimeField | |

**`PostLike`**
| Field | Type | Notes |
|-------|------|-------|
| id | BigAutoField PK | |
| user | FK → User | CASCADE |
| post | FK → Post | CASCADE |
| created_at | DateTimeField | |
| unique_together | (user, post) | |

**`HubMembership`**
| Field | Type | Notes |
|-------|------|-------|
| id | BigAutoField PK | |
| user | FK → User | CASCADE |
| hub | FK → Hub | CASCADE |
| joined_at | DateTimeField | |
| unique_together | (user, hub) | |

**`Challenge`**
| Field | Type | Notes |
|-------|------|-------|
| id | BigAutoField PK | |
| hub | FK → Hub | CASCADE |
| title | CharField(100) | |
| description | TextField | |
| target_count | PositiveIntegerField | how many times to complete |
| is_main | BooleanField | True = the hub's main goal |
| created_by | FK → User | CASCADE |
| ends_at | DateTimeField | optional deadline |
| created_at | DateTimeField | |

**`ChallengeProgress`**
| Field | Type | Notes |
|-------|------|-------|
| id | BigAutoField PK | |
| challenge | FK → Challenge | CASCADE |
| user | FK → User | CASCADE |
| current_count | PositiveIntegerField | |
| created_at / updated_at | DateTimeField | |
| unique_together | (challenge, user) | |

---

## 7. Current API Endpoints (All Working)

Base URL (dev): `http://10.0.2.2:8000` (Android emulator → localhost)

### Auth — `/api/auth/`
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/signup/` | None | Create account. Returns tokens + user. |
| POST | `/api/auth/login/` | None | Email+password login. Returns tokens + user. |
| POST | `/api/auth/social-login/` | None | OAuth token login (Google/FB/Apple). |
| POST | `/api/auth/logout/` | Bearer | Blacklists refresh token. |
| GET | `/api/auth/check-email/?email=` | None | `{"available": bool}` |
| GET | `/api/auth/check-username/?username=` | None | `{"available": bool}` |
| POST | `/api/auth/token/refresh/` | None | `{"refresh": "..."}` → `{"access": "..."}` |
| POST | `/api/users/{user_id}/profile-picture/` | Bearer | Multipart upload. |

### Community — `/api/`
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/posts/feed/` | Bearer | Posts from user's joined hubs (paginated) |
| GET | `/api/posts/global/` | Bearer | All public hub posts (paginated) |
| POST | `/api/posts/` | Bearer | Create post (`hub`, `content`, `media_url`) |
| POST | `/api/posts/{id}/like/` | Bearer | Toggle like. Returns `{liked, likes_count}` |
| GET | `/api/hubs/` | Bearer | List hubs (`?interest=id` filter) |
| POST | `/api/hubs/` | Bearer | Create hub |
| GET | `/api/hubs/{id}/` | Bearer | Hub detail (includes `main_challenge`, `is_member`) |
| GET | `/api/hubs/recommended/` | Bearer | Hubs matching user's interests |
| POST | `/api/hubs/{id}/join/` | Bearer | Join hub |
| POST | `/api/hubs/{id}/leave/` | Bearer | Leave hub |
| GET | `/api/hubs/{id}/posts/` | Bearer | Posts in a hub (403 for private non-members) |
| GET | `/api/hubs/{id}/challenges/` | Bearer | List challenges in hub |
| POST | `/api/hubs/{id}/challenges/` | Bearer | Create sub-challenge (members only) |
| POST | `/api/challenges/{id}/progress/` | Bearer | Increment progress by 1 |

### Standard Response Shapes

**Auth response (login / signup / social-login):**
```json
{
  "access_token": "...",
  "refresh_token": "...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "username": "user1",
    "full_name": "User One",
    "date_of_birth": "1999-01-01",
    "phone_number": "+1234",
    "profile_picture_url": null,
    "interests": [1, 2]
  }
}
```

**Paginated list response:**
```json
{
  "count": 42,
  "next": "http://.../api/posts/feed/?page=2",
  "previous": null,
  "results": [...]
}
```

**Post object:**
```json
{
  "id": 1,
  "author": 1,
  "author_username": "user1",
  "author_avatar_url": "http://...",
  "hub": 3,
  "hub_name": "Marathon 2025",
  "content": "Completed 10km today!",
  "media_url": "",
  "is_public": true,
  "created_at": "2026-06-13T10:00:00Z",
  "updated_at": "2026-06-13T10:00:00Z",
  "likes_count": 12,
  "liked_by_me": false
}
```

**Hub object:**
```json
{
  "id": 3,
  "name": "Marathon 2025",
  "description": "Run a full marathon by December",
  "category": 1,
  "category_name": "Fitness",
  "members_count": 47,
  "cover_image_url": "",
  "is_public": true,
  "created_by": 1,
  "created_at": "2026-01-01T00:00:00Z",
  "is_member": true,
  "main_challenge": {
    "title": "Complete a full marathon",
    "target_count": 1,
    "user_progress": 0
  }
}
```

---

## 8. Android App — Current Structure

### Navigation
Single `MainActivity` → Navigation Component → 5 bottom-nav destinations:

| Tab | Fragment | Status |
|-----|----------|--------|
| Home | `HomeFragment` | Working — loads feed, shows posts with like toggle |
| Explore | `ExploreFragment` | Stub — empty |
| Create | `CreatePostFragment` | Working — creates post in a hub |
| Hubs | `HubsFragment` (ViewPager2) | Working — "My Hubs" + "Explore Hubs" tabs |
| Profile | `ProfileFragment` | Working — shows username, basic info |

### Auth Flow
`LoginActivity` → `SignupStep1Activity` (email/pass) → `SignupStep2Activity` (username/name/DOB) → `SignupStep3ProfileSetupActivity` (interests + photo) → `MainActivity`

### Data Layer
```
data/
  api/
    AuthApiService.kt       — signup, login, social, logout, check-*
    FeedApiService.kt       — posts feed, global posts, like toggle
    HubApiService.kt        — hubs CRUD, join/leave, hub posts, challenges
    RetrofitClient.kt       — single Retrofit instance, JWT interceptor
  local/
    UserPreferences.kt      — DataStore, stores access_token, refresh_token, user JSON
  model/
    User.kt, Hub.kt, Post.kt, AuthResponse.kt, ...
  repository/
    AuthRepository.kt
    HubRepository.kt
    PostRepository.kt
```

---

## 9. Detailed Feature Specifications

### 6.1 Home Screen

**What it is:** An Instagram-style feed. The primary content space.

**What it shows:**
1. Posts from hubs the user has joined (already implemented via `/api/posts/feed/`)
2. Hub announcements — when a hub admin reposts a user's post to the global home, it appears here with the hub name + original username as a header
3. Personal posts from the user themselves

**Post card design:**
- Author avatar + username + hub name badge (e.g., "in Marathon 2025")
- Timestamp
- Text content
- Media (image or video player, inline)
- Like button (with count) + Comment button (with count) + Share button
- If this is a hub announcement repost: a "Reposted by [Hub]" header above the card

**Rules:**
- Feed is paginated (10 per page, load-more on scroll)
- Video posts autoplay on scroll into view (muted), unmute on tap
- Liked state persists optimistically (toggle without waiting for server)

---

### 6.2 Explore Screen

**What it is:** A TikTok / Instagram Reels style full-screen vertical video scroller.

**Two tabs (top, like TikTok):**

**"For You" tab:**
- Full-screen short videos from any public hub, or personal video posts
- Personalized by interest matching
- Swipe up/down to navigate
- Overlay UI (bottom): username, hub name badge, caption snippet, like/comment/share buttons (right side), follow/join button

**"Your Hubs" tab:**
- Same format but only videos from hubs the user has joined
- Gives members a curated video-only view of their hub activity

**Notes:**
- A "video post" is any Post where `media_url` is a video file (mp4 etc.)
- Backend needs a dedicated `/api/posts/explore/` endpoint that returns video-only posts with personalization ordering
- The Android side needs a `ExoPlayer`-based full-screen RecyclerView (PageSnapHelper)

---

### 6.3 Create Screen

**What it is:** The central creation hub. Tapping "+" opens this.

**Options displayed:**
1. **New Post** — text + optional image/video, must pick a hub (from user's joined hubs)
2. **Progress Update** — special post type tied to a challenge: shows current progress bar, "+1" increment confirmation, adds a post documenting the update
3. **New Hub** (if user has permission) — opens the Create Hub flow

**Post creation flow:**
1. Pick hub (required — no personal posts outside a hub in this model)
2. Write content
3. Optionally attach media (image or video, from camera or gallery)
4. Set visibility (public / members-only)
5. Post

**Media handling:**
- Images: compress before upload, upload to server as multipart
- Videos: same — backend stores file locally (dev), CDN (prod)
- `media_url` on the Post model must become a file field on the backend, not just a URL

---

### 6.4 Hubs Screen

**Two tabs:**

**"Your Hubs" tab:**
- List of hubs user has joined
- Each card shows: hub name, category, cover image, member count, main challenge title + user's current progress bar
- Tap → Hub Detail

**"Explore Hubs" tab:**
- Discover hubs not yet joined
- Search bar at top
- Filter by interest/category
- Recommendation section (from user interests, uses `/api/hubs/recommended/`)
- Cards show: name, category, member count, description preview, "Join" button

#### Hub Detail Screen

**Header:**
- Cover image
- Hub name + category badge
- Member count
- Join/Leave button (non-admin)

**Tabs inside hub detail:**
1. **Posts** — paginated list of posts in this hub (`/api/hubs/{id}/posts/`)
2. **Challenges** — list of active challenges with progress bars for current user
3. **Members** — list of hub members (needs `/api/hubs/{id}/members/` endpoint)
4. **About** — description, creation date, admin info

**Hub roles:**
- **Creator / Admin** — can edit hub details, set main challenge, repost user content to home, approve achievements, invite members
- **Member** — can post, track challenges, react
- (Future: Moderator role)

---

### 6.5 Profile Screen

**Header:**
- Profile picture (tappable to upload new)
- Full name + username
- Follower/Following count (not yet implemented)
- Joined hubs count
- Edit Profile button (own profile) / Follow button (other user)

**Tabs:**
1. **Posts** — user's own posts (grid or list view)
2. **Progress** — all challenges user is participating in across hubs, with progress bars
3. **Achievements** — verified completed goals (with badge/medal display)
4. **Liked** — posts user has liked

**Settings (gear icon):**
- Edit profile (name, photo, bio, interests)
- Notification settings
- Privacy settings
- Logout

---

### 6.6 Achievement & Verification System

This is the app's most unique feature.

**Flow:**
1. A user completes a challenge (`current_count >= target_count`)
2. The system marks it as "pending verification"
3. The hub admin (or all members via a vote mechanism — TBD) reviews the user's posts and progress updates as evidence
4. If verified: the achievement is stamped as `verified = True`
5. A **Hub Event** is broadcast to all hub members: "🏆 [Username] completed [Challenge]!"
6. The event appears:
   - In the home feed for all hub members
   - As a pinned post/announcement in the hub
   - On the user's Profile > Achievements tab with a badge/medal
7. Optional: physical/digital awards (future feature — could be NFT badge, printed certificate, etc.)

**Data model needed (not yet built):**
- `Achievement` — links User + Challenge, has `verified`, `verified_at`, `verified_by`, `broadcast_post` FK
- `HubEvent` — a special post type that is an announcement (type: `achievement`, `announcement`, `milestone`)

---

## 10. Gaps — What Needs to Be Built

### 7.1 Backend Gaps

| Feature | What's needed |
|---------|--------------|
| Comments | New `Comment` model (FK to Post + FK to User), `CommentLike`, endpoints: list + create + delete |
| Post media as file | Change `Post.media_url` from URLField to `FileField` + multipart upload endpoint |
| Hub cover image as file | Change `Hub.cover_image_url` from URLField to `FileField` |
| Hub roles | `HubMembership.role` field: `member` / `admin` / `moderator` |
| Hub members list | `GET /api/hubs/{id}/members/` endpoint |
| Explore video feed | `GET /api/posts/explore/` — video-only, personalized ordering |
| User profile update | `PATCH /api/users/{id}/profile/` endpoint |
| Notifications | `Notification` model + `GET /api/notifications/` |
| Achievement system | `Achievement` model + verification endpoints |
| Hub Events | `HubEvent` model or Post type flag |
| Repost / Hub announcement | Admin can repost a post to the hub announcement level |
| Follow system | `UserFollow` model (user follows another user, not just hubs) |
| Search | `/api/search/?q=` for hubs + users |
| Post comments count | `comments_count` is already in the Android Post model but not returned by the API |
| Admin-only challenge creation | `is_main` challenge should only be createable by the hub creator |

### 7.2 Android Gaps

| Feature | What's needed |
|---------|--------------|
| Explore screen | Full ExoPlayer-based video scroll (TikTok style) |
| Comments screen | Bottom sheet or full screen comment list with input |
| Profile edit | Edit screen for name, bio, photo, interests |
| Notifications tab/bell | Bell icon in toolbar, notification list screen |
| Hub admin panel | Edit hub name/desc/cover, manage challenges, repost, approve achievements |
| Achievement display | Achievements tab on profile with badge UI |
| Media upload | Camera/gallery picker, upload flow for posts and profile picture (currently limited) |
| Search | Hub + user search screen |
| User profile view | Tapping another user's name → their profile |
| Token auto-refresh | Retrofit interceptor to auto-refresh JWT on 401 |
| Error states | Proper empty state + error retry UI in all lists |
| Loading skeletons | Placeholder UI while content loads |

---

## 11. Database Models To Add Next (Priority Order)

### Phase 2 Additions

```
community/models.py additions:

Comment
  - post → FK(Post, CASCADE, related_name='comments')
  - author → FK(User, CASCADE)
  - content → TextField
  - created_at → DateTimeField(auto_now_add)
  - unique: none (multiple comments per user/post ok)

Achievement
  - user → FK(User, CASCADE)
  - challenge → FK(Challenge, CASCADE)
  - verified → BooleanField(default=False)
  - verified_at → DateTimeField(null, blank)
  - verified_by → FK(User, null, blank, related_name='verifications')
  - broadcast_post → FK(Post, null, blank) ← the hub event post
  - created_at → DateTimeField(auto_now_add)
  - unique_together: (user, challenge)

HubEvent (a special type of post for broadcast announcements)
  - This can be modeled as a new field on Post:
    post_type: CharField choices = ('regular', 'achievement_broadcast', 'admin_announcement')
  - Or as a separate model. Decision: add post_type to Post model.

HubMembership (update existing):
  - Add role: CharField choices=('member','admin','moderator'), default='member'
  - On hub creation, creator gets role='admin'

accounts/models.py additions:

UserFollow
  - follower → FK(User, CASCADE, related_name='following')
  - following → FK(User, CASCADE, related_name='followers')
  - created_at → DateTimeField(auto_now_add)
  - unique_together: (follower, following)

Notification
  - recipient → FK(User, CASCADE)
  - actor → FK(User, null) ← who triggered it
  - verb → CharField(100) ← 'liked your post', 'joined your hub', etc.
  - target_type → CharField(50) ← 'post', 'hub', 'achievement'
  - target_id → IntegerField
  - is_read → BooleanField(default=False)
  - created_at → DateTimeField(auto_now_add)
```

---

## 12. API Endpoints To Add Next (Priority Order)

### Phase 2 Endpoints

```
Comments:
  GET  /api/posts/{id}/comments/         — list comments, paginated
  POST /api/posts/{id}/comments/         — create comment
  DELETE /api/comments/{id}/            — delete own comment

Profile:
  GET    /api/users/{id}/profile/        — get any user's profile
  PATCH  /api/users/{id}/profile/        — update own profile (name, bio, dob, interests)

Hub admin:
  PATCH  /api/hubs/{id}/                 — edit hub (admin only)
  GET    /api/hubs/{id}/members/         — list members with roles
  POST   /api/hubs/{id}/challenges/main/ — set/update main challenge (admin only)
  POST   /api/posts/{id}/repost-to-hub/  — admin reposts a user post as hub announcement

Achievements:
  GET    /api/achievements/              — current user's achievements
  GET    /api/users/{id}/achievements/   — another user's achievements
  POST   /api/achievements/{id}/verify/  — admin verifies an achievement

Explore feed:
  GET    /api/posts/explore/             — video posts, personalized
  GET    /api/posts/explore/hubs/        — video posts from user's hubs only

Search:
  GET    /api/search/?q=&type=hubs|users|posts

Notifications:
  GET    /api/notifications/
  POST   /api/notifications/{id}/read/
  POST   /api/notifications/read-all/

Follow:
  POST   /api/users/{id}/follow/
  POST   /api/users/{id}/unfollow/
  GET    /api/users/{id}/followers/
  GET    /api/users/{id}/following/
```

---

## 13. Development Phases

### Phase 1 — Core Social Loop (Current state + completion) ✅ Mostly Done
- [x] Auth (signup, login, social, JWT)
- [x] Hub CRUD (create, list, detail, join/leave)
- [x] Post creation and feed
- [x] Like toggle
- [x] Challenges + progress tracking
- [x] Android: Home feed, Hubs screen, Create post, Basic profile
- [ ] Token auto-refresh (Retrofit interceptor on 401)
- [ ] Media file upload for posts (currently URL only)
- [ ] `comments_count` returned from API

### Phase 2 — Comments & Profile
- [ ] Comments model + API + Android UI
- [ ] Profile edit screen (name, bio, interests, photo)
- [ ] User profile view (tapping a username)
- [ ] Hub member roles (admin vs member)
- [ ] Hub admin: edit hub, manage challenges

### Phase 3 — Explore (Short Videos)
- [ ] Backend: Explore feed endpoint (video-only, personalized)
- [ ] Android: ExoPlayer full-screen vertical scroll
- [ ] Post type refinement (video vs image vs text)

### Phase 4 — Achievements & Verification
- [ ] Achievement model + API
- [ ] Verification flow (admin reviews evidence, approves)
- [ ] Achievement broadcast as hub event post
- [ ] Achievement badges on profile

### Phase 5 — Social Graph & Discovery
- [ ] Follow/unfollow users
- [ ] Search (hubs, users)
- [ ] Notifications (in-app)
- [ ] Hub announcements / admin repost

### Phase 6 — Polish & Production
- [ ] Replace SQLite → PostgreSQL
- [ ] Replace local media → cloud storage (S3 or Cloudinary)
- [ ] Push notifications (Firebase)
- [ ] Social login signature verification (not just JWT decode)
- [ ] Rate limiting, spam prevention
- [ ] CORS configuration for production

---

## 14. Conventions & Rules

These rules apply to every new feature we build. **Never deviate without updating this document.**

### Backend Rules
1. All endpoints require `Authorization: Bearer <token>` unless explicitly marked as public (auth endpoints only).
2. All list endpoints use the `DefaultPagination` class: page_size=10, max=50.
3. Permission checks: use `ensure_hub_access()` pattern for any resource that belongs to a hub.
4. Denormalized counters (`members_count`, `likes_count`, `comments_count`) are updated with `F()` expressions, never by reading first.
5. Models use `BigAutoField` (already set as `DEFAULT_AUTO_FIELD`).
6. Every model with user-generated content must have `created_at` (auto_now_add) and `updated_at` (auto_now) except simple join tables.
7. Error responses use DRF standard format: `{"detail": "message"}` or `{"field": ["error"]}`.
8. The `is_main` challenge on a hub can only be set by the hub creator.
9. Never store raw passwords — always use `create_user()` or `set_unusable_password()`.

### Android Rules
1. All ViewModels expose `StateFlow` or `LiveData`. Never do network calls in fragments directly.
2. All API calls go through a Repository. Fragments only talk to ViewModels.
3. JWT token is stored in DataStore, never in SharedPreferences or memory only.
4. A global Retrofit interceptor must handle 401 by attempting a token refresh before failing.
5. All RecyclerViews that load from the API must implement load-more (pagination).
6. Fragment backstack: pop rather than replace when navigating to detail screens.
7. Hub ID is always passed via `Bundle`/`SafeArgs`, never fetched again from scratch.

### Naming Conventions
| Layer | Convention | Example |
|-------|-----------|---------|
| Django models | PascalCase, singular | `HubMembership` |
| Django views | PascalCase + View/ListView | `HubDetailView` |
| Django URLs | kebab-case | `hubs-detail` |
| API paths | kebab-case, plural resource | `/api/hubs/{id}/members/` |
| Kotlin classes | PascalCase | `HubDetailFragment` |
| Kotlin files | PascalCase matching class | `HubDetailFragment.kt` |
| Kotlin ViewModels | `<Screen>ViewModel` | `HubDetailViewModel` |
| JSON fields | snake_case | `members_count` |
| Kotlin model fields | camelCase with `@SerializedName` | `membersCount` |

---

## 15. Key Design Decisions (Already Made)

1. **Every post belongs to a hub.** There are no free-floating personal posts. This keeps content anchored to goals. (Personal expression happens inside a hub's context.)
2. **`members_count` is a denormalized counter** on Hub, not counted via JOIN every time.
3. **JWT refresh token blacklisting** is enabled — logout truly invalidates the session.
4. **Media URLs vs file fields**: currently `media_url` is a URLField on Post. This works for linking external media but will need to become a `FileField` when we build in-app upload. We will add a separate `POST /api/posts/{id}/media/` multipart upload endpoint and keep `media_url` for the absolute URL, OR migrate `media_url` to `FileField`. Decision: migrate to `FileField` in Phase 2.
5. **Hub `cover_image_url` is a URLField** currently. Same migration plan as above.
6. **SQLite in dev.** Migrations must be PostgreSQL-compatible from the start (no SQLite-specific SQL).
7. **The Explore tab** is TikTok-style video-only, NOT a photo grid like Instagram Explore. This is intentional to emphasize short-form video proof of progress.
8. **Achievement verification** is admin-gated (hub creator approves), not crowd-sourced voting. This is simpler and avoids spam.

---

## 16. Open Questions (Decide Before Building These Features)

1. **Can a user belong to unlimited hubs, or is there a cap?** — A cap (e.g., 20) prevents spammy memberships and keeps the hub concept meaningful.
2. **Can a user post in a hub without having any progress on the main challenge?** — Currently yes. Consider requiring at least one progress update before a "progress post" but keeping general posts free.
3. **Is "follow user" separate from "join hub"?** — Currently there is no user-follow. We may add it in Phase 5. Until then, social graph = hub membership.
4. **What happens when a hub creator leaves?** — Admin transfer logic needed. Currently the creator can leave (no protection), which would leave the hub admin-less.
5. **Video size limit and compression?** — Decide server-side max (e.g., 100MB) and whether we transcode on the backend or leave that to the client.
6. **Explore feed algorithm?** — For MVP: reverse chronological, filtered to user interests. For v2: engagement-weighted ranking.

---

*Last updated: 2026-06-13*
*This document is the master reference. Update it whenever a design decision changes.*
