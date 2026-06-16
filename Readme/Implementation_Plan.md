# HubRise — Implementation Plan
*Approved 2026-06-13. Every checkbox here maps to real code. Tick it only when the feature is fully working end-to-end.*

---

## How to read this file
- **[B]** = Backend (Django)
- **[A]** = Android (Kotlin)
- Items are ordered by dependency — do not skip ahead.
- When a group is fully checked, update `App_startup.md` accordingly.

---

## Phase 1 — Foundation Fixes (no new screens, high impact)

### 1.1 Post Type Flag  *(Suggestion 1)*
- [x] [B] Add `post_type` CharField to `Post` model with choices: `regular`, `progress_update`, `achievement_broadcast`, `admin_announcement`. Default = `regular`.
- [x] [B] Create and run migration. *(migration 0004)*
- [x] [B] Add `post_type` to `PostSerializer` fields.
- [x] [B] Add `post_type` to `CreatePostSerializer` writeable fields.
- [x] [A] Add `postType: String` field to `Post.kt` data class.

### 1.2 Hub Member Role  *(Suggestion 4)*
- [x] [B] Add `role` CharField to `HubMembership` with choices: `member`, `admin`, `moderator`. Default = `member`.
- [x] [B] Create and run migration. *(migration 0005)*
- [x] [B] When a hub is created, set creator's membership `role = admin`.
- [ ] [B] Add `role` to a new `HubMembershipSerializer`. *(needed when building member list endpoint)*
- [x] [B] Restrict `is_main=True` challenge creation to `role=admin` only (in `ChallengeListCreateView`).
- [ ] [A] Add `role: String` field to a new `HubMembership.kt` data class. *(needed when displaying member list)*

### 1.3 Hub Member Cap  *(Suggestion 6)*
- [x] [B] In `JoinHubView`, reject if user already has ≥ 20 memberships. Return `403` with `{"detail": "You cannot join more than 20 hubs."}`.

### 1.4 comments_count in API  *(Suggestion 5)*
- [x] [B] Add `comments_count` as `SerializerMethodField` to `PostSerializer` (returns 0 until Comment model added).
- [x] [A] `Post.kt` already has `commentsCount` with `@SerializedName("comments_count")`. ✓

### 1.5 Personal Hub on Signup  *(Suggestion 7)*
- [x] [B] After user is created in `SignupSerializer.create()`, auto-create a Hub named "Personal" with `is_public=False`, `created_by=user`, and auto-join user as `role=admin`.
- [x] [B] Also do the same in `SocialLoginSerializer.create()` when a new user is made.
- [x] [B] Make `category` nullable (`null=True, blank=True`) on Hub for personal hubs.
- [x] [B] Add migration for nullable category on Hub. *(migration 0006)*
- [ ] [A] In `CreatePostFragment`, filter out the personal hub from the hub picker (or label it clearly as "Personal").

### 1.6 Token Auto-Refresh  *(Suggestion 2)*
- [x] [A] Add `RetrofitClient.init(context)` called from `LoginActivity` and `MainActivity`.
- [x] [A] Add `tokenInterceptor` that attaches `Authorization: Bearer <token>` to every request.
- [x] [A] Add `tokenAuthenticator` (OkHttp `Authenticator`) that intercepts 401 responses.
- [x] [A] Authenticator reads `refresh_token` from `UserPreferences` (via `runBlocking`).
- [x] [A] Calls `POST /api/auth/token/refresh/` on a separate no-auth Retrofit instance.
- [x] [A] On success: saves new `access_token`, retries original request with new token.
- [x] [A] On failure: clears all tokens, navigates to `LoginActivity` with clear task flags.
- [x] [A] `RefreshTokenRequest` / `RefreshTokenResponse` already existed in `TokenModels.kt`. ✓

---

## Phase 2 — Media as File Fields  *(Suggestion 3)*

### 2.1 Backend Media Migration
- [ ] [B] Change `Post.media_url` from `URLField` to `ImageField` / `FileField` (upload_to=`post_media/`). Keep `media_url` as the field name for API compatibility — the serializer will return the absolute URL.
- [ ] [B] Change `Hub.cover_image_url` from `URLField` to `ImageField` (upload_to=`hub_covers/`). Keep field name `cover_image_url`.
- [ ] [B] Create and run migration.
- [ ] [B] Update `PostSerializer` to return absolute URL for `media_url` (same pattern as `profile_picture_url`).
- [ ] [B] Update `HubSerializer.get_cover_image_url` to handle `FileField` (already uses a method — update to use `.url` from the field).
- [ ] [B] Add `POST /api/posts/{id}/media/` multipart endpoint to upload media to a post (or include it in post creation as multipart).
- [ ] [B] Add `POST /api/hubs/{id}/cover/` multipart endpoint for hub cover upload (admin only).
- [ ] [A] Add media picker (camera + gallery) to `CreatePostFragment`.
- [ ] [A] Upload media via multipart after post creation.
- [ ] [A] Add cover image upload to `CreateHubFragment`.
- [ ] [A] Add `MediaApiService.kt` for the media upload endpoints.

