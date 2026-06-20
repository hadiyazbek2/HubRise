from django.contrib.auth import get_user_model
from rest_framework import serializers

from .models import (
    Challenge,
    ChallengeTemplate,
    Comment,
    CompletionRequest,
    Hub,
    HubMembership,
    Post,
    PostLike,
    TemplateStage,
    UserCountProgress,
    UserStageProgress,
    UserStreakProgress,
)

User = get_user_model()


def get_challenge_progress(challenge, user):
    """Returns (percent_complete: int, summary: str) for a user's progress on a challenge,
    regardless of which of the 3 progress models the challenge uses."""
    if not user or not user.is_authenticated:
        user = None

    if challenge.progress_model == Challenge.MODEL_STAGE:
        total = challenge.stages.count()
        completed = (
            UserStageProgress.objects.filter(
                stage__challenge=challenge, user=user, status=UserStageProgress.STATUS_COMPLETED
            ).count()
            if user
            else 0
        )
        percent = int((completed / total) * 100) if total else 0
        summary = f"{completed}/{total} stages"
        return percent, summary

    if challenge.progress_model == Challenge.MODEL_COUNT:
        config = getattr(challenge, "count_config", None)
        target = config.target_count if config else 0
        progress = UserCountProgress.objects.filter(challenge=challenge, user=user).first() if user else None
        current = progress.current_count if progress else 0
        percent = int((float(current) / float(target)) * 100) if target else 0
        unit = config.unit_label if config else ""
        summary = f"{float(current):g}/{float(target):g} {unit}".strip()
        return percent, summary

    # streak
    config = getattr(challenge, "streak_config", None)
    target = config.target_days if config else 0
    progress = UserStreakProgress.objects.filter(challenge=challenge, user=user).first() if user else None
    total_checkins = progress.total_checkins if progress else 0
    percent = int((total_checkins / target) * 100) if target else 0
    summary = f"{total_checkins}/{target} days"
    return percent, summary


class CommentSerializer(serializers.ModelSerializer):
    author_username = serializers.CharField(source="author.username", read_only=True)
    author_avatar_url = serializers.SerializerMethodField()

    class Meta:
        model = Comment
        fields = ["id", "post", "author", "author_username", "author_avatar_url", "content", "created_at"]
        read_only_fields = ["author", "post", "created_at"]

    def get_author_avatar_url(self, obj: Comment) -> str:
        request = self.context.get("request")
        profile = getattr(obj.author, "profile", None)
        if not profile or not getattr(profile, "profile_picture", None):
            return ""
        try:
            url = profile.profile_picture.url
            return request.build_absolute_uri(url) if request else url
        except Exception:
            return ""


class ChallengeSerializer(serializers.ModelSerializer):
    hub_name = serializers.CharField(source="hub.name", read_only=True)
    created_by_username = serializers.CharField(source="created_by.username", read_only=True)
    summary = serializers.SerializerMethodField()
    percent_complete = serializers.SerializerMethodField()

    class Meta:
        model = Challenge
        fields = [
            "id",
            "hub",
            "hub_name",
            "title",
            "description",
            "progress_model",
            "is_main",
            "created_by",
            "created_by_username",
            "ends_at",
            "created_at",
            "summary",
            "percent_complete",
        ]
        read_only_fields = ["hub", "created_by", "created_at"]

    def get_summary(self, obj: Challenge) -> str:
        request = self.context.get("request")
        _, summary = get_challenge_progress(obj, request.user if request else None)
        return summary

    def get_percent_complete(self, obj: Challenge) -> int:
        request = self.context.get("request")
        percent, _ = get_challenge_progress(obj, request.user if request else None)
        return percent


class MainChallengeDetailSerializer(serializers.Serializer):
    """Nested serializer for main challenge in hub detail with user progress"""
    id = serializers.IntegerField()
    title = serializers.CharField()
    progress_model = serializers.CharField()
    summary = serializers.SerializerMethodField()
    percent_complete = serializers.SerializerMethodField()

    def get_summary(self, obj: Challenge) -> str:
        request = self.context.get("request")
        _, summary = get_challenge_progress(obj, request.user if request else None)
        return summary

    def get_percent_complete(self, obj: Challenge) -> int:
        request = self.context.get("request")
        percent, _ = get_challenge_progress(obj, request.user if request else None)
        return percent


