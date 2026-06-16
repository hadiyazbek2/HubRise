from django.conf import settings
from django.db import models

from accounts.models import Interest


class Hub(models.Model):
    name = models.CharField(max_length=150)
    description = models.TextField(blank=True)
    category = models.ForeignKey(Interest, null=True, blank=True, on_delete=models.PROTECT, related_name="hubs")
    members_count = models.PositiveIntegerField(default=0)
    cover_image = models.ImageField(upload_to="hub_covers/", blank=True)
    is_public = models.BooleanField(default=True)
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="created_hubs",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    # Layer 1 (peer micro-validation): score a post needs to reach to be marked Trusted.
    peer_validation_threshold = models.DecimalField(max_digits=4, decimal_places=2, default=3.0)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self) -> str:
        return self.name


class Post(models.Model):
    TYPE_REGULAR = "regular"
    TYPE_PROGRESS = "progress_update"
    TYPE_ACHIEVEMENT = "achievement_broadcast"
    TYPE_ANNOUNCEMENT = "admin_announcement"
    TYPE_STAGE_PROOF = "stage_proof"
    TYPE_COUNT_ENTRY = "count_entry"
    TYPE_STREAK_CHECKIN = "streak_checkin"

    POST_TYPE_CHOICES = (
        (TYPE_REGULAR, "Regular"),
        (TYPE_PROGRESS, "Progress Update"),
        (TYPE_ACHIEVEMENT, "Achievement Broadcast"),
        (TYPE_ANNOUNCEMENT, "Admin Announcement"),
        (TYPE_STAGE_PROOF, "Stage Proof"),
        (TYPE_COUNT_ENTRY, "Count Entry"),
        (TYPE_STREAK_CHECKIN, "Streak Check-in"),
    )

    # post_type values that represent a challenge progress action and are
    # therefore eligible for Layer 1 peer validation ("I believe this").
    CHALLENGE_POST_TYPES = (TYPE_STAGE_PROOF, TYPE_COUNT_ENTRY, TYPE_STREAK_CHECKIN)

    author = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="posts",
    )
    hub = models.ForeignKey(Hub, null=True, blank=True, on_delete=models.SET_NULL, related_name="posts")
    post_type = models.CharField(max_length=30, choices=POST_TYPE_CHOICES, default=TYPE_REGULAR)
    content = models.TextField()
    media_file = models.FileField(upload_to="post_media/", blank=True)
    is_public = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    # Set only for auto-generated challenge-progress posts (stage_proof/count_entry/streak_checkin).
    challenge = models.ForeignKey(
        "Challenge", null=True, blank=True, on_delete=models.SET_NULL, related_name="posts"
    )
    linked_stage = models.ForeignKey(
        "ChallengeStage", null=True, blank=True, on_delete=models.SET_NULL, related_name="proof_posts"
    )
    # Layer 1 (peer micro-validation)
    weighted_validation_score = models.DecimalField(max_digits=6, decimal_places=2, default=0)
    is_trusted = models.BooleanField(default=False)
    trusted_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self) -> str:
        return f"post:{self.id}"


class PostLike(models.Model):
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="post_likes",
    )
    post = models.ForeignKey(Post, on_delete=models.CASCADE, related_name="likes")
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=["user", "post"], name="unique_user_post_like"),
        ]


class HubMembership(models.Model):
    ROLE_MEMBER = "member"
    ROLE_ADMIN = "admin"
    ROLE_MODERATOR = "moderator"

    ROLE_CHOICES = (
        (ROLE_MEMBER, "Member"),
        (ROLE_ADMIN, "Admin"),
        (ROLE_MODERATOR, "Moderator"),
    )

    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="hub_memberships",
    )
    hub = models.ForeignKey(Hub, on_delete=models.CASCADE, related_name="memberships")
    role = models.CharField(max_length=20, choices=ROLE_CHOICES, default=ROLE_MEMBER)
    joined_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=["user", "hub"], name="unique_user_hub_membership"),
        ]


class Comment(models.Model):
    post = models.ForeignKey(Post, on_delete=models.CASCADE, related_name="comments")
    author = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="comments",
    )
    content = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["created_at"]

    def __str__(self) -> str:
        return f"comment:{self.id}"


class Challenge(models.Model):
    MODEL_STAGE = "stage"
    MODEL_COUNT = "count"
    MODEL_STREAK = "streak"
    MODEL_CHOICES = (
        (MODEL_STAGE, "Stage-Based"),
        (MODEL_COUNT, "Count-Based"),
        (MODEL_STREAK, "Streak-Based"),
    )

    hub = models.ForeignKey(Hub, on_delete=models.CASCADE, related_name="challenges")
    title = models.CharField(max_length=100)
    description = models.TextField(blank=True)
    progress_model = models.CharField(max_length=10, choices=MODEL_CHOICES, default=MODEL_COUNT)
    is_main = models.BooleanField(default=False)
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="created_challenges",
    )
    ends_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self) -> str:
        return self.title


