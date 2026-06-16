from django.conf import settings
from django.db import models


class Interest(models.Model):
    name = models.CharField(max_length=100, unique=True)

    def __str__(self) -> str:
        return self.name


class UserProfile(models.Model):
    user = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="profile",
    )
    full_name = models.CharField(max_length=255)
    bio = models.CharField(max_length=200, blank=True, default="")
    date_of_birth = models.DateField()
    phone_number = models.CharField(max_length=30, blank=True)
    profile_picture = models.FileField(upload_to="profile_pictures/", blank=True)
    interests = models.ManyToManyField(Interest, blank=True, related_name="profiles")
    # Optional wishlist link, surfaced via the "Gift" support button on a member's
    # challenge-completion announcement post.
    wishlist_url = models.URLField(max_length=500, blank=True, default="")
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self) -> str:
        return f"profile:{self.user_id}"


class UserFollow(models.Model):
    follower = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="following",
    )
    following = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="followers",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=["follower", "following"], name="unique_user_follow"),
        ]
        ordering = ["-created_at"]

    def __str__(self) -> str:
        return f"{self.follower_id} → {self.following_id}"


class Notification(models.Model):
    TYPE_FOLLOW = "follow"
    TYPE_LIKE = "like"
    TYPE_COMMENT = "comment"
    TYPE_COMPLETION_SUBMITTED = "completion_submitted"
    TYPE_COMPLETION_APPROVED = "completion_approved"
    TYPE_COMPLETION_REJECTED = "completion_rejected"

    TYPES = [
        (TYPE_FOLLOW, "Follow"),
        (TYPE_LIKE, "Like"),
        (TYPE_COMMENT, "Comment"),
        (TYPE_COMPLETION_SUBMITTED, "Completion Submitted"),
        (TYPE_COMPLETION_APPROVED, "Completion Approved"),
        (TYPE_COMPLETION_REJECTED, "Completion Rejected"),
    ]

    recipient = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="notifications"
    )
    sender = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="sent_notifications"
    )
    notification_type = models.CharField(max_length=20, choices=TYPES)
    post = models.ForeignKey(
        "community.Post", null=True, blank=True, on_delete=models.CASCADE
    )
    challenge = models.ForeignKey(
        "community.Challenge", null=True, blank=True, on_delete=models.CASCADE
    )
    is_read = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self) -> str:
        return f"{self.sender_id} → {self.recipient_id}: {self.notification_type}"


class SocialAccount(models.Model):
    PROVIDER_GOOGLE = "google"
    PROVIDER_FACEBOOK = "facebook"
    PROVIDER_APPLE = "apple"

    PROVIDER_CHOICES = (
        (PROVIDER_GOOGLE, "Google"),
        (PROVIDER_FACEBOOK, "Facebook"),
        (PROVIDER_APPLE, "Apple"),
    )

    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="social_accounts",
    )
    provider = models.CharField(max_length=20, choices=PROVIDER_CHOICES)
    provider_user_id = models.CharField(max_length=255)
    email = models.EmailField(blank=True)
    extra_data = models.JSONField(default=dict, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["provider", "provider_user_id"],
                name="unique_provider_user_id",
            ),
        ]
        indexes = [
            models.Index(fields=["provider", "email"]),
        ]

    def __str__(self) -> str:
        return f"{self.provider}:{self.provider_user_id}"