class HubSerializer(serializers.ModelSerializer):
    category_name = serializers.SerializerMethodField()
    is_member = serializers.SerializerMethodField()
    is_creator = serializers.SerializerMethodField()
    created_by_username = serializers.CharField(source="created_by.username", read_only=True)
    main_challenge = serializers.SerializerMethodField()
    cover_image_url = serializers.SerializerMethodField()
    invite_code = serializers.SerializerMethodField()

    class Meta:
        model = Hub
        fields = [
            "id",
            "name",
            "description",
            "category",
            "category_name",
            "members_count",
            "cover_image_url",
            "is_public",
            "created_by",
            "created_by_username",
            "created_at",
            "is_member",
            "is_creator",
            "main_challenge",
            "invite_code",
        ]
        read_only_fields = ["members_count", "created_by", "created_at", "is_member", "is_creator", "main_challenge"]

    def get_invite_code(self, obj: Hub):
        request = self.context.get("request")
        if request and request.user.is_authenticated and obj.created_by_id == request.user.id:
            return obj.invite_code
        return None

    def get_category_name(self, obj: Hub) -> str:
        return obj.category.name if obj.category else ""

    def get_is_member(self, obj: Hub) -> bool:
        request = self.context.get("request")
        if not request or not request.user.is_authenticated:
            return False
        return obj.memberships.filter(user=request.user).exists()

    def get_is_creator(self, obj: Hub) -> bool:
        request = self.context.get("request")
        if not request or not request.user.is_authenticated:
            return False
        return obj.created_by_id == request.user.id

    def get_main_challenge(self, obj: Hub):
        main_challenge = obj.challenges.filter(is_main=True).first()
        if not main_challenge:
            return None
        return MainChallengeDetailSerializer(main_challenge, context=self.context).data

    def get_cover_image_url(self, obj: Hub):
        if not obj.cover_image:
            return ""
        request = self.context.get("request")
        try:
            url = obj.cover_image.url
            return request.build_absolute_uri(url) if request else url
        except Exception:
            return ""


class PostSerializer(serializers.ModelSerializer):
    author_username = serializers.CharField(source="author.username", read_only=True)
    author_avatar_url = serializers.SerializerMethodField()
    hub_name = serializers.SerializerMethodField()
    likes_count = serializers.IntegerField(source="likes.count", read_only=True)
    comments_count = serializers.SerializerMethodField()
    liked_by_me = serializers.SerializerMethodField()
    media_url = serializers.SerializerMethodField()
    challenge_title = serializers.SerializerMethodField()
    validations_count = serializers.IntegerField(source="validations.count", read_only=True)
    validated_by_me = serializers.SerializerMethodField()
    author_wishlist_url = serializers.SerializerMethodField()
    media_type = serializers.SerializerMethodField()

    class Meta:
        model = Post
        fields = [
            "id",
            "author",
            "author_avatar_url",
            "author_username",
            "author_wishlist_url",
            "hub",
            "hub_name",
            "post_type",
            "content",
            "media_url",
            "media_type",
            "is_public",
            "created_at",
            "updated_at",
            "likes_count",
            "comments_count",
            "liked_by_me",
            "challenge",
            "challenge_title",
            "is_trusted",
            "validations_count",
            "validated_by_me",
        ]
        read_only_fields = [
            "author", "created_at", "updated_at", "likes_count", "comments_count", "liked_by_me",
            "challenge", "challenge_title", "is_trusted", "validations_count", "validated_by_me",
            "author_wishlist_url", "media_type",
        ]

    def get_hub_name(self, obj: Post):
        return obj.hub.name if obj.hub else None

    def get_challenge_title(self, obj: Post):
        return obj.challenge.title if obj.challenge else None

    def get_author_wishlist_url(self, obj: Post) -> str:
        profile = getattr(obj.author, "profile", None)
        return profile.wishlist_url if profile else ""

    _VIDEO_EXTS = {".mp4", ".mov", ".mkv", ".webm", ".avi", ".3gp", ".m4v"}
    _IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".heic", ".bmp"}

    def get_media_type(self, obj: Post) -> str:
        if not obj.media_file:
            return ""
        import os
        ext = os.path.splitext(obj.media_file.name)[1].lower()
        if ext in self._VIDEO_EXTS:
            return "video"
        if ext in self._IMAGE_EXTS:
            return "image"
        return ""

    def get_validated_by_me(self, obj: Post) -> bool:
        request = self.context.get("request")
        if not request or not request.user.is_authenticated:
            return False
        return obj.validations.filter(validator=request.user).exists()

    def get_media_url(self, obj: Post) -> str:
        if not obj.media_file:
            return ""
        request = self.context.get("request")
        try:
            url = obj.media_file.url
            return request.build_absolute_uri(url) if request else url
        except Exception:
            return ""

    def get_comments_count(self, obj: Post) -> int:
        return obj.comments.count()

    def get_liked_by_me(self, obj: Post) -> bool:
        request = self.context.get("request")
        if not request or not request.user.is_authenticated:
            return False
        return obj.likes.filter(user=request.user).exists()

    def get_author_avatar_url(self, obj: Post):
        request = self.context.get("request")
        profile = getattr(obj.author, "profile", None)
        if not profile or not getattr(profile, "profile_picture", None):
            return ""
        try:
            pic_url = profile.profile_picture.url
        except Exception:
            return ""
        if not request:
            return pic_url
        try:
            return request.build_absolute_uri(pic_url)
        except Exception:
            return pic_url