---

## Phase 3 — Comments

### 3.1 Backend Comments
- [ ] [B] Add `Comment` model to `community/models.py`:
  - `post` → FK(Post, CASCADE, related_name='comments')
  - `author` → FK(User, CASCADE)
  - `content` → TextField
  - `created_at` → DateTimeField(auto_now_add)
- [ ] [B] Create and run migration.
- [ ] [B] Add `CommentSerializer` (id, post, author, author_username, author_avatar_url, content, created_at).
- [ ] [B] Add `CommentListCreateView` — `GET /api/posts/{id}/comments/` and `POST /api/posts/{id}/comments/`.
- [ ] [B] Add `CommentDeleteView` — `DELETE /api/comments/{id}/` (author only).
- [ ] [B] Wire new URLs in `community/urls.py`.
- [ ] [B] Now update `PostSerializer.comments_count` to use real `comments.count()`.

### 3.2 Android Comments UI
- [ ] [A] Add `Comment.kt` data class.
- [ ] [A] Add `CommentApiService.kt` with list, create, delete endpoints.
- [ ] [A] Add `CommentRepository.kt`.
- [ ] [A] Create `CommentsBottomSheetFragment` (bottom sheet with RecyclerView + input bar).
- [ ] [A] Create `CommentsViewModel`.
- [ ] [A] Create `CommentAdapter`.
- [ ] [A] Wire "comment" button on post cards in `PostAdapter` to open `CommentsBottomSheetFragment`.
- [ ] [A] Wire "comment" button on Explore video overlay.

---

## Phase 4 — Explore Screen (Short Videos)  *(Suggestion 8)*

### 4.1 Backend Explore Feed
- [ ] [B] Add `GET /api/posts/explore/` — returns paginated video posts from public hubs, filtered by user's interests, ordered by `created_at` DESC.
- [ ] [B] Add `GET /api/posts/explore/hubs/` — same but only from user's joined hubs.
- [ ] [B] "Video post" = any post where `media_url` is non-empty and `post_type = regular` (or `progress_update`). Add a `is_video` SerializerMethodField or filter by file extension.

### 4.2 Android Explore Screen
- [ ] [A] Add ExoPlayer dependency to `build.gradle`.
- [ ] [A] Create `ExploreFragment` with two tabs ("For You" / "Your Hubs") using ViewPager2.
- [ ] [A] Create `ExploreVideoFragment` — full-screen vertical RecyclerView with `PagerSnapHelper`.
- [ ] [A] Create `VideoPlayerViewHolder` — binds `ExoPlayer` to a `PlayerView`.
- [ ] [A] Implement play/pause on scroll: release player when off screen, acquire when on screen.
- [ ] [A] Overlay UI (bottom-left: username, hub badge, caption; right: like, comment, share).
- [ ] [A] Create `ExploreViewModel` + `ExploreRepository`.
- [ ] [A] Implement load-more pagination.

---

## Phase 5 — Hub Admin Panel

### 5.1 Backend Admin Endpoints
- [ ] [B] Add `PATCH /api/hubs/{id}/` — edit hub name, description, category (admin only, checks `role=admin`).
- [ ] [B] Add `GET /api/hubs/{id}/members/` — list members with username, avatar, role, joined_at.
- [ ] [B] Add `POST /api/hubs/{id}/members/{user_id}/role/` — change a member's role (admin only).
- [ ] [B] Add `POST /api/posts/{id}/announce/` — admin reposts a user's post as `post_type=admin_announcement` (creates a new Post linked to original).
- [ ] [B] Add `POST /api/hubs/{id}/challenges/main/` — set or update the main challenge (admin only, sets `is_main=True`, clears previous main).

### 5.2 Android Hub Admin UI
- [ ] [A] Show "Admin" button on `HubDetailFragment` toolbar when `role=admin`.
- [ ] [A] Create `HubAdminFragment` with: edit hub info, manage members, set main challenge.
- [ ] [A] Create `HubMembersFragment` — list members with role badges, tap to change role.
- [ ] [A] In `HubDetailFragment`, add "Announce" option on post long-press (admin only).

---

## Phase 6 — Achievement & Verification  *(Suggestion 9)*

### 6.1 Backend Achievement System
- [ ] [B] Add `Achievement` model:
  - `user` → FK(User, CASCADE)
  - `challenge` → FK(Challenge, CASCADE)
  - `verified` → BooleanField(default=False)
  - `verified_at` → DateTimeField(null, blank)
  - `verified_by` → FK(User, null, blank, related_name='verifications')
  - `broadcast_post` → FK(Post, null, blank)
  - `created_at` → DateTimeField(auto_now_add)
  - unique_together: (user, challenge)
- [ ] [B] Create and run migration.
- [ ] [B] Auto-create an `Achievement` (unverified) when `ChallengeProgress.current_count >= target_count`.
- [ ] [B] Add `GET /api/achievements/` — current user's achievements.
- [ ] [B] Add `GET /api/users/{id}/achievements/` — any user's verified achievements.
- [ ] [B] Add `POST /api/achievements/{id}/verify/` — admin verifies, creates broadcast Post (`post_type=achievement_broadcast`), sets `verified=True`.
- [ ] [B] Add `GET /api/achievements/{id}/evidence/` *(Suggestion 9)* — returns user's last 10 posts in the relevant hub near completion time, for admin review.

