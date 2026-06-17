# HubRise — Explore Page Plan & Ideas

## Concept

The Explore tab is a TikTok / Instagram Reels / YouTube Shorts-style vertical full-screen swipe feed surfacing public posts that have media attached (video-first, image fallback). It replaces the "Coming Soon" stub that occupied the second slot in the bottom nav.

---

## Screen layout

```
┌─────────────────────────┐
│                          │
│                          │
│                          │
│     VIDEO (autoplay,     │       ❤  142   ← like (tap)
│     muted, looped)       │       💬 12    ← comment (opens sheet)
│         — or —           │       ↗        ← share
│     static IMAGE         │
│     (fills screen,       │
│      crop-zoom)          │
│                          │
│ 🔇 ── mute badge (tap)  │
│                          │
│ @username   Hub name ●   │
│ Caption text here...     │
│ ──────────────────────── │
│  ■ ■ ■  bottom nav bar  │
└─────────────────────────┘
       ↑ swipe up = next, swipe down = previous
```

### Behavior
| Element | Behavior |
|---|---|
| Video | Autoplays muted on page-enter, loops, pauses on swipe-away |
| Image | Shows full-screen (crop-zoom), held until user swipes |
| Mute badge | Top-right; tap to toggle sound on/off |
| Right action rail | Like (filled/outline), comment count (opens CommentsBottomSheet), share |
| Bottom overlay | Avatar chip · @username · hub name pill · caption text |
| Double-tap | Like shorthand animation (Phase B) |

---

## Architecture decisions

### Content source
Reuses existing `Post.media_file` — no new content type. Backend endpoint `GET /api/explore/feed/` returns public posts (hub `is_public=True`, or personal public posts) that have a media file attached, ordered video-first then by newest.

A `media_type` field is exposed via `PostSerializer` (computed from file extension — no schema migration needed):
- `"video"` for .mp4/.mov/.mkv/.webm etc.
- `"image"` for .jpg/.png/.gif/.webp etc.
- `""` (empty) for posts without media

### Android: single-player pattern
One `ExoPlayer` instance created and owned by `ExploreFragment`. The adapter holds a reference; on page change the player is attached to the incoming page's `PlayerView` and detached from the outgoing one. Keeps memory usage flat regardless of feed length.

### Engagement
Reuses existing like (`POST /api/posts/<id>/like/`) and comment infrastructure. No new backend models.

---

## Phasing

| Phase | What ships | Status |
|---|---|---|
| **MVP** | Vertical feed, video autoplay/mute, image fallback, like/comment/share, pagination | ✅ This round |
| B | Double-tap to like animation, view-count tracking per post, "For You" ranking signal | ⏸ Later |
| C | Dedicated in-app short-video capture flow | ⏸ Later |

---

## Files changed (MVP)

### Backend
- `community/serializers.py` — `PostSerializer` gains `media_type` SerializerMethodField
- `community/views.py` — new `ExploreListView`
- `community/urls.py` — `path("explore/feed/", ...)`

### Android
- `app/build.gradle.kts` — `media3-exoplayer:1.3.1` + `media3-ui:1.3.1`
- `data/model/Post.kt` — `mediaType: String` field
- `data/api/ExploreApiService.kt` — Retrofit interface for `GET api/explore/feed/`
- `data/repository/ExploreRepository.kt` — paginated feed fetch
- `ui/explore/ExploreViewModel.kt` — `loadFeed()`, `loadMore()`, `toggleLike()` (optimistic)
- `res/layout/fragment_explore.xml` — black FrameLayout with ViewPager2 (vertical), loading spinner, empty state
- `res/layout/item_explore_post.xml` — `PlayerView` + `ImageView` (one visible at a time), bottom gradient, right action rail (❤️/💬/↗), bottom-left info (avatar / @username / hub chip / caption), top-right mute badge
- `res/drawable/gradient_bottom_overlay.xml` — transparent→semi-black gradient for overlay readability
- `res/drawable/ic_volume_off.xml`, `ic_volume_on.xml`, `circle_semi_dark.xml` — new icons
- `ui/explore/ExploreAdapter.kt` — single shared `ExoPlayer` attached/detached per page change; `setCurrentPosition()` triggers bind for active page, `onViewRecycled()` detaches player; mute toggle stored as adapter-level `isMuted` flag
- `ui/explore/ExploreFragment.kt` — creates `ExoPlayer`, passes to adapter; `ViewPager2.OnPageChangeCallback` calls `setCurrentPosition()`; `onPause`→`pausePlayer()`, `onResume`→`resumePlayer()`, `onDestroyView`→`releasePlayer()`; `onNearEnd` lambda triggers `viewModel.loadMore()`

**Verified:** `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL, no new warnings. `python manage.py check` → no issues. Backend smoke test: `GET /api/explore/feed/` returns `200`, `media_type: "video"` computed correctly from file extension.