class CreatePostSerializer(serializers.ModelSerializer):
    class Meta:
        model = Post
        fields = ["hub", "post_type", "content", "is_public"]
        extra_kwargs = {"hub": {"required": False, "allow_null": True}}

    def validate_hub(self, hub):
        if hub is None:
            return None
        request = self.context["request"]
        if not HubMembership.objects.filter(user=request.user, hub=hub).exists():
            raise serializers.ValidationError("You must join the hub before posting.")
        return hub

    def create(self, validated_data):
        return Post.objects.create(author=self.context["request"].user, **validated_data)


class LikeToggleResponseSerializer(serializers.Serializer):
    liked = serializers.BooleanField()
    likes_count = serializers.IntegerField()


class CompletionRequestSerializer(serializers.ModelSerializer):
    username = serializers.CharField(source="user.username", read_only=True)
    user_avatar_url = serializers.SerializerMethodField()
    challenge_title = serializers.CharField(source="challenge.title", read_only=True)
    hub_id = serializers.IntegerField(source="challenge.hub_id", read_only=True)
    reviewed_by_username = serializers.CharField(source="reviewed_by.username", read_only=True, allow_null=True)

    class Meta:
        model = CompletionRequest
        fields = [
            "id",
            "user",
            "username",
            "user_avatar_url",
            "challenge",
            "challenge_title",
            "hub_id",
            "status",
            "member_note",
            "submitted_at",
            "reviewed_at",
            "reviewed_by",
            "reviewed_by_username",
            "admin_note",
            "announcement_post",
        ]
        read_only_fields = [
            "user", "status", "submitted_at", "reviewed_at", "reviewed_by", "announcement_post",
        ]

    def get_user_avatar_url(self, obj: CompletionRequest):
        request = self.context.get("request")
        profile = getattr(obj.user, "profile", None)
        if not profile or not getattr(profile, "profile_picture", None):
            return ""
        try:
            url = profile.profile_picture.url
            return request.build_absolute_uri(url) if request else url
        except Exception:
            return ""


class TemplateStageSerializer(serializers.ModelSerializer):
    class Meta:
        model = TemplateStage
        fields = ["order_index", "title", "description", "proof_type"]


class ChallengeTemplateSerializer(serializers.ModelSerializer):
    stages = TemplateStageSerializer(many=True, read_only=True)

    class Meta:
        model = ChallengeTemplate
        fields = [
            "id",
            "name",
            "category",
            "progress_model",
            "description",
            "is_official",
            "use_count",
            "stages",
            "count_target",
            "count_unit_label",
            "count_entry_increment",
            "streak_target_days",
            "streak_frequency",
            "streak_grace_days",
        ]
