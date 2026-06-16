# Auth API Contract (Aligned With Frontend Doc)

## Status
The backend is now aligned to your frontend contract and you do **not** need to change frontend request/response shapes.

## What I Did (Implementation Summary)
I implemented two main parts in the backend:

1. Authentication and profile APIs that match your frontend contract exactly.
2. Community APIs for hubs, posts, memberships, and likes.

### 1) Authentication + Profile
- Added JWT auth flow with access and refresh tokens.
- Updated login/signup/social-login request and response formats to match your frontend doc.
- Added availability checks for email and username.
- Added profile picture upload endpoint with multipart support.
- Added profile fields needed by frontend (`full_name`, `date_of_birth`, `phone_number`, `interests`, `profile_picture_url`).

### 2) Community (Hubs + Posts)
- Added new models:
  - `Hub` (with category, visibility, member count, creator, timestamps)
  - `Post` (author, hub, content, optional media URL, privacy level, timestamps)
  - `Challenge` (hub, title, description, target count, main/sub flag, creator, optional end date)
  - `ChallengeProgress` (per-user progress for a challenge)
  - `PostLike` (unique per user/post)
  - `HubMembership` (unique per user/hub)
- Added APIs:
  - User feed from joined hubs
  - Global feed from public hubs
  - Hubs listing + hub creation + single hub detail
  - Recommended hubs from user interests
  - Join/leave hub
  - List posts in a hub
  - Create post
  - Toggle post like
  - List challenges in a hub
  - Create sub-challenges for a hub
  - Increment challenge progress
  - Hub detail now includes `main_challenge` with `title`, `target_count`, and `user_progress`

### 3) Security + Rules Applied
- All new community endpoints require `Authorization: Bearer <access_token>`.
- Private hub posts are protected: non-members get `403`.
- Post creation requires hub membership.
- Like endpoint toggles state (`liked: true/false`) and returns updated likes count.

### 4) Validation + Testing
- Added and ran tests for both auth and community APIs.
- Confirmed migrations and Django checks pass.

## Implemented Endpoints

### 1) Login
`POST /api/auth/login/`

Request:
```json
{
  "email": "user@example.com",
  "password": "StrongPass123!"
}
```

Response `200`:
```json
{
  "access_token": "JWT_ACCESS",
  "refresh_token": "JWT_REFRESH",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "username": "user1",
    "full_name": "User One",
    "date_of_birth": "1999-01-01",
    "phone_number": "+123456",
    "profile_picture_url": null,
    "interests": [1, 2]
  }
}
```

Errors:
- `400 Bad Request` (missing/invalid fields)
- `401 Unauthorized` (wrong credentials)

### 2) Social Login (Google, Facebook, Apple)
`POST /api/auth/social-login/`

Request:
```json
{
  "provider": "google",
  "id_token": "provider_token_here"
}
```

Response `200`: same shape as login (`access_token`, `refresh_token`, `user`).

Note:
- This is still setup-light (no provider secret setup).
- `id_token` is parsed without signature verification for now (prototype mode).

### 3) Check Email Availability
`GET /api/auth/check-email/?email=user@example.com`

Response `200`:
```json
{ "available": true }
```

### 4) Check Username Availability
`GET /api/auth/check-username/?username=myname`

Response `200`:
```json
{ "available": true }
```

### 5) Signup
`POST /api/auth/signup/`

Request:
```json
{
  "email": "user@example.com",
  "password": "StrongPass123!",
  "full_name": "User One",
  "username": "user1",
  "date_of_birth": "1999-01-01",
  "phone_number": "+123456",
  "interests": [1, 2]
}
```

Response `201`: same shape as login (`access_token`, `refresh_token`, `user`).

Errors:
- `400 Bad Request` (validation errors)
- `409 Conflict` (existing email or username)

### 6) Upload Profile Picture
`POST /api/users/{user_id}/profile-picture/`

Headers:
```http
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

Body:
- `image_file`: file

Response `200`:
```json
{
  "profile_picture_url": "http://127.0.0.1:8000/media/profile_pictures/..."
}
```

### 7) Logout
`POST /api/auth/logout/`

Headers:
```http
Authorization: Bearer <access_token>
Content-Type: application/json
```

Request:
```json
{
  "refresh_token": "JWT_REFRESH"
}
```

Response `200`:
```json
{
  "message": "Successfully logged out"
}
```

Errors:
- `400 Bad Request` (missing refresh_token or invalid token)
- `401 Unauthorized` (missing authorization header)

**How it works:**
- Requires valid access token in Authorization header
- Accepts refresh token in request body
- Blacklists the refresh token to prevent reuse
- User is logged out and cannot use the refresh token to get new access tokens

## Extra Endpoint
`POST /api/auth/token/refresh/`
```json
{ "refresh": "JWT_REFRESH" }
```
Returns:
```json
{ "access": "NEW_ACCESS_TOKEN" }
```

## Data Model Support
Backend now stores and returns fields needed by your frontend models:
- `email`
- `username`
- JWT tokens (`access_token`, `refresh_token`)
- `profile_picture_url`
- `full_name`
- `date_of_birth`
- `phone_number`
- `interests` (list of IDs)

## Backend Models Added
- `Interest`
- `UserProfile` (linked 1:1 to Django user)
- `SocialAccount` (provider link table)

## Run
```bash
source .venv/bin/activate
pip install -r requirements.txt
python manage.py migrate
python manage.py runserver
```

## Updated Files
- `accounts/models.py`
- `accounts/serializers.py`
- `accounts/views.py`
- `accounts/urls.py`
- `accounts/admin.py`
- `accounts/tests.py`
- `accounts/migrations/0002_interest_alter_socialaccount_provider_userprofile.py`
- `backend/settings.py`
- `backend/urls.py`

## Community Models Added
- `Hub`
- `Post`
- `PostLike` (`unique_together`: user + post via unique constraint)
- `HubMembership` (`unique_together`: user + hub via unique constraint)

## Community Endpoints (JWT required)
- `GET /api/posts/feed/` posts from hubs where the auth user is a member (paginated, newest first)
- `GET /api/posts/global/` all public hub posts (paginated, newest first)
- `GET /api/hubs/` list hubs (supports `?interest=<interest_id>`)
- `POST /api/hubs/` create a new hub
- `GET /api/hubs/{id}/` get single hub details
- `GET /api/hubs/recommended/` hubs matching auth user interests
- `POST /api/hubs/{id}/join/` join hub
- `POST /api/hubs/{id}/leave/` leave hub
- `GET /api/hubs/{id}/posts/` posts in one hub (private hubs require membership)
- `POST /api/posts/` create post (`hub`, `content`, optional `media_url`)
- `POST /api/posts/{id}/like/` toggle like

### Community API Notes
- All endpoints above require `Authorization: Bearer <access_token>`.
- Pagination format follows DRF (`count`, `next`, `previous`, `results`).
- Hub list/detail responses include `is_member` so frontend can render Join/Joined without extra requests.
- Creating a post requires membership in the target hub.
- Creating a hub automatically creates membership for the creator and starts with `members_count = 1`.
