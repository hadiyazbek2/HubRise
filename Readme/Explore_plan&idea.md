Explore page plan
Right now ExploreFragment is a literal "Coming Soon" stub, so this is a clean slate. Here's the plan for a TikTok/Reels/Shorts-style vertical feed:

Layout — full-screen vertical swipe, one post per screen


┌─────────────────────────┐
│ ▏ progress bar (top)    │ ← thin segmented bar like Stories, optional
│                          │
│                          │
│      VIDEO (autoplay,    │        ●  ❤  142      ← like
│      muted, looped)      │        ●  💬 12        ← comment
│      — or a static       │        ●  ↗            ← share
│      image post —        │        ●  🔇/🔊         ← mute toggle
│      fills the screen    │
│                          │
│ @username · Hub name     │
│ caption text here...     │
└─────────────────────────┘
        ↑ swipe up → next post, swipe down → previous
Video: autoplay on screen-enter, muted by default (tap anywhere to unmute), loops while visible, pauses the instant it's swiped away.
Image posts: shown like a still slide — same overlay, swipe to advance (no timer auto-advance for v1, keep it user-paced).
Right-side action rail reuses the existing like/comment endpoints — no new backend models.
Bottom-left overlay: avatar, @username, hub chip, caption — same info PostAdapter already shows, just restyled for full-bleed.
Where the content comes from
No new content type needed — this plan repurposes your existing Post.media_file. A new lightweight endpoint, e.g. GET /api/explore/feed/, returns public posts (hub is_public=True or no hub) that have media attached, video-first. The one real backend addition: a media_type field (image/video) computed on save instead of Android guessing from the file extension — small, safe, additive migration, same pattern as every prior round.

Android architecture

ViewPager2 (vertical orientation) + RecyclerView.Adapter, one ViewHolder type per media kind.
Video playback via Media3 ExoPlayer (new dependency) — a single shared player instance attached/detached to whichever item is currently on-screen (the standard TikTok-clone pattern; avoids spinning up N players for N feed items).
Play/pause wired to ViewPager2.OnPageChangeCallback + fragment lifecycle (pause on onPause/leaving the tab, resume on return).
Likes/comments/share reuse HomeViewModel/CommentRepository exactly as today — this screen is a different presentation of posts, not a different data model.
Phasing

MVP — vertical swipe feed of existing public media posts, chronological order, autoplay/mute, like+comment+share working, tap-to-open full comments sheet (reuse CommentsBottomSheetFragment).
Later — smarter ranking ("for you" vs "following"), double-tap-to-like animation, view-count tracking.
Later still — a dedicated short-video capture flow (record-in-app) rather than only surfacing whatever was posted via the normal Create Post screen.
Want me to start on the MVP (backend media_type field + feed endpoint + the Android ViewPager2/ExoPlayer screen), or hold here while you think it over?