# ---------------------------------------------------------------------------
# Model A — Stage-Based: ordered stages a member completes sequentially.
# ---------------------------------------------------------------------------

class ChallengeStage(models.Model):
    PROOF_PHOTO = "photo"
    PROOF_VIDEO = "video"
    PROOF_TEXT = "text"
    PROOF_NUMBER = "number"
    PROOF_ANY = "any"
    PROOF_CHOICES = (
        (PROOF_PHOTO, "Photo"),
        (PROOF_VIDEO, "Video"),
        (PROOF_TEXT, "Text"),
        (PROOF_NUMBER, "Number"),
        (PROOF_ANY, "Any"),
    )

    challenge = models.ForeignKey(Challenge, on_delete=models.CASCADE, related_name="stages")
    order_index = models.PositiveIntegerField()
    title = models.CharField(max_length=120)
    description = models.TextField(blank=True)
    proof_type = models.CharField(max_length=10, choices=PROOF_CHOICES, default=PROOF_ANY)
    proof_prompt = models.CharField(max_length=255, blank=True)
    is_milestone = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["order_index"]
        constraints = [
            models.UniqueConstraint(fields=["challenge", "order_index"], name="unique_challenge_stage_order"),
        ]

    def __str__(self) -> str:
        return f"{self.challenge.title} - Stage {self.order_index}: {self.title}"


class UserStageProgress(models.Model):
    STATUS_NOT_STARTED = "not_started"
    STATUS_IN_PROGRESS = "in_progress"
    STATUS_COMPLETED = "completed"
    STATUS_CHOICES = (
        (STATUS_NOT_STARTED, "Not Started"),
        (STATUS_IN_PROGRESS, "In Progress"),
        (STATUS_COMPLETED, "Completed"),
    )

    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="stage_progress")
    stage = models.ForeignKey(ChallengeStage, on_delete=models.CASCADE, related_name="progress")
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default=STATUS_NOT_STARTED)
    proof_post = models.ForeignKey(Post, null=True, blank=True, on_delete=models.SET_NULL)
    completed_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=["user", "stage"], name="unique_user_stage_progress"),
        ]

    def __str__(self) -> str:
        return f"{self.user.username} - {self.stage}: {self.status}"


# ---------------------------------------------------------------------------
# Model B — Count-Based: numeric target reached via logged entries.
# ---------------------------------------------------------------------------

class ChallengeCountConfig(models.Model):
    challenge = models.OneToOneField(Challenge, on_delete=models.CASCADE, related_name="count_config")
    target_count = models.DecimalField(max_digits=10, decimal_places=2)
    unit_label = models.CharField(max_length=50, blank=True, default="")
    entry_increment = models.DecimalField(max_digits=10, decimal_places=2, default=1)
    require_proof_per_entry = models.BooleanField(default=False)
    # False (default): each logged amount is added to the running total — good for
    # tally-style goals (books read, posts published, separate workout sessions).
    # True: each logged amount IS the new total — good for goals where the member
    # already tracks a cumulative number elsewhere (total km run, total $ saved).
    is_cumulative = models.BooleanField(default=False)

    def __str__(self) -> str:
        return f"{self.challenge.title} count config"


class UserCountProgress(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="count_progress")
    challenge = models.ForeignKey(Challenge, on_delete=models.CASCADE, related_name="count_progress_entries")
    current_count = models.DecimalField(max_digits=10, decimal_places=2, default=0)
    is_complete = models.BooleanField(default=False)
    completed_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=["user", "challenge"], name="unique_user_count_progress"),
        ]

    def __str__(self) -> str:
        return f"{self.user.username} - {self.challenge.title}: {self.current_count}"


# ---------------------------------------------------------------------------
# Model C — Streak-Based: consecutive check-ins toward a target day count.
# ---------------------------------------------------------------------------

class ChallengeStreakConfig(models.Model):
    FREQ_DAILY = "daily"
    FREQ_WEEKLY = "weekly"
    FREQ_CHOICES = (
        (FREQ_DAILY, "Daily"),
        (FREQ_WEEKLY, "Weekly"),
    )

    challenge = models.OneToOneField(Challenge, on_delete=models.CASCADE, related_name="streak_config")
    target_days = models.PositiveIntegerField()
    frequency = models.CharField(max_length=10, choices=FREQ_CHOICES, default=FREQ_DAILY)
    grace_days = models.PositiveIntegerField(default=0)
    require_proof = models.BooleanField(default=False)

    def __str__(self) -> str:
        return f"{self.challenge.title} streak config"


class UserStreakProgress(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="streak_progress")
    challenge = models.ForeignKey(Challenge, on_delete=models.CASCADE, related_name="streak_progress_entries")
    current_streak = models.PositiveIntegerField(default=0)
    longest_streak = models.PositiveIntegerField(default=0)
    total_checkins = models.PositiveIntegerField(default=0)
    checkin_calendar = models.JSONField(default=list)
    last_checkin_date = models.DateField(null=True, blank=True)
    is_complete = models.BooleanField(default=False)

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=["user", "challenge"], name="unique_user_streak_progress"),
        ]

    def __str__(self) -> str:
        return f"{self.user.username} - {self.challenge.title}: streak {self.current_streak}"


