# HubRise — Complete Project Documentation
## Full Codebase Explainer for Senior Project Report & Presentation

> **Student:** Hadi Yazbek
> **Supervisor:** Dr. Mohamad Rida Mortada
> **Department:** Computer Science, Engineering
> **Academic Year:** 2025–2026
> **GitHub:** https://github.com/hadiyazbek2/HubRise

---

## TABLE OF CONTENTS

1. [Vision & Concept](#1-vision--concept)
2. [What Makes HubRise Different (Related Work)](#2-what-makes-hubrise-different-related-work)
3. [System Architecture](#3-system-architecture)
4. [Tech Stack (Full Details)](#4-tech-stack-full-details)
5. [Database Design & Models](#5-database-design--models)
6. [Backend — Django REST API](#6-backend--django-rest-api)
7. [All API Endpoints (Complete Reference)](#7-all-api-endpoints-complete-reference)
8. [Android Frontend — Kotlin MVVM](#8-android-frontend--kotlin-mvvm)
9. [Feature-by-Feature Walkthrough](#9-feature-by-feature-walkthrough)
10. [Key Code Highlights & Explanations](#10-key-code-highlights--explanations)
11. [App Flow & Navigation](#11-app-flow--navigation)
12. [Limitations](#12-limitations)
13. [Future Work](#13-future-work)
14. [References](#14-references)

---

## 1. Vision & Concept

### Elevator Pitch
HubRise is a **goal-oriented social network** for Android. The core idea: instead of scrolling through entertainment, users join **Hubs** — communities built around a shared real-world goal — and hold each other accountable through progress posts, measurable challenges, peer validation, and verified achievement broadcasts.

Think of it as: **Instagram's social feed + Strava's goal tracking + Discord's communities**, but built for ANY goal, not just fitness.

### The Core Problem
Existing social networks give users an audience but not a purpose:
- **Instagram/BeReal**: passive content, no goal structure, no accountability
- **Strava**: great for fitness tracking, but limited to physical activity only
- **Habitica**: gamifies habit tracking but social layer is weak and shallow
- **Discord**: excellent communities but no progress tracking or achievement system

None of these platforms allow you to join a community, set a structured goal, track measurable progress, get peer validation, and broadcast verified achievements — all in one place.

### The Solution: HubRise
HubRise solves this by organizing everything around **goals**:
- Every user joins **Hubs** built around a shared objective
- Every Hub can run **Challenges** in 3 flexible models (stage, count, streak)
- Every challenge submission passes through a **3-layer verification system** (self-submit → peer micro-validation → admin final review)
- Achievements are **broadcast to the community** as special announcement posts
- All of this is wrapped in a familiar **social feed** with likes, comments, follow/following, and notifications

---

## 2. What Makes HubRise Different (Related Work)

| Feature | Strava | Habitica | Instagram | HubRise |
|---|---|---|---|---|
| Any goal type | ❌ Fitness only | ❌ Habits only | ❌ None | ✅ Any goal |
| Social feed | ✅ | ❌ | ✅ | ✅ |
| Peer validation | ❌ | ❌ | ❌ | ✅ |
| Structured challenges | Partial | ✅ | ❌ | ✅ |
| 3 challenge models | ❌ | ❌ | ❌ | ✅ |
| Admin-verified achievement | ❌ | ❌ | ❌ | ✅ |
| Community roles (admin/mod) | ❌ | Partial | ❌ | ✅ |
| Achievement broadcast post | ❌ | ❌ | ❌ | ✅ |
| Comments + notifications | ❌ | ❌ | ✅ | ✅ |

### HubRise's Unique Contributions
1. **3-model challenge system** — stage-based, count-based, and streak-based challenges in one platform
2. **Peer micro-validation** — community members can mark a progress post as "trusted," adding social accountability
3. **3-layer completion flow** — self-submission → peer validation score → admin final approval
4. **Achievement broadcast** — completing a challenge auto-publishes a special post to the Hub feed, celebrating the achievement publicly
5. **Wishlist URL on profiles** — supporters can send gifts to challenge completers via a wishlist link
6. **Challenge Templates** — 8 official pre-built templates (fitness, finance, reading, mindfulness, etc.) to accelerate Hub creation

---

## 3. System Architecture

### High-Level Overview

```
┌─────────────────────────────────┐        HTTP/REST         ┌──────────────────────────┐
│       Android App (Kotlin)      │ ◄──────────────────────► │   Django Backend (Python) │
│                                 │   Bearer JWT Token        │                           │
│  ┌─────────┐  ┌──────────────┐  │                           │  ┌────────┐  ┌─────────┐ │
│  │   UI    │  │  ViewModel   │  │                           │  │  DRF   │  │  JWT    │ │
│  │Fragments│  │  LiveData    │  │                           │  │  Views │  │  Auth   │ │
│  └────┬────┘  └──────┬───────┘  │                           │  └───┬────┘  └────┬────┘ │
│       │              │          │                           │      │            │      │
│  ┌────▼──────────────▼───────┐  │                           │  ┌───▼────────────▼────┐ │
│  │      Repository Layer     │  │                           │  │    Django ORM       │ │
│  └────────────┬──────────────┘  │                           │  └──────────┬──────────┘ │
│               │                 │                           │             │            │
│  ┌────────────▼──────────────┐  │                           │  ┌──────────▼──────────┐ │
│  │  Retrofit + OkHttp Client │  │                           │  │   SQLite Database   │ │
│  │  (tokenInterceptor +      │  │                           │  │  (→ PostgreSQL prod)│ │
│  │   tokenAuthenticator)     │  │                           │  └─────────────────────┘ │
│  └───────────────────────────┘  │                           │                           │
│                                 │                           │  ┌─────────────────────┐  │
│  ┌───────────────────────────┐  │                           │  │  Media File Storage │  │
│  │  Android DataStore        │  │                           │  │  (local filesystem) │  │
│  │  (JWT tokens + user info) │  │                           │  └─────────────────────┘  │
│  └───────────────────────────┘  │                           └──────────────────────────┘
└─────────────────────────────────┘
```

### Architecture Pattern
- **Backend:** MVC via Django REST Framework (Models → Serializers → Views → URLs)
- **Android:** MVVM (Model-View-ViewModel) + Repository Pattern
  - **View** (Fragments/Activities) observes LiveData from ViewModel
  - **ViewModel** calls Repository methods, holds UI state
  - **Repository** calls Retrofit API services, returns sealed Result objects
  - **Model** = Kotlin data classes matching backend JSON

### Data Flow Example (Loading the Home Feed)
```
HomeFragment.onViewCreated()
  └─► HomeViewModel.loadFeed()
        └─► PostRepository.getFeed(page=1)
              └─► FeedApiService.getFeed()         [Retrofit HTTP call]
                    └─► GET /community/posts/feed/  [Django FeedPostsView]
                          └─► PostSerializer → JSON response
                    ◄── PaginatedResponse<Post>
              ◄── Result.Success(data)
        ◄── _posts.value = data.results            [LiveData update]
  ◄── Observer triggers → PostAdapter.submitList() [RecyclerView updates]
```

---

## 4. Tech Stack (Full Details)

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Python | 3.12 | Backend language |
| Django | 6.x | Web framework |
| Django REST Framework (DRF) | 3.x | REST API toolkit |
| djangorestframework-simplejwt | Latest | JWT authentication + token blacklisting |
| Pillow | Latest | Profile picture & hub cover image processing |
| SQLite | Built-in | Development database |
| PostgreSQL | Planned | Production database |

### Android
| Technology | Purpose |
|---|---|
| Kotlin | Primary Android language |
| Android SDK | Android APIs |
| MVVM Architecture | Separation of concerns (UI / logic / data) |
| LiveData | Reactive UI state management |
| ViewModelScope + Coroutines | Async operations without callback hell |
| Retrofit 2 | Type-safe HTTP client |
| OkHttp 4 | HTTP interceptors (token injection, auto-refresh) |
| Gson | JSON serialization/deserialization |
| Jetpack Navigation Component | Single-activity navigation with back stack |
| Android DataStore (Preferences) | Secure, async, coroutine-native token storage |
| Glide | Image loading, caching, and display |
| ViewPager2 | Tab-based screens (Hubs: My Hubs / Explore) |
| BottomSheetDialogFragment | Comments overlay |
| RecyclerView + DiffUtil | Efficient list rendering |
| MultipartBody (OkHttp) | Media file upload (profile picture, post media) |

### Development Tools
| Tool | Purpose |
|---|---|
| Android Studio | Android IDE |
| VS Code / PyCharm | Backend development |
| Postman | API testing |
| Git + GitHub | Version control |
| Django Admin | Backend data management |

---

## 5. Database Design & Models

### Accounts App Models

#### User (Django built-in)
Standard Django User model. Fields used: `id`, `username`, `email`, `password` (hashed).

#### UserProfile (1-to-1 with User)
```
UserProfile
├── user          → OneToOne → User
├── full_name     CharField(255)
├── bio           CharField(200, blank)
├── date_of_birth DateField
├── phone_number  CharField(30, blank)
├── profile_picture FileField(upload_to="profile_pictures/")
├── interests     ManyToMany → Interest
├── wishlist_url  URLField(500, blank)
└── updated_at    DateTimeField(auto_now)
```

#### Interest
```
Interest
└── name  CharField(100, unique)
```
Predefined categories like "Fitness", "Reading", "Finance", "Mindfulness", etc. Used to tag both user interests and Hubs.

#### UserFollow
```
UserFollow
├── follower   → FK → User
├── following  → FK → User
└── created_at DateTimeField
[CONSTRAINT: unique (follower, following)]
```

#### Notification
```
Notification
├── recipient         → FK → User
├── sender            → FK → User
├── notification_type CharField  ["follow", "like", "comment", 
│                                  "completion_submitted",
│                                  "completion_approved", "completion_rejected"]
├── post              → FK → Post (nullable)
├── challenge         → FK → Challenge (nullable)
├── is_read           BooleanField(default=False)
└── created_at        DateTimeField
```

#### SocialAccount
```
SocialAccount
├── user             → FK → User
├── provider         CharField  ["google", "facebook", "apple"]
├── provider_user_id CharField(255)
├── email            EmailField
├── extra_data       JSONField
└── created_at / updated_at
```

---

### Community App Models

#### Hub
```
Hub
├── name                     CharField(150)
├── description              TextField
├── category                 → FK → Interest (nullable)
├── members_count            PositiveIntegerField (denormalized)
├── cover_image              ImageField(upload_to="hub_covers/")
├── is_public                BooleanField(default=True)
├── created_by               → FK → User
├── peer_validation_threshold DecimalField(default=3.0)
└── created_at               DateTimeField
```
`peer_validation_threshold`: a post needs this weighted validation score to be marked as "Trusted".

#### HubMembership
```
HubMembership
├── user      → FK → User
├── hub       → FK → Hub
├── role      CharField  ["member", "admin", "moderator"]
└── joined_at DateTimeField
[CONSTRAINT: unique (user, hub)]
```
When a user creates a Hub, a HubMembership row is created with role="admin".

#### Post
```
Post
├── author         → FK → User
├── hub            → FK → Hub (nullable — allows personal posts)
├── post_type      CharField  ["regular", "progress_update", 
│                               "achievement_broadcast", "admin_announcement",
│                               "stage_proof", "count_entry", "streak_checkin"]
├── content        TextField
├── media_file     FileField(upload_to="post_media/")
├── is_public      BooleanField(default=True)
├── challenge      → FK → Challenge (nullable, set for auto-generated posts)
├── linked_stage   → FK → ChallengeStage (nullable)
├── weighted_validation_score DecimalField(default=0)  — Layer 1 peer score
├── is_trusted     BooleanField(default=False)          — crossed threshold?
├── trusted_at     DateTimeField (nullable)
└── created_at / updated_at
```
`CHALLENGE_POST_TYPES = [stage_proof, count_entry, streak_checkin]` — these are eligible for peer validation.

#### PostLike
```
PostLike
├── user       → FK → User
├── post       → FK → Post
└── created_at DateTimeField
[CONSTRAINT: unique (user, post)]
```

#### PostValidation (Peer Micro-Validation — Layer 1)
```
PostValidation
├── post       → FK → Post
├── validator  → FK → User
├── weight     DecimalField  (validator's contribution weight)
└── created_at DateTimeField
[CONSTRAINT: unique (post, validator)]
```
When a user validates a post, their weighted vote is added. When `post.weighted_validation_score >= hub.peer_validation_threshold`, the post is marked `is_trusted=True`.

#### Comment
```
Comment
├── post       → FK → Post
├── author     → FK → User
├── content    TextField
└── created_at DateTimeField
[ordering: created_at ascending]
```

#### Challenge
```
Challenge
├── hub            → FK → Hub
├── title          CharField(100)
├── description    TextField
├── progress_model CharField  ["stage", "count", "streak"]
├── is_main        BooleanField(default=False)  — one main challenge per hub
├── created_by     → FK → User
├── ends_at        DateTimeField (nullable)
└── created_at     DateTimeField
```

#### ChallengeStage (for progress_model="stage")
```
ChallengeStage
├── challenge    → FK → Challenge
├── order_index  PositiveIntegerField
├── title        CharField(120)
├── description  TextField
├── proof_type   CharField  ["photo", "video", "text", "number", "any"]
├── proof_prompt CharField(255)  — instruction shown to member
├── is_milestone BooleanField
└── created_at   DateTimeField
[CONSTRAINT: unique (challenge, order_index)]
[ordering: order_index]
```

#### UserStageProgress
```
UserStageProgress
├── user        → FK → User
├── stage       → FK → ChallengeStage
├── status      CharField  ["not_started", "in_progress", "completed"]
├── proof_post  → FK → Post (nullable)
├── completed_at DateTimeField (nullable)
└── created_at / updated_at
[CONSTRAINT: unique (user, stage)]
```

#### ChallengeCountConfig (for progress_model="count")
```
ChallengeCountConfig
├── challenge             → OneToOne → Challenge
├── target_count          DecimalField
├── unit_label            CharField(50)  e.g. "km", "books", "$"
├── entry_increment       DecimalField(default=1)
├── require_proof_per_entry BooleanField
└── is_cumulative         BooleanField
    False = each log ADDS to total (books read: log +1 each time)
    True  = each log IS the new total (km run: log current cumulative km)
```

#### UserCountProgress
```
UserCountProgress
├── user          → FK → User
├── challenge     → FK → Challenge
├── current_count DecimalField
├── is_complete   BooleanField
├── completed_at  DateTimeField (nullable)
└── updated_at    DateTimeField
[CONSTRAINT: unique (user, challenge)]
```

#### ChallengeStreakConfig (for progress_model="streak")
```
ChallengeStreakConfig
├── challenge    → OneToOne → Challenge
├── target_days  PositiveIntegerField
├── frequency    CharField  ["daily", "weekly"]
├── grace_days   PositiveIntegerField(default=0)
└── require_proof BooleanField
```

#### UserStreakProgress
```
UserStreakProgress
├── user             → FK → User
├── challenge        → FK → Challenge
├── current_streak   PositiveIntegerField
├── longest_streak   PositiveIntegerField
├── total_checkins   PositiveIntegerField
├── checkin_calendar JSONField  [list of "YYYY-MM-DD" strings]
├── last_checkin_date DateField (nullable)
└── is_complete      BooleanField
[CONSTRAINT: unique (user, challenge)]
```
`checkin_calendar` is a JSON array of date strings used to render the calendar heatmap on the Android ChallengeDetail screen.

#### CompletionRequest (Layer 3 — Admin Final Review)
```
CompletionRequest
├── user             → FK → User
├── challenge        → FK → Challenge
├── status           CharField  ["pending", "approved", "rejected"]
├── member_note      TextField  (member's self-assessment)
├── submitted_at     DateTimeField
├── reviewed_at      DateTimeField (nullable)
├── reviewed_by      → FK → User (nullable, the admin)
├── admin_note       TextField  (admin feedback)
└── announcement_post → FK → Post (nullable, auto-created on approval)
[CONSTRAINT: unique pending request per (user, challenge)]
```

#### ChallengeTemplate & TemplateStage
```
ChallengeTemplate
├── name           CharField(120)
├── category       CharField  ["fitness", "finance", "learning", "reading",
│                               "mindfulness", "creative", "career", "other"]
├── progress_model CharField  ["stage", "count", "streak"]
├── description    TextField
├── is_official    BooleanField  (pre-loaded by the platform)
├── use_count      PositiveIntegerField  (incremented each time used)
├── count_target / count_unit_label / count_entry_increment
├── streak_target_days / streak_frequency / streak_grace_days
└── created_at

TemplateStage
├── template    → FK → ChallengeTemplate
├── order_index PositiveIntegerField
├── title       CharField(120)
├── description TextField
└── proof_type  CharField
```
When a Hub admin applies a template to create a challenge, the template's stages/config are **copied** into the new Challenge rows. The new rows are never linked back to the template, keeping the template read-only.

---

## 6. Backend — Django REST API

### Project Structure
```
backend/
├── backend/           ← Django project (settings, urls, wsgi)
│   ├── settings.py
│   └── urls.py        ← includes accounts/ and community/ URL namespaces
├── accounts/          ← Auth, profiles, follow, notifications
│   ├── models.py
│   ├── views.py
│   ├── serializers.py
│   └── urls.py
└── community/         ← Hubs, posts, challenges, comments
    ├── models.py
    ├── views.py
    ├── serializers.py
    └── urls.py
```

### Authentication System
- Uses `djangorestframework-simplejwt`
- Access token TTL: **30 minutes**
- Refresh token TTL: **7 days**
- Token blacklisting enabled — on logout, the refresh token is blacklisted in the database so it cannot be reused
- All community endpoints require `IsAuthenticated` (Bearer token in Authorization header)
- Auth endpoints (signup, login, social-login, check-email, check-username) use `AllowAny`

### Key View Logic

#### `build_auth_response(user, request)` — accounts/views.py
This helper function is the central auth response builder. Called by SignupView, LoginView, and SocialLoginView. It generates a JWT pair using `RefreshToken.for_user(user)` and returns a consistent dict with `access_token`, `refresh_token`, and the full serialized `user` object. By centralizing this, all three auth flows return identical JSON, allowing the Android client to use a single `AuthResponse` data class for all of them.

#### `FeedPostsView` — community/views.py
Returns a personalized post feed. Filters posts where:
- The post's hub is one the current user is a member of, OR
- The post is from a user the current user follows
Ordered by `created_at` descending. Paginated (page_size=10, max=50).

#### `GlobalPostsView` — community/views.py
Returns all public posts from all public Hubs. No membership requirement. Used as the "Discover" feed tab.

#### `ToggleLikeView` — community/views.py
Toggles like on a post. Uses `get_or_create` — if the like exists, delete it (unlike); if not, create it (like). Updates `post.likes_count` using `F()` expressions (database-level atomic increment/decrement, avoids race conditions). Creates a Notification of type "like" for the post author. Returns `{"liked": bool, "likes_count": int}`.

#### `PostValidateView` — community/views.py
Handles peer micro-validation. Only callable on challenge-type posts (`stage_proof`, `count_entry`, `streak_checkin`). Creates a `PostValidation` row with the validator's weight. After each validation, the post's `weighted_validation_score` is recalculated. If the score crosses the hub's `peer_validation_threshold`, the post is marked `is_trusted=True` and `trusted_at` is set.

#### `ChallengeListCreateView` — community/views.py
GET: lists all challenges for a hub, with each challenge annotated with the current user's progress (percent_complete, summary) via `get_challenge_progress()` in serializers.py.
POST: creates a new challenge. If a `template_id` is provided, copies the template's stages/config into the new challenge rows (template remains untouched). Supports all 3 progress models and their respective config objects.

#### `CompletionRequestReviewView` — community/views.py
Only accessible to hub admins. PATCH to approve or reject a pending CompletionRequest. On **approval**: creates an `achievement_broadcast` Post in the hub feed, links it to the CompletionRequest, marks the user's progress as complete, creates a `completion_approved` Notification for the member. On **rejection**: creates a `completion_rejected` Notification with the admin note.

#### `HubCompletionRequestsListView` — community/views.py
Returns all pending CompletionRequests for all challenges in a given Hub. Only accessible to hub admins/moderators. Used by the CompletionRequestsFragment on Android.

#### `SearchView` — community/views.py
Single endpoint that accepts a `q` query parameter and searches across:
- Users (by username or full_name)
- Hubs (by name or description)
- Posts (by content)
- Challenges (by title or description)
Returns a combined JSON object: `{"users": [...], "hubs": [...], "posts": [...], "challenges": [...]}`.

#### `RecommendedHubsView` — community/views.py
Returns Hubs that match the current user's registered interests (via UserProfile.interests). Excludes Hubs the user is already a member of. Falls back to most popular Hubs if no interest match found.

#### `ChallengeLeaderboardView` — community/views.py
Returns top members ranked by progress for a given challenge. Calculates ranking differently per progress model: stage (stages completed), count (current_count), streak (total_checkins).

#### `DefaultPagination` — community/views.py
```python
class DefaultPagination(PageNumberPagination):
    page_size = 10
    page_size_query_param = "page_size"
    max_page_size = 50
```
All list endpoints use this. Response format:
```json
{
  "count": 42,
  "next": "http://host/endpoint/?page=2",
  "previous": null,
  "results": [...]
}
```

### Serializers

#### `PostSerializer`
The richest serializer in the project. Includes:
- `author_username`, `author_avatar_url` — denormalized from related profile
- `hub_name` — denormalized from hub
- `likes_count` — from related PostLike set
- `comments_count` — from related Comment set
- `liked_by_me` — checks if current user's id is in the likes
- `validated_by_me` — checks if current user has a PostValidation row for this post
- `validations_count` — from related PostValidation set
- `is_trusted` — whether peer validation crossed the threshold
- `media_url` — absolute URL built with `request.build_absolute_uri()`
- `media_type` — auto-detected from file extension ("image" or "video")
- `challenge_title` — denormalized from linked challenge
- `author_wishlist_url` — from author's UserProfile, shown on achievement posts

#### `get_challenge_progress(challenge, user)` — serializers.py
A standalone helper function that returns `(percent_complete: int, summary: str)` for any challenge, regardless of model:
- Stage: `"3/5 stages"`, percent = completed/total
- Count: `"45.0/100.0 km"`, percent = current/target
- Streak: `"12/30 days"`, percent = total_checkins/target_days

This function is called by `ChallengeSerializer` and `MainChallengeDetailSerializer`, making the progress display model-agnostic.

#### `HubSerializer`
Includes computed fields:
- `is_member` — is the current user a member of this hub?
- `is_creator` — is the current user the hub creator?
- `main_challenge` — the hub's primary challenge with the user's progress embedded
- `cover_image_url` — absolute URL
- `category_name` — from the related Interest

---

## 7. All API Endpoints (Complete Reference)

### Base URL
- **Android Emulator:** `http://10.0.2.2:8000/`
- **Physical Device:** `http://192.168.3.230:8000/`
- All community endpoints prefixed with `/community/`
- All auth endpoints prefixed with `/accounts/`

### Authentication Endpoints (`/accounts/`)
| Method | URL | Auth | Description |
|---|---|---|---|
| POST | `/accounts/signup/` | None | Register new user, returns JWT pair + user object |
| POST | `/accounts/login/` | None | Login, returns JWT pair + user object |
| POST | `/accounts/logout/` | Bearer | Blacklists refresh token |
| POST | `/accounts/social-login/` | None | Login via Google/Facebook/Apple |
| GET | `/accounts/check-email/?email=X` | None | Check if email is available |
| GET | `/accounts/check-username/?username=X` | None | Check if username is available |
| POST | `/accounts/token/refresh/` | None | Exchange refresh token for new access token |

### User & Profile Endpoints (`/community/`)
| Method | URL | Auth | Description |
|---|---|---|---|
| GET | `/community/users/{id}/profile/` | Bearer | Get public profile of any user |
| PATCH | `/community/users/{id}/profile/` | Bearer | Update own profile (name, bio, wishlist_url) |
| POST | `/community/users/{id}/profile-picture/` | Bearer | Upload profile picture (multipart) |
| GET | `/community/users/{id}/posts/` | Bearer | Get posts by a user (respects visibility) |
| POST | `/community/users/{id}/follow/` | Bearer | Toggle follow/unfollow a user |

### Post Endpoints
| Method | URL | Auth | Description |
|---|---|---|---|
| GET | `/community/posts/feed/` | Bearer | Personalized feed (joined hubs + followed users) |
| GET | `/community/posts/global/` | Bearer | All public posts across all hubs |
| POST | `/community/posts/` | Bearer | Create a new post (JSON) |
| POST | `/community/posts/{id}/media/` | Bearer | Upload media for a post (multipart) |
| POST | `/community/posts/{id}/like/` | Bearer | Toggle like/unlike |
| POST | `/community/posts/{id}/validate/` | Bearer | Peer micro-validate a challenge post |
| GET | `/community/posts/{id}/validations/` | Bearer | List validations for a post |
| GET | `/community/posts/{id}/comments/` | Bearer | List comments on a post |
| POST | `/community/posts/{id}/comments/` | Bearer | Add a comment |
| DELETE | `/community/comments/{id}/` | Bearer | Delete own comment |

### Hub Endpoints
| Method | URL | Auth | Description |
|---|---|---|---|
| GET | `/community/hubs/` | Bearer | List all public hubs (paginated) |
| POST | `/community/hubs/` | Bearer | Create a new hub |
| GET | `/community/hubs/recommended/` | Bearer | Hubs matching user's interests |
| GET | `/community/hubs/{id}/` | Bearer | Hub detail (includes main challenge progress) |
| POST | `/community/hubs/{id}/join/` | Bearer | Join a hub |
| POST | `/community/hubs/{id}/leave/` | Bearer | Leave a hub |
| GET | `/community/hubs/{id}/posts/` | Bearer | Posts published in a specific hub |
| POST | `/community/hubs/{id}/cover/` | Bearer | Upload hub cover image (admin only) |
| PATCH | `/community/hubs/{id}/settings/` | Bearer | Update hub settings (admin only) |
| DELETE | `/community/hubs/{id}/delete/` | Bearer | Delete hub (admin only) |
| GET | `/community/hubs/{id}/members/` | Bearer | List hub members |
| DELETE | `/community/hubs/{id}/members/{uid}/` | Bearer | Remove a member (admin only) |
| GET | `/community/hubs/{id}/completion-requests/` | Bearer | Pending completion requests (admin only) |

### Challenge Endpoints
| Method | URL | Auth | Description |
|---|---|---|---|
| GET | `/community/hubs/{id}/challenges/` | Bearer | List challenges in a hub (with user progress) |
| POST | `/community/hubs/{id}/challenges/` | Bearer | Create a challenge (admin only) |
| GET | `/community/challenges/{id}/` | Bearer | Challenge detail with full config |
| GET | `/community/challenges/{id}/leaderboard/` | Bearer | Member leaderboard for a challenge |
| POST | `/community/challenges/{id}/completion-request/` | Bearer | Submit completion request |
| GET | `/community/challenges/{id}/completion-request/mine/` | Bearer | Check own request status |
| PATCH | `/community/completion-requests/{id}/` | Bearer | Approve or reject (admin only) |

### Other Endpoints
| Method | URL | Auth | Description |
|---|---|---|---|
| GET | `/community/explore/feed/` | Bearer | All public posts for Explore screen |
| GET | `/community/search/?q=X` | Bearer | Search users, hubs, posts, challenges |
| GET | `/community/templates/` | Bearer | List official challenge templates |
| GET | `/community/notifications/` | Bearer | List notifications (marks all as read) |
| GET | `/community/notifications/unread-count/` | Bearer | Returns `{"count": N}` |

---

## 8. Android Frontend — Kotlin MVVM

### Project Structure
```
app/src/main/java/com/example/hubrise/
├── MainActivity.kt                    ← Single Activity host
├── data/
│   ├── api/
│   │   ├── RetrofitClient.kt          ← HTTP client (token injection + auto-refresh)
│   │   ├── AuthApiService.kt          ← Auth Retrofit interface
│   │   ├── FeedApiService.kt          ← Feed Retrofit interface
│   │   ├── HubApiService.kt           ← Hub/Challenge Retrofit interface
│   │   ├── CommentApiService.kt       ← Comment Retrofit interface
│   │   ├── ExploreApiService.kt       ← Explore feed Retrofit interface
│   │   ├── NotificationApiService.kt  ← Notification Retrofit interface
│   │   ├── SearchApiService.kt        ← Search Retrofit interface
│   │   └── UserApiService.kt          ← User profile Retrofit interface
│   ├── local/
│   │   └── UserPreferences.kt         ← DataStore wrapper (token + user info)
│   ├── model/                         ← Kotlin data classes
│   │   ├── Post.kt, Hub.kt, Challenge.kt, Comment.kt
│   │   ├── AuthResponse.kt, LoginRequest.kt, SignupRequest.kt
│   │   ├── PaginatedResponse.kt, NotificationItem.kt
│   │   └── ...
│   └── repository/                    ← Data access layer
│       ├── AuthRepository.kt
│       ├── PostRepository.kt
│       ├── HubRepository.kt
│       ├── CommentRepository.kt
│       ├── ExploreRepository.kt
│       ├── NotificationRepository.kt
│       ├── SearchRepository.kt
│       └── UserRepository.kt
└── ui/
    ├── auth/
    │   ├── login/                     LoginActivity + LoginViewModel
    │   └── signup/                    SignupStep1/2/3 Activity + ViewModel
    ├── home/                          HomeFragment + HomeViewModel + PostAdapter
    ├── hubs/                          HubsFragment, HubDetailFragment, 
    │                                  ChallengeDetailFragment, CreateHubFragment,
    │                                  CreateChallengeFragment, MyHubsFragment,
    │                                  ExploreHubsFragment, HubSettingsFragment,
    │                                  CompletionRequestsFragment
    ├── explore/                       ExploreFragment + ExploreViewModel + ExploreAdapter
    ├── create/                        CreatePostFragment + CreatePostViewModel
    ├── comments/                      CommentsBottomSheetFragment + CommentsViewModel
    ├── profile/                       ProfileFragment, EditProfileFragment,
    │                                  UserProfileFragment + ViewModels
    ├── search/                        SearchFragment + SearchViewModel + 4 adapters
    └── notifications/                 NotificationsFragment + NotificationViewModel
```

### MVVM Pattern Explained

**ViewModel** responsibilities:
- Holds UI state in `MutableLiveData` (private) exposed as `LiveData` (public read-only)
- Calls Repository methods inside `viewModelScope.launch { }` (coroutines)
- Never holds a reference to Context, View, or Fragment
- Survives screen rotation (backed by `ViewModelStore`)

**Repository** responsibilities:
- All `suspend fun` methods — called only from coroutines
- Returns a sealed `Result` class: `Result.Success(data)` or `Result.Error(message)`
- Wraps all API calls in `try/catch`, returns `Result.failure` on exception
- Never touches the UI

**Fragment** responsibilities:
- Observes LiveData from ViewModel via `viewLifecycleOwner`
- Updates UI based on observed state
- Sends user events to ViewModel (button clicks, etc.)
- Never calls the API directly

### RetrofitClient.kt — Detailed Explanation

The most architecturally important file in the Android codebase. It is a Kotlin `object` (singleton) that configures all HTTP communication.

**Emulator Detection:**
```kotlin
private fun isEmulator(): Boolean {
    return (Build.FINGERPRINT.startsWith("generic")
        || Build.MODEL.contains("Emulator")
        || Build.PRODUCT.startsWith("sdk")
        || Build.HARDWARE == "goldfish"
        ...)
}
```
Automatically picks the correct base URL — emulator uses `10.0.2.2` (routes to host machine localhost), physical device uses the LAN IP.

**Token Interceptor (OkHttp Interceptor):**
```kotlin
private val tokenInterceptor = Interceptor { chain ->
    val token = runBlocking { prefs.accessToken.first() }
    val request = if (!token.isNullOrEmpty()) {
        chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    } else { chain.request() }
    chain.proceed(request)
}
```
Runs on every HTTP request. Reads the token synchronously (via `runBlocking` on the DataStore Flow), and injects the `Authorization: Bearer <token>` header.

**Token Authenticator (OkHttp Authenticator):**
```kotlin
private val tokenAuthenticator = object : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains("token/refresh")) return null
        val refreshToken = runBlocking { prefs.refreshToken.first() } ?: return navigateToLogin()
        return try {
            val refreshResponse = runBlocking { noAuthRetrofit().refreshToken(RefreshTokenRequest(refreshToken)) }
            runBlocking { prefs.saveAccessToken(refreshResponse.access) }
            response.request.newBuilder().header("Authorization", "Bearer ${refreshResponse.access}").build()
        } catch (e: Exception) { navigateToLogin(); null }
    }
}
```
OkHttp calls `authenticate()` automatically whenever a `401 Unauthorized` is received. It uses a **separate no-auth Retrofit instance** (no interceptor) to call `/accounts/token/refresh/` — this is critical to avoid an infinite loop. The new access token is saved to DataStore, and the original request is retried with the new token. The user never sees a login error for an expired access token — it is completely transparent.

**Two Retrofit Instances:**
- `getRetrofit()` — uses OkHttp with both the tokenInterceptor and tokenAuthenticator. Used for all authenticated endpoints.
- `noAuthRetrofit()` — bare OkHttp, no auth. Used for login/signup/refresh (these must not have a token attached).

### UserPreferences.kt — Detailed Explanation

Wraps Android **Jetpack DataStore (Preferences)** — a modern, coroutine-native replacement for SharedPreferences.

**Why DataStore over SharedPreferences?**
- DataStore is backed by Protocol Buffers and guaranteed to be safe for concurrent coroutine access
- SharedPreferences can throw `ClassCastException` at runtime; DataStore is type-safe
- DataStore exposes data as Kotlin `Flow` — perfectly integrated with coroutines and LiveData
- No `apply()`/`commit()` confusion — all operations are suspend functions

**Stored data:**
- `ACCESS_TOKEN` — JWT access token (30-min TTL)
- `REFRESH_TOKEN` — JWT refresh token (7-day TTL)
- `USER_ID` — integer ID
- `EMAIL`, `USERNAME`, `FULL_NAME`, `PROFILE_PICTURE_URL`
- `IS_LOGGED_IN` — boolean flag for splash screen routing
- `LAST_LOGIN_TIMESTAMP` — long ms timestamp

**`clearAll()`:** atomically clears all preferences in one DataStore transaction. Called on logout.

### HomeViewModel.kt — Detailed Explanation

Manages the Home Feed screen state. Key features:

**Pagination:**
```kotlin
private var currentPage = 1
private var hasMorePages = false
private var isLoadingMoreFlag = false

fun loadMore() {
    if (isLoadingMoreFlag || !hasMorePages || _isLoading.value == true) return
    // loads next page and appends to _posts
}
```
The RecyclerView's `addOnScrollListener` in HomeFragment calls `loadMore()` when the user reaches near the bottom.

**Optimistic Like Toggle:**
```kotlin
fun toggleLike(post: Post) {
    // Immediately update UI (optimistic)
    _posts.value = _posts.value.orEmpty().map {
        if (it.id == post.id) it.copy(
            isLiked = !it.isLiked,
            likesCount = if (it.isLiked) it.likesCount - 1 else it.likesCount + 1
        ) else it
    }
    // Fire the API call in background (no rollback on failure — next refresh corrects)
    viewModelScope.launch { repository.toggleLike(post.id) }
}
```
The like button responds instantly without waiting for the network — gives the feel of a native social app.

**Peer Validation:**
```kotlin
fun toggleValidate(post: Post) {
    viewModelScope.launch {
        val result = if (post.validatedByMe) repository.unvalidatePost(post.id) 
                     else repository.validatePost(post.id)
        // Updates post.validatedByMe, validationsCount, and isTrusted in the list
    }
}
```

### Key Adapters

**PostAdapter (HomeFragment, HubDetailFragment)**
- RecyclerView adapter with `DiffUtil` for efficient updates
- Each post card shows: avatar, username, hub name, timestamp, content, media (image/video), like count, comment count, validation count, "Trusted" badge if `isTrusted=true`
- Callback interface for: like toggle, comment click, validate click, profile click, hub click

**ExploreAdapter (ExploreFragment)**
- Full-screen vertical RecyclerView (TikTok style)
- Each item fills the entire screen
- Video posts use ExoPlayer for auto-play

**HubsAdapter / MyHubsAdapter**
- Grid or list of Hub cards
- Each card shows cover image, name, member count, category badge

**HubChallengeAdapter (HubDetailFragment)**
- Lists challenges in a hub
- Shows progress bar, percent complete, summary (e.g., "3/5 stages", "45/100 km")

**CalendarHeatmapAdapter (ChallengeDetailFragment)**
- Renders a grid calendar view
- Colors each day based on whether it appears in `UserStreakProgress.checkin_calendar`

**LeaderboardAdapter (ChallengeDetailFragment)**
- Ranked list of members and their progress values

---

## 9. Feature-by-Feature Walkthrough

### Feature 1: User Registration (3-Step Signup)

**Step 1 — SignupStep1Activity:**
- User enters email and password
- On "Next": calls `GET /accounts/check-email/?email=X`
- If available → saves to ViewModel state, navigates to Step 2
- If taken → shows inline error under the email field

**Step 2 — SignupStep2Activity:**
- User enters username, full name, date of birth
- On "Next": calls `GET /accounts/check-username/?username=X`
- Date picker uses a custom `ModernDatePickerDialog` component
- If all valid → saves to ViewModel, navigates to Step 3

**Step 3 — SignupStep3ProfileSetupActivity:**
- User selects interests (chips from the API)
- Optionally uploads a profile picture
- On "Create Account": calls `POST /accounts/signup/` with all collected data
- On success: saves JWT + user info to DataStore, navigates to MainActivity

**Why 3 steps?**
Breaking the form into 3 steps improves UX — one cognitive task at a time, and real-time validation at each step boundary catches errors early before submitting a large form.

---

### Feature 2: Login & Session Management

**LoginActivity:**
- User enters email + password
- Calls `POST /accounts/login/`
- On success: tokens and user info saved to DataStore
- Navigates to MainActivity with `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK` (prevents going back to login)

**Automatic Token Refresh:**
When any API call returns 401, OkHttp's `tokenAuthenticator` silently:
1. Calls `POST /accounts/token/refresh/` with the stored refresh token
2. Saves the new access token to DataStore
3. Retries the original failed request with the new token
The user never sees an error or gets redirected to login unless the refresh token itself is expired or blacklisted.

**Logout:**
- Calls `POST /accounts/logout/` with `refresh_token` in body
- Backend blacklists the refresh token (it can never be used again)
- `UserPreferences.clearAll()` wipes DataStore
- Navigate to LoginActivity with `FLAG_ACTIVITY_CLEAR_TASK`

---

### Feature 3: Home Feed

**HomeFragment:**
- Two tabs via TabLayout: "My Feed" and "Global"
- My Feed: `GET /community/posts/feed/` — posts from joined hubs + followed users
- Global: `GET /community/posts/global/` — all public posts
- Pull-to-refresh via SwipeRefreshLayout
- Infinite scroll — when scrolled to bottom, `loadMore()` is called
- Each post card: avatar → click opens UserProfileFragment; hub name → click opens HubDetailFragment
- Like button: optimistic UI update, API call in background
- Comment icon: opens CommentsBottomSheetFragment
- Validate button (on challenge posts): calls peer validation API

---

### Feature 4: Hubs

**HubsFragment (ViewPager2 + 2 Tabs):**
- Tab 1: **My Hubs** — shows all hubs the user is a member of (grid layout)
- Tab 2: **Explore Hubs** — shows recommended hubs + all public hubs

**HubDetailFragment:**
- Shows hub cover image, name, description, member count, category
- Shows hub posts (paginated)
- Shows hub challenges (with progress bars)
- Join/Leave button (updates members_count in real time from API response)
- If user is admin: Settings button → HubSettingsFragment

**HubSettingsFragment:**
- Edit hub name, description, category
- Upload new cover image
- View and remove members
- Delete hub

**CreateHubFragment:**
- Form: name, description, category (spinner from Interest list)
- Optional cover image upload
- On submit: `POST /community/hubs/`, then `POST /community/hubs/{id}/cover/`

---

### Feature 5: Challenges

**ChallengeDetailFragment:**
This is the richest fragment in the app. Adapts its UI entirely based on `challenge.progress_model`:

**For Stage-Based:**
- Shows list of stages with status icons (not started / in progress / completed)
- Current stage has a "Submit Proof" button
- Proof submission creates a `stage_proof` post and updates `UserStageProgress`
- Completed stages show a checkmark and a link to the proof post

**For Count-Based:**
- Shows a horizontal progress bar (current_count / target_count)
- "Log Entry" button opens a dialog where the user enters a number
- Submit creates a `count_entry` post and updates `UserCountProgress.current_count`
- Shows unit label (e.g., "42.5 / 100 km")

**For Streak-Based:**
- Shows a calendar heatmap (CalendarHeatmapAdapter)
- Shows current streak, longest streak, total check-ins
- "Check In Today" button (disabled if already checked in today)
- Check-in creates a `streak_checkin` post and updates `UserStreakProgress`

**Completion Request Flow:**
1. Member taps "Request Completion" → opens dialog with optional note
2. `POST /community/challenges/{id}/completion-request/`
3. Hub admin receives a `completion_submitted` notification
4. Admin opens CompletionRequestsFragment, sees the request with member's proof posts
5. Admin taps Approve or Reject (with optional note)
6. `PATCH /community/completion-requests/{id}/` with `{"status": "approved", "admin_note": "..."}`
7. On approval: backend auto-creates an `achievement_broadcast` post in the hub feed
8. Member receives `completion_approved` notification

**CreateChallengeFragment:**
- Lets admin pick progress model (Stage / Count / Streak) via radio buttons
- UI dynamically changes based on selection:
  - Stage: add/remove/reorder stage rows
  - Count: target number, unit, increment, cumulative toggle
  - Streak: target days, frequency, grace days
- Optional: load from a ChallengeTemplate (fetched from `/community/templates/`)
- Applying a template pre-fills all fields — admin can then customize

---

### Feature 6: Comments

**CommentsBottomSheetFragment:**
- Slides up from the bottom when comment icon is tapped
- Fetches `GET /community/posts/{id}/comments/`
- Shows list of comments (author avatar, username, content, timestamp)
- Input field at the bottom + Send button
- `POST /community/posts/{id}/comments/` to submit
- New comment appears at bottom of list immediately
- Long-press own comment → delete option

---

### Feature 7: Profile & Social

**ProfileFragment (own profile):**
- Shows: profile picture, full name, username, bio, post count, hub count, followers, following
- Shows own posts in a grid/list
- "Edit Profile" button → EditProfileFragment

**UserProfileFragment (other users):**
- Same layout as own profile
- Follow/Unfollow button
- Follow calls `POST /community/users/{id}/follow/` (toggle)
- If following → button shows "Following"; tap again to unfollow
- Follower count updates immediately in the UI

**EditProfileFragment:**
- Edit full name, bio, wishlist URL
- Upload new profile picture
- `PATCH /community/users/{id}/profile/` for text fields
- `POST /community/users/{id}/profile-picture/` for image (multipart)

---

### Feature 8: Notifications

**NotificationsFragment:**
- Full list of notifications, newest first
- Each notification has an icon (type-specific), sender avatar, message text, timestamp
- Notification types and their messages:
  - `follow` → "@username started following you"
  - `like` → "@username liked your post"
  - `comment` → "@username commented on your post"
  - `completion_submitted` → "@username submitted a completion request for 'Challenge Title'"
  - `completion_approved` → "🎉 Your completion of 'Challenge Title' was approved!"
  - `completion_rejected` → "Your completion request for 'Challenge Title' needs more work — tap to see why"
- On screen open: all notifications marked as read automatically
- Unread badge on the Notifications tab icon (updated by `fetchUnreadCount()` in HomeViewModel)

---

### Feature 9: Search

**SearchFragment:**
- Single search bar at the top
- Results shown in 4 tabs: Users, Hubs, Posts, Challenges
- `GET /community/search/?q=<query>`
- Each tab has a dedicated adapter:
  - `UserSearchAdapter` — avatar + username + full name
  - `HubSearchAdapter` — cover image + hub name + member count
  - `PostSearchAdapter` — author + content snippet + hub name
  - `ChallengeSearchAdapter` — title + progress model icon + hub name

---

### Feature 10: Explore Screen

**ExploreFragment:**
- Full-screen vertical RecyclerView
- Each item fills 100% of screen height
- Posts with `media_type="video"` play automatically (ExoPlayer)
- Posts with `media_type="image"` show the image full-screen
- Like and comment overlays on top of the media
- Loads from `GET /community/explore/feed/`

---

## 10. Key Code Highlights & Explanations

### Code Highlight A: `RetrofitClient.kt` — The OkHttp Authenticator

**File:** `app/src/main/java/com/example/hubrise/data/api/RetrofitClient.kt`

**What it does:** Implements automatic, transparent JWT token refresh when the access token expires.

**Why it matters:** Without this, a user whose 30-minute access token expires would see an error screen and have to log in again every 30 minutes. With this authenticator, the refresh happens silently in the background — the user never notices.

**The key design decision:** The refresh call uses a **separate, no-auth OkHttp client** (`noAuthRetrofit()`). This is critical — if the refresh used the same auth client, when the refresh endpoint returns 200, the authenticator would be called again recursively, causing an infinite loop. The guard `if (response.request.url.encodedPath.contains("token/refresh")) return null` also prevents this.

```kotlin
private val tokenAuthenticator = object : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // Guard: don't retry if the refresh request itself failed
        if (response.request.url.encodedPath.contains("token/refresh")) return null
        
        val prefs = UserPreferences(appContext)
        val refreshToken = runBlocking { prefs.refreshToken.first() }
            ?: return navigateToLogin()

        return try {
            // Use a bare Retrofit (no auth interceptor) to avoid infinite loop
            val refreshService = noAuthRetrofit().create(AuthApiService::class.java)
            val refreshResponse = runBlocking {
                refreshService.refreshToken(RefreshTokenRequest(refreshToken))
            }
            // Persist the new access token
            runBlocking { prefs.saveAccessToken(refreshResponse.access) }
            
            // Replay the original request with the new token
            response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshResponse.access}")
                .build()
        } catch (e: Exception) {
            navigateToLogin()  // Refresh itself failed — force login
            null
        }
    }
    
    private fun navigateToLogin(): Request? {
        runBlocking { UserPreferences(appContext).clearAll() }
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val intent = Intent(appContext, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            appContext.startActivity(intent)
        }
        return null
    }
}
```

---

### Code Highlight B: `UserPreferences.kt` — DataStore Token Storage

**File:** `app/src/main/java/com/example/hubrise/data/local/UserPreferences.kt`

**What it does:** Provides type-safe, coroutine-native access to all persisted session data.

**Why DataStore over SharedPreferences:**
SharedPreferences is synchronous and not safe for concurrent access from coroutines. DataStore wraps all I/O in Kotlin Flows and suspend functions, making it a first-class citizen in the coroutine ecosystem. It also guarantees atomicity — `clearAll()` is a single atomic transaction, not multiple individual deletes.

```kotlin
private val Context.dataStore: DataStore<Preferences> 
    by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    companion object {
        private val ACCESS_TOKEN  = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val USER_ID       = intPreferencesKey("user_id")
        private val IS_LOGGED_IN  = booleanPreferencesKey("is_logged_in")
        // ... more keys
    }

    // Flow-based reactive read
    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }

    // Suspend write
    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { it[ACCESS_TOKEN] = token }
    }

    // Atomic clear on logout
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    // One-shot read (used by RetrofitClient)
    suspend fun hasToken(): Boolean = accessToken.first()?.isNotEmpty() == true
}
```

---

### Code Highlight C: Challenge Model — Polymorphic Design (Django)

**File:** `backend/community/models.py`

**What it does:** Models three fundamentally different challenge types with a clean, normalized schema.

**The design choice:** One `Challenge` header row with a `progress_model` field, plus three separate configuration models (one per type). User progress is also stored in separate tables per model. This avoids a single fat table with many nullable columns (the "wide table anti-pattern").

```
Challenge (header)
├── progress_model = "stage"   → ChallengeStage rows (1 per milestone)
│                              → UserStageProgress rows (1 per user per stage)
├── progress_model = "count"   → ChallengeCountConfig (1 row, 1:1)
│                              → UserCountProgress (1 per user)
└── progress_model = "streak"  → ChallengeStreakConfig (1 row, 1:1)
                               → UserStreakProgress (1 per user)
```

**`get_challenge_progress()` in serializers.py** abstracts this complexity — callers don't care what model a challenge uses; they always get back `(percent_complete: int, summary: str)`. This is used everywhere progress needs to be displayed.

---

### Code Highlight D: `build_auth_response()` — DRY Authentication

**File:** `backend/accounts/views.py`

**What it does:** Single function called by 3 different auth views to produce a consistent response.

**Why it matters:** Signup, Login, and Social Login are three different flows that all result in the same outcome: the user is authenticated and the client needs a JWT pair + user data. By centralizing this in one function, the API contract is guaranteed to be identical across all three flows, the Android `AuthRepository` uses a single `AuthResponse` data class, and any future change to the response structure only needs to be made in one place.

```python
def build_auth_response(user, request):
    refresh = RefreshToken.for_user(user)
    return {
        "access_token":  str(refresh.access_token),
        "refresh_token": str(refresh),
        "user": UserSerializer(user, context={"request": request}).data,
    }

# Called identically from all three views:
class SignupView(APIView):
    def post(self, request):
        user = serializer.save()
        return Response(build_auth_response(user, request), status=201)

class LoginView(APIView):
    def post(self, request):
        user = serializer.validated_data["user"]
        return Response(build_auth_response(user, request), status=200)
```

---

### Code Highlight E: `HomeViewModel.toggleLike()` — Optimistic UI

**File:** `app/src/main/java/com/example/hubrise/ui/home/HomeViewModel.kt`

**What it does:** Updates the like button UI instantly before the API call completes.

**Why it matters:** If we waited for the API call to return before updating the UI, there would be a visible delay (100–500ms) between tap and response. Social apps feel broken when interactions don't respond instantly. The optimistic approach updates the local copy of the post list immediately, fires the API call in the background, and accepts that if the call fails, the next feed refresh will correct the state.

```kotlin
fun toggleLike(post: Post) {
    // Step 1: Update UI immediately (optimistic)
    _posts.value = _posts.value.orEmpty().map {
        if (it.id == post.id) it.copy(
            isLiked = !it.isLiked,
            likesCount = if (it.isLiked) it.likesCount - 1 else it.likesCount + 1
        ) else it
    }
    // Step 2: Send API call in background (fire and forget)
    viewModelScope.launch {
        repository.toggleLike(post.id)
        // No rollback — next loadFeed() corrects any mismatch
    }
}
```

---

### Code Highlight F: `get_challenge_progress()` — Model-Agnostic Progress (Django)

**File:** `backend/community/serializers.py`

**What it does:** Returns progress data for ANY challenge regardless of its model type.

**Why it matters:** The challenge list API needs to show progress for every challenge regardless of whether it is stage/count/streak. Without this abstraction, every view that shows challenges would need to branch on `progress_model` to calculate progress separately. This function encapsulates all three calculations behind a unified interface.

```python
def get_challenge_progress(challenge, user):
    if challenge.progress_model == Challenge.MODEL_STAGE:
        total = challenge.stages.count()
        completed = UserStageProgress.objects.filter(
            stage__challenge=challenge, user=user, 
            status=UserStageProgress.STATUS_COMPLETED
        ).count()
        percent = int((completed / total) * 100) if total else 0
        return percent, f"{completed}/{total} stages"

    if challenge.progress_model == Challenge.MODEL_COUNT:
        config = challenge.count_config
        progress = UserCountProgress.objects.filter(challenge=challenge, user=user).first()
        current = progress.current_count if progress else 0
        percent = int((float(current) / float(config.target_count)) * 100)
        return percent, f"{float(current):g}/{float(config.target_count):g} {config.unit_label}"

    # Streak model
    config = challenge.streak_config
    progress = UserStreakProgress.objects.filter(challenge=challenge, user=user).first()
    total_checkins = progress.total_checkins if progress else 0
    percent = int((total_checkins / config.target_days) * 100)
    return percent, f"{total_checkins}/{config.target_days} days"
```

---

## 11. App Flow & Navigation

### App Startup Flow
```
App launches
  └─► MainActivity.onCreate()
        └─► Check DataStore: hasToken()?
              ├─► YES → NavHostFragment with HomeFragment as start destination
              └─► NO  → LoginActivity
```

### Navigation Graph (MainActivity)
```
HomeFragment  ─────────────────────────────────────────────────┐
HubsFragment                                                    │
  ├── HubDetailFragment                                         │
  │     ├── ChallengeDetailFragment                            │
  │     │     └── CreateChallengeFragment                      │
  │     │     └── CompletionRequestsFragment                   │
  │     └── HubSettingsFragment                                │
  └── CreateHubFragment                                         │
ExploreFragment                                                 │
CreatePostFragment                                              │
ProfileFragment                                                 │
  └── EditProfileFragment                                       │
  └── UserProfileFragment (other users)                        │ All fragments
SearchFragment                                                  │ can open:
NotificationsFragment                                           │  UserProfileFragment
CommentsBottomSheetFragment (modal over any screen)            │  HubDetailFragment
```

### Authentication Flow
```
LoginActivity ──► SignupStep1Activity ──► SignupStep2Activity ──► SignupStep3Activity
     │                                                                    │
     │                                                               MainActivity
     └──────────────────────────────────────────────────────────────────►
```

---

## 12. Limitations

### Technical Limitations
1. **SQLite database** — development only. SQLite serializes writes, making it unsuitable for concurrent production traffic. PostgreSQL migration is needed before any real deployment.

2. **No push notifications** — Notifications are pull-based. The app fetches unread count on screen open. Users receive no alert while the app is in the background. Firebase Cloud Messaging (FCM) is the planned solution.

3. **Social login not fully verified** — The `SocialLoginView` accepts a `provider` + `id_token` but does not cryptographically verify the token signature against Google/Facebook/Apple's public keys. This is a security gap — in production, the backend must verify tokens before trusting them.

4. **Local media storage** — Uploaded images and videos are stored on the server's local filesystem under `/media/`. In production, this must be replaced with AWS S3 or Google Cloud Storage + a CDN. The current setup doesn't scale horizontally (multiple server instances can't share local storage).

5. **Single server process** — No Redis caching layer, no Celery task queue, no horizontal scaling. The server handles all requests synchronously in Django's development server.

6. **No automated test suite** — `tests.py` files exist as stubs but contain no tests. No integration tests, no unit tests.

7. **Token refresh uses `runBlocking`** — In the OkHttp `tokenInterceptor` and `tokenAuthenticator`, `runBlocking` is used to call suspend functions from the OkHttp callback thread. This is a pragmatic workaround — OkHttp doesn't natively support coroutines. A production implementation would use a dedicated Mutex to prevent concurrent refresh calls.

### Feature Limitations (Not Yet Built)
- No direct messaging between users
- No achievement badge system on profiles
- No Hub leaderboard on the Hub Detail screen (API exists, UI not wired)
- No post editing — posts cannot be edited after creation
- Social login UI buttons exist but the complete OAuth handshake is not implemented end-to-end on Android
- No email verification during signup
- No password reset flow

---

## 13. Future Work

### Short-Term (Next Version)
- **Firebase Cloud Messaging (FCM)**: real-time push notifications for follows, likes, comments, challenge approvals
- **PostgreSQL migration**: switch from SQLite for production readiness
- **AWS S3 + CloudFront**: scalable media storage with CDN delivery
- **Social login completion**: full OAuth handshake on Android (Google Sign-In SDK, Facebook SDK)
- **Email verification**: confirm email address after signup
- **Password reset**: forgot password flow via email

### Medium-Term
- **Achievement Badges**: permanent badges on user profiles for completed challenges
- **Hub Leaderboards**: live ranking of members by challenge progress, displayed in HubDetailFragment
- **Post editing**: allow users to edit the text content of their own posts
- **Comment replies**: nested comments (reply to a specific comment)
- **Direct messaging**: private 1:1 messages between users

### Long-Term Vision
- **iOS version**: Swift/SwiftUI client consuming the same REST API
- **AI challenge suggestions**: recommend challenges based on user interests and activity history
- **Video encoding pipeline**: auto-compress uploaded videos (Celery + FFmpeg) for consistent quality
- **Analytics dashboard**: Hub admins see engagement stats, member activity trends, challenge completion rates
- **Monetization**: premium Hub features, sponsored challenges, brand partnerships

---

## 14. References

1. Django Software Foundation. *Django Documentation*. https://docs.djangoproject.com/
2. Encode OSS. *Django REST Framework Documentation*. https://www.django-rest-framework.org/
3. SimpleJWT Contributors. *DRF SimpleJWT Documentation*. https://django-rest-framework-simplejwt.readthedocs.io/
4. Google. *Android Kotlin Developer Guide*. https://developer.android.com/kotlin
5. Square, Inc. *Retrofit 2 Documentation*. https://square.github.io/retrofit/
6. Square, Inc. *OkHttp Documentation*. https://square.github.io/okhttp/
7. Google. *Android Jetpack DataStore*. https://developer.android.com/topic/libraries/architecture/datastore
8. Google. *Android Navigation Component*. https://developer.android.com/guide/navigation
9. Google. *ViewModel Overview*. https://developer.android.com/topic/libraries/architecture/viewmodel
10. Bump Technologies. *Glide Image Loading Library*. https://bumptech.github.io/glide/
11. Strava. *Strava Platform*. https://www.strava.com/
12. HabitRPG, Inc. *Habitica*. https://habitica.com/
13. Meta Platforms. *Instagram*. https://www.instagram.com/
14. Google. *ExoPlayer Documentation*. https://exoplayer.dev/
15. JWT.io. *JSON Web Tokens Introduction*. https://jwt.io/introduction/

---

## APPENDIX: Full Model Relationship Diagram

```
User ────────────────────────────────────────────────────────────────────────┐
 │                                                                            │
 ├──[1:1]──► UserProfile (full_name, bio, profile_picture, wishlist_url)     │
 │                 └──[M:M]──► Interest                                       │
 │                                  └──[1:N]──► Hub.category                 │
 │                                                                            │
 ├──[1:N]──► UserFollow (follower → following)                               │
 │                                                                            │
 ├──[1:N]──► HubMembership ──────────────────────────────────────────────────┤
 │                 └──► Hub (name, description, cover_image, members_count)   │
 │                           └──[1:N]──► Challenge                            │
 │                           │               ├──[stage] ChallengeStage ──────►│
 │                           │               │               └── UserStageProgress
 │                           │               ├──[count] ChallengeCountConfig  │
 │                           │               │               └── UserCountProgress
 │                           │               └──[streak] ChallengeStreakConfig │
 │                           │                               └── UserStreakProgress
 │                           └──[1:N]──► Post                                 │
 │                                           ├──[1:N]──► PostLike ───────────►│
 │                                           ├──[1:N]──► Comment ────────────►│
 │                                           └──[1:N]──► PostValidation ─────►│
 │                                                                            │
 ├──[1:N]──► CompletionRequest ──► Challenge ──► Hub                         │
 │                                                                            │
 └──[1:N]──► Notification (recipient, sender, type, post?, challenge?)       │
                                                                              │
ChallengeTemplate ──[1:N]──► TemplateStage                                   │
(read-only blueprints — copied at challenge creation, never linked back)     │
```

---

*Document generated from the live HubRise codebase — June 2026*
*GitHub: https://github.com/hadiyazbek2/HubRise*