### 6.2 Android Achievement UI
- [ ] [A] Add `Achievement.kt` data class.
- [ ] [A] Add `AchievementApiService.kt`.
- [ ] [A] Add `AchievementRepository.kt`.
- [ ] [A] Add "Achievements" tab to `ProfileFragment` with badge grid.
- [ ] [A] Create `AchievementAdapter` with badge/medal card design.
- [ ] [A] Show "Verify" button on unverified achievements in hub admin panel.
- [ ] [A] Show achievement broadcast posts distinctly in the home feed (trophy icon header).

---

## Phase 7 — Profile Completion

### 7.1 Backend Profile Endpoints
- [ ] [B] Add `GET /api/users/{id}/profile/` — full profile for any user (public data only for others).
- [ ] [B] Add `PATCH /api/users/{id}/profile/` — update own profile (full_name, phone_number, date_of_birth, interests).

### 7.2 Android Profile Screens
- [ ] [A] Create `EditProfileFragment` — form to edit name, bio, DOB, interests, photo.
- [ ] [A] Wire "Edit Profile" button on `ProfileFragment`.
- [ ] [A] Create `UserProfileFragment` — for viewing other users' profiles (triggered by tapping username).
- [ ] [A] Add "Posts", "Progress", "Achievements", "Liked" tabs to `ProfileFragment`.
- [ ] [A] Create `UserPostsFragment` — grid/list of user's posts.
- [ ] [A] Create `UserProgressFragment` — list of challenge progress bars across hubs.
- [ ] [A] Create `LikedPostsFragment` — posts user has liked.

---

## Phase 8 — Social Graph

### 8.1 Backend Follow System
- [ ] [B] Add `UserFollow` model (follower → FK User, following → FK User, created_at, unique_together).
- [ ] [B] Migration.
- [ ] [B] Add `POST /api/users/{id}/follow/` and `POST /api/users/{id}/unfollow/`.
- [ ] [B] Add `GET /api/users/{id}/followers/` and `GET /api/users/{id}/following/`.
- [ ] [B] Add `followers_count` and `following_count` to user profile serializer.
- [ ] [B] Add `is_following` flag to user profile serializer (for the requesting user).

### 8.2 Android Follow UI
- [ ] [A] Add Follow/Unfollow button to `UserProfileFragment`.
- [ ] [A] Show follower/following counts on profile header.

---

## Phase 9 — Search

### 9.1 Backend Search
- [ ] [B] Add `GET /api/search/?q=&type=hubs|users|posts` — returns paginated results.
- [ ] [B] Hubs: filter by name, description.
- [ ] [B] Users: filter by username, full_name.
- [ ] [B] Posts: filter by content (public posts only).

### 9.2 Android Search UI
- [ ] [A] Add search icon to `HubsFragment` toolbar (already planned in App_startup.md).
- [ ] [A] Create `SearchFragment` — search input + tabs (Hubs / Users / Posts).
- [ ] [A] Create `SearchViewModel` + `SearchRepository`.
- [ ] [A] Wire search results to existing Hub and User card components.

---

## Phase 10 — Notifications

### 10.1 Backend Notifications
- [ ] [B] Add `Notification` model (recipient, actor, verb, target_type, target_id, is_read, created_at).
- [ ] [B] Migration.
- [ ] [B] Create notification on: post liked, post commented, hub joined, achievement verified, hub announcement.
- [ ] [B] Add `GET /api/notifications/` — paginated, newest first.
- [ ] [B] Add `POST /api/notifications/{id}/read/` and `POST /api/notifications/read-all/`.
- [ ] [B] Add `unread_count` to notifications list response header.

### 10.2 Android Notifications UI
- [ ] [A] Add bell icon to `MainActivity` toolbar with unread badge.
- [ ] [A] Create `NotificationsFragment`.
- [ ] [A] Create `NotificationAdapter`.
- [ ] [A] Poll for unread count on app foreground (or use long-polling / WebSocket later).

---

## Phase 11 — Production Readiness (Later)

- [ ] [B] Migrate from SQLite to PostgreSQL.
- [ ] [B] Replace local media storage with cloud storage (S3 or Cloudinary).
- [ ] [B] Add Firebase Cloud Messaging for push notifications.
- [ ] [B] Implement real OAuth signature verification for social login.
- [ ] [B] Add rate limiting (django-ratelimit or DRF throttling).
- [ ] [B] Set `DEBUG=False`, configure `ALLOWED_HOSTS`, `SECRET_KEY` from env.
- [ ] [B] Add CORS headers (django-cors-headers) for any web clients.
- [ ] [A] Add crash reporting (Firebase Crashlytics).
- [ ] [A] Add analytics (Firebase Analytics).

---

*Last updated: 2026-06-13*