# ---------------------------------------------------------------------------
# Layer 1 — Peer Micro-Validation ("I believe this")
# ---------------------------------------------------------------------------

class PostValidation(models.Model):
    post = models.ForeignKey(Post, on_delete=models.CASCADE, related_name="validations")
    validator = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="post_validations"
    )
    weight = models.DecimalField(max_digits=3, decimal_places=2)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=["post", "validator"], name="unique_post_validator"),
        ]

    def __str__(self) -> str:
        return f"{self.validator.username} validated post:{self.post_id} (weight {self.weight})"


# ---------------------------------------------------------------------------
# Layer 3 — Admin Final Review
# ---------------------------------------------------------------------------

class CompletionRequest(models.Model):
    STATUS_PENDING = "pending"
    STATUS_APPROVED = "approved"
    STATUS_REJECTED = "rejected"
    STATUS_CHOICES = (
        (STATUS_PENDING, "Pending"),
        (STATUS_APPROVED, "Approved"),
        (STATUS_REJECTED, "Rejected"),
    )

    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="completion_requests")
    challenge = models.ForeignKey(Challenge, on_delete=models.CASCADE, related_name="completion_requests")
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default=STATUS_PENDING)
    member_note = models.TextField(blank=True)
    submitted_at = models.DateTimeField(auto_now_add=True)
    reviewed_at = models.DateTimeField(null=True, blank=True)
    reviewed_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.SET_NULL, related_name="+"
    )
    admin_note = models.TextField(blank=True)
    announcement_post = models.ForeignKey(Post, null=True, blank=True, on_delete=models.SET_NULL, related_name="+")

    class Meta:
        ordering = ["-submitted_at"]
        constraints = [
            models.UniqueConstraint(
                fields=["user", "challenge"],
                condition=models.Q(status="pending"),
                name="unique_pending_completion_request",
            ),
        ]

    def __str__(self) -> str:
        return f"{self.user.username} - {self.challenge.title}: {self.status}"


# ---------------------------------------------------------------------------
# Template System — read-only blueprints applied at challenge-creation time.
# Applying a template copies its stages/config into the new challenge; the
# new rows are never linked back to the template (it stays untouched).
# ---------------------------------------------------------------------------

class ChallengeTemplate(models.Model):
    CATEGORY_FITNESS = "fitness"
    CATEGORY_FINANCE = "finance"
    CATEGORY_LEARNING = "learning"
    CATEGORY_READING = "reading"
    CATEGORY_MINDFULNESS = "mindfulness"
    CATEGORY_CREATIVE = "creative"
    CATEGORY_CAREER = "career"
    CATEGORY_OTHER = "other"
    CATEGORY_CHOICES = (
        (CATEGORY_FITNESS, "Fitness"),
        (CATEGORY_FINANCE, "Finance"),
        (CATEGORY_LEARNING, "Learning"),
        (CATEGORY_READING, "Reading"),
        (CATEGORY_MINDFULNESS, "Mindfulness"),
        (CATEGORY_CREATIVE, "Creative"),
        (CATEGORY_CAREER, "Career"),
        (CATEGORY_OTHER, "Other"),
    )

    name = models.CharField(max_length=120)
    category = models.CharField(max_length=20, choices=CATEGORY_CHOICES, default=CATEGORY_OTHER)
    progress_model = models.CharField(max_length=10, choices=Challenge.MODEL_CHOICES)
    description = models.TextField(blank=True)
    is_official = models.BooleanField(default=False)
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.SET_NULL, related_name="+"
    )
    use_count = models.PositiveIntegerField(default=0)

    # Default config snapshots (templates aren't linked to live Stage/Config rows).
    count_target = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True)
    count_unit_label = models.CharField(max_length=50, blank=True, default="")
    count_entry_increment = models.DecimalField(max_digits=10, decimal_places=2, default=1)
    streak_target_days = models.PositiveIntegerField(null=True, blank=True)
    streak_frequency = models.CharField(max_length=10, default=ChallengeStreakConfig.FREQ_DAILY)
    streak_grace_days = models.PositiveIntegerField(default=0)

    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-use_count", "name"]

    def __str__(self) -> str:
        return self.name


class TemplateStage(models.Model):
    template = models.ForeignKey(ChallengeTemplate, on_delete=models.CASCADE, related_name="stages")
    order_index = models.PositiveIntegerField()
    title = models.CharField(max_length=120)
    description = models.TextField(blank=True)
    proof_type = models.CharField(max_length=10, choices=ChallengeStage.PROOF_CHOICES, default=ChallengeStage.PROOF_ANY)

    class Meta:
        ordering = ["order_index"]

    def __str__(self) -> str:
        return f"{self.template.name} - Stage {self.order_index}: {self.title}"
