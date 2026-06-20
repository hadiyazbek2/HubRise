from datetime import timedelta
from decimal import Decimal, InvalidOperation

from django.contrib.auth import get_user_model
from django.db import transaction
from django.db.models import Count, F, Q, Sum
from django.shortcuts import get_object_or_404
from django.utils import timezone
from django.utils.dateparse import parse_datetime
from rest_framework import generics, serializers, status
from rest_framework.pagination import PageNumberPagination
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.models import Notification, UserFollow, UserProfile

from .models import (
    Challenge,
    ChallengeCountConfig,
    ChallengeStage,
    ChallengeStreakConfig,
    ChallengeTemplate,
    Comment,
    CompletionRequest,
    Hub,
    HubMembership,
    Post,
    PostLike,
    PostValidation,
    UserCountProgress,
    UserStageProgress,
    UserStreakProgress,
)
from .serializers import (
    ChallengeTemplateSerializer,
    CommentSerializer,
    CompletionRequestSerializer,
    CreatePostSerializer,
    HubSerializer,
    PostSerializer,
    ChallengeSerializer,
)

User = get_user_model()


class DefaultPagination(PageNumberPagination):
    page_size = 10
    page_size_query_param = "page_size"
    max_page_size = 50


def ensure_hub_access(user, hub) -> bool:
    if hub is None:
        return True  # Personal posts (no hub) are accessible to their author
    return (
        hub.is_public
        or hub.created_by_id == user.id
        or HubMembership.objects.filter(user=user, hub=hub).exists()
    )


class FeedPostsView(generics.ListAPIView):
    serializer_class = PostSerializer
    pagination_class = DefaultPagination

    def get_queryset(self):
        user = self.request.user
        following_ids = UserFollow.objects.filter(follower=user).values_list("following_id", flat=True)
        return (
            Post.objects.filter(
                Q(hub__is_public=True)
                | Q(hub__memberships__user=user)
                | Q(hub__isnull=True, author=user)
                | Q(hub__isnull=True, author__in=following_ids, is_public=True)
            )
            .select_related("author", "hub")
            .prefetch_related("likes")
            .distinct()
            .order_by("-created_at")
        )


class GlobalPostsView(generics.ListAPIView):
    serializer_class = PostSerializer
    pagination_class = DefaultPagination

    def get_queryset(self):
        return (
            Post.objects.filter(hub__is_public=True)
            .select_related("author", "hub")
            .prefetch_related("likes")
            .order_by("-created_at")
        )


class HubListView(generics.ListCreateAPIView):
    serializer_class = HubSerializer
    pagination_class = DefaultPagination

    def get_queryset(self):
        user = self.request.user
        queryset = (
            Hub.objects.filter(
                Q(is_public=True) | Q(memberships__user=user)
            )
            .distinct()
            .select_related("category", "created_by")
        )
        interest_id = self.request.query_params.get("interest")
        if interest_id:
            queryset = queryset.filter(category_id=interest_id)
        return queryset

    @transaction.atomic
    def perform_create(self, serializer):
        hub = serializer.save(created_by=self.request.user, members_count=1)
        HubMembership.objects.get_or_create(
            user=self.request.user,
            hub=hub,
            defaults={"role": HubMembership.ROLE_ADMIN},
        )


class HubDetailView(generics.RetrieveAPIView):
    serializer_class = HubSerializer
    lookup_url_kwarg = "id"

    def get_queryset(self):
        return Hub.objects.select_related("category", "created_by")

    def retrieve(self, request, *args, **kwargs):
        hub = self.get_object()
        if not ensure_hub_access(request.user, hub):
            return Response(
                {"detail": "You do not have access to this hub."},
                status=status.HTTP_403_FORBIDDEN,
            )
        serializer = self.get_serializer(hub)
        return Response(serializer.data, status=status.HTTP_200_OK)


class RecommendedHubsView(generics.ListAPIView):
    serializer_class = HubSerializer
    pagination_class = DefaultPagination

    def get_queryset(self):
        profile = UserProfile.objects.filter(user=self.request.user).prefetch_related("interests").first()
        if not profile:
            return Hub.objects.none()
        interest_ids = profile.interests.values_list("id", flat=True)
        return (
            Hub.objects.filter(category_id__in=interest_ids, is_public=True)
            .select_related("category", "created_by")
        )


MAX_HUB_MEMBERSHIPS = 20


class JoinHubView(APIView):
    def post(self, request, id: int):
        hub = get_object_or_404(Hub, id=id)
        already_member = HubMembership.objects.filter(user=request.user, hub=hub).exists()
        if not hub.is_public and not already_member:
            return Response(
                {"detail": "This hub is private. You need an invitation to join."},
                status=status.HTTP_403_FORBIDDEN,
            )
        current_count = HubMembership.objects.filter(user=request.user).count()
        if not already_member and current_count >= MAX_HUB_MEMBERSHIPS:
            return Response(
                {"detail": f"You cannot join more than {MAX_HUB_MEMBERSHIPS} hubs."},
                status=status.HTTP_403_FORBIDDEN,
            )
        membership, created = HubMembership.objects.get_or_create(user=request.user, hub=hub)
        if created:
            Hub.objects.filter(id=hub.id).update(members_count=F("members_count") + 1)
            hub.refresh_from_db(fields=["members_count"])
        return Response(
            {"joined": True, "members_count": hub.members_count},
            status=status.HTTP_200_OK,
        )


class LeaveHubView(APIView):
    def post(self, request, id: int):
        hub = get_object_or_404(Hub, id=id)
        deleted_count, _ = HubMembership.objects.filter(user=request.user, hub=hub).delete()
        if deleted_count:
            Hub.objects.filter(id=hub.id, members_count__gt=0).update(members_count=F("members_count") - 1)
            hub.refresh_from_db(fields=["members_count"])
        return Response(
            {"joined": False, "members_count": hub.members_count},
            status=status.HTTP_200_OK,
        )


class JoinByInviteCodeView(APIView):
    def post(self, request):
        code = request.data.get("code", "").strip().upper()
        if not code:
            return Response({"detail": "Invite code is required."}, status=status.HTTP_400_BAD_REQUEST)
        hub = get_object_or_404(Hub, invite_code=code)
        if HubMembership.objects.filter(user=request.user, hub=hub).exists():
            return Response({"detail": "You are already a member of this hub."}, status=status.HTTP_400_BAD_REQUEST)
        current_count = HubMembership.objects.filter(user=request.user).count()
        if current_count >= MAX_HUB_MEMBERSHIPS:
            return Response(
                {"detail": f"You cannot join more than {MAX_HUB_MEMBERSHIPS} hubs."},
                status=status.HTTP_403_FORBIDDEN,
            )
        HubMembership.objects.create(user=request.user, hub=hub)
        Hub.objects.filter(id=hub.id).update(members_count=F("members_count") + 1)
        hub.refresh_from_db()
        return Response(HubSerializer(hub, context={"request": request}).data, status=status.HTTP_200_OK)


class ResetInviteCodeView(APIView):
    def post(self, request, id: int):
        hub = get_object_or_404(Hub, id=id)
        if hub.created_by_id != request.user.id:
            return Response({"detail": "Only the hub creator can reset the invite code."}, status=status.HTTP_403_FORBIDDEN)
        import secrets, string
        alphabet = string.ascii_uppercase + string.digits
        new_code = "".join(secrets.choice(alphabet) for _ in range(8))
        while Hub.objects.filter(invite_code=new_code).exclude(id=hub.id).exists():
            new_code = "".join(secrets.choice(alphabet) for _ in range(8))
        hub.invite_code = new_code
        hub.save(update_fields=["invite_code"])
        return Response({"invite_code": hub.invite_code})


class HubPostsView(generics.ListAPIView):
    serializer_class = PostSerializer
    pagination_class = DefaultPagination

    def list(self, request, *args, **kwargs):
        hub = get_object_or_404(Hub, id=self.kwargs["id"])
        if not ensure_hub_access(request.user, hub):
            return Response(
                {"detail": "You do not have access to this hub."},
                status=status.HTTP_403_FORBIDDEN,
            )
        queryset = (
            Post.objects.filter(hub=hub)
            .select_related("author", "hub")
            .prefetch_related("likes")
            .order_by("-created_at")
        )
        page = self.paginate_queryset(queryset)
        if page is not None:
            serializer = self.get_serializer(page, many=True)
            return self.get_paginated_response(serializer.data)
        serializer = self.get_serializer(queryset, many=True)
        return Response(serializer.data)


class CreatePostView(generics.CreateAPIView):
    """Creating a post is the *only* way to make challenge progress.
    If `challenge` is present in the body, this performs the underlying
    progress mutation (stage complete / count log / streak check-in) and
    tags the resulting post accordingly. Otherwise it's a normal post."""

    serializer_class = CreatePostSerializer

    def create(self, request, *args, **kwargs):
        challenge_id = request.data.get("challenge")
        if challenge_id:
            return self._create_progress_post(request, challenge_id)

        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        post = serializer.save()
        response_data = PostSerializer(post, context={"request": request}).data
        return Response(response_data, status=status.HTTP_201_CREATED)

    def _create_progress_post(self, request, challenge_id):
        challenge = get_object_or_404(Challenge, id=challenge_id)
        if not HubMembership.objects.filter(user=request.user, hub=challenge.hub).exists():
            return Response({"detail": "You must join the hub to log progress."}, status=status.HTTP_403_FORBIDDEN)

        content = str(request.data.get("content", "")).strip()

        if challenge.progress_model == Challenge.MODEL_STAGE:
            return self._create_stage_post(request, challenge, content)
        if challenge.progress_model == Challenge.MODEL_COUNT:
            return self._create_count_post(request, challenge, content)
        return self._create_streak_post(request, challenge, content)

    def _create_stage_post(self, request, challenge, content):
        stage_id = request.data.get("stage")
        if not stage_id:
            return Response({"detail": "stage is required for this challenge."}, status=status.HTTP_400_BAD_REQUEST)
        stage = get_object_or_404(ChallengeStage, id=stage_id, challenge=challenge)

        earlier_incomplete = (
            ChallengeStage.objects.filter(challenge=challenge, order_index__lt=stage.order_index)
            .exclude(progress__user=request.user, progress__status=UserStageProgress.STATUS_COMPLETED)
            .exists()
        )
        if earlier_incomplete:
            return Response({"detail": "Complete earlier stages first."}, status=status.HTTP_400_BAD_REQUEST)

        progress, _ = UserStageProgress.objects.get_or_create(user=request.user, stage=stage)
        if progress.status == UserStageProgress.STATUS_COMPLETED:
            return Response({"detail": "This stage is already completed."}, status=status.HTTP_400_BAD_REQUEST)
        progress.status = UserStageProgress.STATUS_COMPLETED
        progress.completed_at = timezone.now()
        progress.save(update_fields=["status", "completed_at", "updated_at"])

        post = Post.objects.create(
            author=request.user,
            hub=challenge.hub,
            post_type=Post.TYPE_STAGE_PROOF,
            challenge=challenge,
            linked_stage=stage,
            content=content or f"✅ Completed stage {stage.order_index}: {stage.title} — {challenge.title}",
        )
        return Response(PostSerializer(post, context={"request": request}).data, status=status.HTTP_201_CREATED)

    def _create_count_post(self, request, challenge, content):
        config = getattr(challenge, "count_config", None)
        if config is None:
            return Response({"detail": "This challenge has no count configuration."}, status=status.HTTP_400_BAD_REQUEST)

        amount = request.data.get("amount")
        try:
            amount_value = Decimal(str(amount)) if amount is not None else config.entry_increment
        except InvalidOperation:
            return Response({"detail": "Invalid amount."}, status=status.HTTP_400_BAD_REQUEST)
        if amount_value <= 0:
            return Response({"detail": "amount must be positive."}, status=status.HTTP_400_BAD_REQUEST)

        progress, _ = UserCountProgress.objects.get_or_create(user=request.user, challenge=challenge)
        if config.is_cumulative:
            # The member already tracks their own running total (km run, $ saved) and
            # just reports it — each entry IS the new total, not an amount to add.
            progress.current_count = amount_value
        else:
            progress.current_count = progress.current_count + amount_value
        if not progress.is_complete and progress.current_count >= config.target_count:
            progress.is_complete = True
            progress.completed_at = timezone.now()
        progress.save()

        unit = f" {config.unit_label}" if config.unit_label else ""
        if config.is_cumulative:
            default_content = (
                f"📈 Now at {float(progress.current_count):g}/{float(config.target_count):g}{unit}"
                f" — {challenge.title}"
            )
        else:
            default_content = (
                f"📈 Logged +{float(amount_value):g} — {float(progress.current_count):g}/{float(config.target_count):g}{unit}"
                f" — {challenge.title}"
            )
        post = Post.objects.create(
            author=request.user,
            hub=challenge.hub,
            post_type=Post.TYPE_COUNT_ENTRY,
            challenge=challenge,
            content=content or default_content,
        )
        return Response(PostSerializer(post, context={"request": request}).data, status=status.HTTP_201_CREATED)

    def _create_streak_post(self, request, challenge, content):
        config = getattr(challenge, "streak_config", None)
        if config is None:
            return Response({"detail": "This challenge has no streak configuration."}, status=status.HTTP_400_BAD_REQUEST)

        today = timezone.localdate()
        progress, _ = UserStreakProgress.objects.get_or_create(user=request.user, challenge=challenge)
        if progress.last_checkin_date == today:
            return Response({"detail": "You already checked in today."}, status=status.HTTP_400_BAD_REQUEST)

        if progress.last_checkin_date is not None:
            gap_days = (today - progress.last_checkin_date).days
            if gap_days > 1 + config.grace_days:
                progress.current_streak = 0

        progress.current_streak += 1
        progress.longest_streak = max(progress.longest_streak, progress.current_streak)
        progress.total_checkins += 1
        progress.checkin_calendar = list(progress.checkin_calendar) + [today.isoformat()]
        progress.last_checkin_date = today
        if not progress.is_complete and progress.total_checkins >= config.target_days:
            progress.is_complete = True
        progress.save()

        default_content = (
            f"🔥 Day {progress.current_streak} streak — {progress.total_checkins}/{config.target_days} days"
            f" — {challenge.title}"
        )
        post = Post.objects.create(
            author=request.user,
            hub=challenge.hub,
            post_type=Post.TYPE_STREAK_CHECKIN,
            challenge=challenge,
            content=content or default_content,
        )
        return Response(PostSerializer(post, context={"request": request}).data, status=status.HTTP_201_CREATED)


class ToggleLikeView(APIView):
    def post(self, request, id: int):
        post = get_object_or_404(Post, id=id)
        if not ensure_hub_access(request.user, post.hub):
            return Response(
                {"detail": "You do not have access to this post."},
                status=status.HTTP_403_FORBIDDEN,
            )

        like, created = PostLike.objects.get_or_create(user=request.user, post=post)
        if created:
            liked = True
            if post.author != request.user:
                from accounts.models import Notification
                Notification.objects.create(
                    recipient=post.author,
                    sender=request.user,
                    notification_type=Notification.TYPE_LIKE,
                    post=post,
                )
        else:
            like.delete()
            liked = False
        likes_count = post.likes.count()
        return Response(
            {"liked": liked, "likes_count": likes_count},
            status=status.HTTP_200_OK,
        )


def has_completed_challenge(user, challenge) -> bool:
    """True if `user` has fully completed `challenge`, regardless of its progress model."""
    if challenge.progress_model == Challenge.MODEL_COUNT:
        return UserCountProgress.objects.filter(user=user, challenge=challenge, is_complete=True).exists()
    if challenge.progress_model == Challenge.MODEL_STREAK:
        return UserStreakProgress.objects.filter(user=user, challenge=challenge, is_complete=True).exists()
    total = challenge.stages.count()
    if total == 0:
        return False
    completed = UserStageProgress.objects.filter(
        stage__challenge=challenge, user=user, status=UserStageProgress.STATUS_COMPLETED
    ).count()
    return completed >= total


class PostValidateView(APIView):
    """Layer 1 — peer micro-validation ("I believe this"). Only available on
    auto-generated challenge-progress posts (stage_proof/count_entry/streak_checkin)."""

    def post(self, request, id: int):
        post = get_object_or_404(Post, id=id)

        if post.challenge_id is None or post.post_type not in Post.CHALLENGE_POST_TYPES:
            return Response(
                {"detail": "Validation is only available for challenge-related posts."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if post.author_id == request.user.id:
            return Response({"detail": "You cannot validate your own post."}, status=status.HTTP_403_FORBIDDEN)

        membership = HubMembership.objects.filter(user=request.user, hub=post.hub).first()
        if not membership:
            return Response({"detail": "You must be a hub member to validate posts."}, status=status.HTTP_403_FORBIDDEN)

        if PostValidation.objects.filter(post=post, validator=request.user).exists():
            return Response({"detail": "Already validated."}, status=status.HTTP_409_CONFLICT)

        if (timezone.now() - membership.joined_at) < timedelta(days=7):
            weight = Decimal("0.5")
        elif has_completed_challenge(request.user, post.challenge):
            weight = Decimal("1.5")
        else:
            weight = Decimal("1.0")

        PostValidation.objects.create(post=post, validator=request.user, weight=weight)

        new_score = post.validations.aggregate(total=Sum("weight"))["total"] or Decimal("0")
        post.weighted_validation_score = new_score
        threshold = post.hub.peer_validation_threshold if post.hub else Decimal("3.0")
        if not post.is_trusted and new_score >= threshold:
            post.is_trusted = True
            post.trusted_at = timezone.now()
        post.save(update_fields=["weighted_validation_score", "is_trusted", "trusted_at"])

        return Response({
            "post_id": post.id,
            "new_score": new_score,
            "is_trusted": post.is_trusted,
            "your_weight": weight,
        })

    def delete(self, request, id: int):
        post = get_object_or_404(Post, id=id)
        deleted, _ = PostValidation.objects.filter(post=post, validator=request.user).delete()
        if not deleted:
            return Response({"detail": "You have not validated this post."}, status=status.HTTP_404_NOT_FOUND)

        new_score = post.validations.aggregate(total=Sum("weight"))["total"] or Decimal("0")
        post.weighted_validation_score = new_score
        threshold = post.hub.peer_validation_threshold if post.hub else Decimal("3.0")
        if post.is_trusted and new_score < threshold:
            post.is_trusted = False
        post.save(update_fields=["weighted_validation_score", "is_trusted"])

        return Response({"post_id": post.id, "new_score": new_score, "is_trusted": post.is_trusted})


class PostValidationsListView(APIView):
    def get(self, request, id: int):
        post = get_object_or_404(Post, id=id)
        if not ensure_hub_access(request.user, post.hub):
            return Response({"detail": "You do not have access to this post."}, status=status.HTTP_403_FORBIDDEN)

        validations = post.validations.select_related("validator", "validator__profile").order_by("-created_at")
        data = []
        for v in validations:
            profile = getattr(v.validator, "profile", None)
            avatar_url = None
            if profile and profile.profile_picture:
                try:
                    avatar_url = request.build_absolute_uri(profile.profile_picture.url)
                except Exception:
                    pass
            data.append({
                "user_id": v.validator.id,
                "username": v.validator.username,
                "avatar_url": avatar_url,
                "weight": v.weight,
            })
        return Response({
            "total_score": post.weighted_validation_score,
            "is_trusted": post.is_trusted,
            "validators": data,
        })


class ChallengeListCreateView(generics.ListCreateAPIView):
    serializer_class = ChallengeSerializer
    pagination_class = DefaultPagination

    def get_queryset(self):
        hub_id = self.kwargs.get("id")
        return Challenge.objects.filter(hub_id=hub_id).order_by("-created_at")

    def list(self, request, *args, **kwargs):
        hub = get_object_or_404(Hub, id=self.kwargs["id"])
        if not ensure_hub_access(request.user, hub):
            return Response(
                {"detail": "You do not have access to this hub."},
                status=status.HTTP_403_FORBIDDEN,
            )
        return super().list(request, *args, **kwargs)

    def create(self, request, *args, **kwargs):
        hub = get_object_or_404(Hub, id=self.kwargs["id"])
        membership = HubMembership.objects.filter(user=request.user, hub=hub).first()
        if not membership:
            return Response(
                {"detail": "You must join the hub to create challenges."},
                status=status.HTTP_403_FORBIDDEN,
            )

        # Applying a template just supplies defaults — anything the client
        # explicitly sends (title, stages, config, ...) still wins. The
        # template itself is never modified or linked to the new challenge.
        template = None
        template_id = request.data.get("template_id")
        if template_id:
            template = get_object_or_404(ChallengeTemplate, id=template_id)

        title = str(request.data.get("title") or (template.name if template else "")).strip()
        if not title:
            return Response({"detail": "Title is required."}, status=status.HTTP_400_BAD_REQUEST)

        progress_model = request.data.get("progress_model") or (
            template.progress_model if template else Challenge.MODEL_COUNT
        )
        if progress_model not in dict(Challenge.MODEL_CHOICES):
            return Response({"detail": "Invalid progress_model."}, status=status.HTTP_400_BAD_REQUEST)

        description = request.data.get("description")
        if description is None:
            description = template.description if template else ""

        is_main_requested = bool(request.data.get("is_main", False))
        if is_main_requested and membership.role != HubMembership.ROLE_ADMIN:
            return Response(
                {"detail": "Only hub admins can set the main challenge."},
                status=status.HTTP_403_FORBIDDEN,
            )
        is_main = is_main_requested and membership.role == HubMembership.ROLE_ADMIN

        # Validate model-specific config BEFORE creating any rows, so a bad
        # request never leaves an orphan Challenge with no stages/config behind.
        stage_inputs = []
        count_kwargs = None
        streak_kwargs = None

        if progress_model == Challenge.MODEL_STAGE:
            stages = request.data.get("stages")
            if not stages and template and template.progress_model == Challenge.MODEL_STAGE:
                stages = [
                    {"title": s.title, "description": s.description, "proof_type": s.proof_type}
                    for s in template.stages.all()
                ]
            if not stages:
                return Response({"detail": "At least one stage is required."}, status=status.HTTP_400_BAD_REQUEST)
            for index, stage_data in enumerate(stages, start=1):
                stage_title = str(stage_data.get("title", "")).strip()
                if not stage_title:
                    return Response({"detail": f"Stage {index} requires a title."}, status=status.HTTP_400_BAD_REQUEST)
                stage_inputs.append({
                    "order_index": index,
                    "title": stage_title,
                    "description": str(stage_data.get("description", "")),
                    "proof_type": stage_data.get("proof_type", ChallengeStage.PROOF_ANY),
                    "proof_prompt": str(stage_data.get("proof_prompt", "")),
                    "is_milestone": bool(stage_data.get("is_milestone", False)),
                })
        elif progress_model == Challenge.MODEL_COUNT:
            count_data = request.data.get("count_config")
            if not count_data and template and template.progress_model == Challenge.MODEL_COUNT:
                count_data = {
                    "target_count": template.count_target,
                    "unit_label": template.count_unit_label,
                    "entry_increment": template.count_entry_increment,
                }
            count_data = count_data or {}
            try:
                target = Decimal(str(count_data.get("target_count")))
            except (InvalidOperation, TypeError):
                target = None
            if not target or target <= 0:
                return Response({"detail": "A positive target_count is required."}, status=status.HTTP_400_BAD_REQUEST)
            count_kwargs = {
                "target_count": target,
                "unit_label": str(count_data.get("unit_label", "")),
                "entry_increment": count_data.get("entry_increment", 1),
                "require_proof_per_entry": bool(count_data.get("require_proof_per_entry", False)),
                "is_cumulative": bool(count_data.get("is_cumulative", False)),
            }
        else:  # streak
            streak_data = request.data.get("streak_config")
            if not streak_data and template and template.progress_model == Challenge.MODEL_STREAK:
                streak_data = {
                    "target_days": template.streak_target_days,
                    "frequency": template.streak_frequency,
                    "grace_days": template.streak_grace_days,
                }
            streak_data = streak_data or {}
            try:
                target_days = int(streak_data.get("target_days"))
            except (TypeError, ValueError):
                target_days = None
            if not target_days or target_days <= 0:
                return Response({"detail": "A positive target_days is required."}, status=status.HTTP_400_BAD_REQUEST)
            streak_kwargs = {
                "target_days": target_days,
                "frequency": streak_data.get("frequency", ChallengeStreakConfig.FREQ_DAILY),
                "grace_days": streak_data.get("grace_days", 0),
                "require_proof": bool(streak_data.get("require_proof", False)),
            }

        ends_at_raw = request.data.get("ends_at")
        ends_at = parse_datetime(ends_at_raw) if ends_at_raw else None

        with transaction.atomic():
            challenge = Challenge.objects.create(
                hub=hub,
                title=title,
                description=str(description),
                progress_model=progress_model,
                is_main=is_main,
                created_by=request.user,
                ends_at=ends_at,
            )
            if stage_inputs:
                for stage_kwargs in stage_inputs:
                    ChallengeStage.objects.create(challenge=challenge, **stage_kwargs)
            elif count_kwargs is not None:
                ChallengeCountConfig.objects.create(challenge=challenge, **count_kwargs)
            elif streak_kwargs is not None:
                ChallengeStreakConfig.objects.create(challenge=challenge, **streak_kwargs)

            if template:
                ChallengeTemplate.objects.filter(id=template.id).update(use_count=F("use_count") + 1)

        return Response(
            ChallengeSerializer(challenge, context={"request": request}).data,
            status=status.HTTP_201_CREATED,
        )


class TemplateListView(APIView):
    """Read-only catalog of official challenge templates. No auth restriction
    beyond being logged in — templates aren't hub-scoped."""

    def get(self, request):
        qs = ChallengeTemplate.objects.filter(is_official=True).prefetch_related("stages")

        category = request.query_params.get("category")
        if category:
            qs = qs.filter(category=category)

        progress_model = request.query_params.get("progress_model")
        if progress_model:
            qs = qs.filter(progress_model=progress_model)

        search = request.query_params.get("search")
        if search:
            qs = qs.filter(Q(name__icontains=search) | Q(description__icontains=search))

        return Response(ChallengeTemplateSerializer(qs, many=True, context={"request": request}).data)


def ensure_challenge_admin(user, challenge) -> bool:
    if challenge.created_by_id == user.id:
        return True
    return HubMembership.objects.filter(
        user=user, hub=challenge.hub, role=HubMembership.ROLE_ADMIN
    ).exists()


class ChallengeDetailView(APIView):
    def get(self, request, id: int):
        challenge = get_object_or_404(Challenge, id=id)
        if not ensure_hub_access(request.user, challenge.hub):
            return Response({"detail": "You do not have access to this challenge."}, status=status.HTTP_403_FORBIDDEN)

        data = ChallengeSerializer(challenge, context={"request": request}).data
        data["can_manage"] = ensure_challenge_admin(request.user, challenge)

        if challenge.progress_model == Challenge.MODEL_STAGE:
            user_progress_by_stage = {
                p.stage_id: p
                for p in UserStageProgress.objects.filter(stage__challenge=challenge, user=request.user)
            }
            data["stages"] = [
                {
                    "id": stage.id,
                    "order_index": stage.order_index,
                    "title": stage.title,
                    "description": stage.description,
                    "proof_type": stage.proof_type,
                    "proof_prompt": stage.proof_prompt,
                    "is_milestone": stage.is_milestone,
                    "status": (
                        user_progress_by_stage[stage.id].status
                        if stage.id in user_progress_by_stage
                        else UserStageProgress.STATUS_NOT_STARTED
                    ),
                }
                for stage in challenge.stages.all()
            ]
        elif challenge.progress_model == Challenge.MODEL_COUNT:
            config = getattr(challenge, "count_config", None)
            progress = UserCountProgress.objects.filter(challenge=challenge, user=request.user).first()
            data["count_config"] = {
                "target_count": config.target_count if config else 0,
                "unit_label": config.unit_label if config else "",
                "entry_increment": config.entry_increment if config else 1,
                "require_proof_per_entry": config.require_proof_per_entry if config else False,
                "is_cumulative": config.is_cumulative if config else False,
            }
            data["my_progress"] = {
                "current_count": progress.current_count if progress else 0,
                "is_complete": progress.is_complete if progress else False,
            }
        else:  # streak
            config = getattr(challenge, "streak_config", None)
            progress = UserStreakProgress.objects.filter(challenge=challenge, user=request.user).first()
            data["streak_config"] = {
                "target_days": config.target_days if config else 0,
                "frequency": config.frequency if config else ChallengeStreakConfig.FREQ_DAILY,
                "grace_days": config.grace_days if config else 0,
                "require_proof": config.require_proof if config else False,
            }
            data["my_progress"] = {
                "current_streak": progress.current_streak if progress else 0,
                "longest_streak": progress.longest_streak if progress else 0,
                "total_checkins": progress.total_checkins if progress else 0,
                "checkin_calendar": progress.checkin_calendar if progress else [],
                "last_checkin_date": progress.last_checkin_date if progress else None,
                "is_complete": progress.is_complete if progress else False,
            }

        return Response(data)

    def delete(self, request, id: int):
        challenge = get_object_or_404(Challenge, id=id)
        if not ensure_challenge_admin(request.user, challenge):
            return Response(
                {"detail": "Only the challenge creator or a hub admin can delete it."},
                status=status.HTTP_403_FORBIDDEN,
            )
        challenge.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class ChallengeLeaderboardView(APIView):
    """Generalized leaderboard: ranks members by whatever the challenge's
    progress model considers 'score' (stages completed / count / check-ins)."""

    def get(self, request, id: int):
        challenge = get_object_or_404(Challenge, id=id)
        if not ensure_hub_access(request.user, challenge.hub):
            return Response({"detail": "You do not have access to this challenge."}, status=status.HTTP_403_FORBIDDEN)

        entries = []
        if challenge.progress_model == Challenge.MODEL_STAGE:
            rows = (
                UserStageProgress.objects.filter(
                    stage__challenge=challenge, status=UserStageProgress.STATUS_COMPLETED
                )
                .values("user_id")
                .annotate(score=Count("id"))
                .order_by("-score")[:20]
            )
            users_by_id = {
                u.id: u for u in User.objects.filter(id__in=[r["user_id"] for r in rows]).select_related("profile")
            }
            entries = [(users_by_id[r["user_id"]], r["score"]) for r in rows if r["user_id"] in users_by_id]
        elif challenge.progress_model == Challenge.MODEL_COUNT:
            qs = (
                UserCountProgress.objects.filter(challenge=challenge, current_count__gt=0)
                .select_related("user", "user__profile")
                .order_by("-current_count")[:20]
            )
            entries = [(p.user, p.current_count) for p in qs]
        else:  # streak
            qs = (
                UserStreakProgress.objects.filter(challenge=challenge, total_checkins__gt=0)
                .select_related("user", "user__profile")
                .order_by("-total_checkins")[:20]
            )
            entries = [(p.user, p.total_checkins) for p in qs]

        data = []
        for rank, (user, score) in enumerate(entries, start=1):
            profile = getattr(user, "profile", None)
            avatar_url = None
            if profile and profile.profile_picture:
                try:
                    avatar_url = request.build_absolute_uri(profile.profile_picture.url)
                except Exception:
                    pass
            data.append({
                "rank": rank,
                "user_id": user.id,
                "username": user.username,
                "avatar_url": avatar_url,
                "score": score,
            })
        return Response(data)


class CompletionRequestSubmitView(APIView):
    """Layer 3 — member submits a completion request once they've finished a challenge.
    Approval (not the completion itself) is what triggers the public announcement."""

    def post(self, request, id: int):
        challenge = get_object_or_404(Challenge, id=id)
        if not HubMembership.objects.filter(user=request.user, hub=challenge.hub).exists():
            return Response(
                {"detail": "You must join the hub to submit a completion request."},
                status=status.HTTP_403_FORBIDDEN,
            )

        if not has_completed_challenge(request.user, challenge):
            return Response({"detail": "You have not completed this challenge yet."}, status=status.HTTP_400_BAD_REQUEST)

        existing = (
            CompletionRequest.objects.filter(user=request.user, challenge=challenge)
            .order_by("-submitted_at")
            .first()
        )
        if existing and existing.status in (CompletionRequest.STATUS_PENDING, CompletionRequest.STATUS_APPROVED):
            return Response(
                {"detail": "You already have a pending or approved request for this challenge."},
                status=status.HTTP_409_CONFLICT,
            )

        member_note = str(request.data.get("member_note", "")).strip()
        completion_request = CompletionRequest.objects.create(
            user=request.user, challenge=challenge, member_note=member_note
        )

        admins = HubMembership.objects.filter(
            hub=challenge.hub, role=HubMembership.ROLE_ADMIN
        ).select_related("user")
        for membership in admins:
            if membership.user_id != request.user.id:
                Notification.objects.create(
                    recipient=membership.user,
                    sender=request.user,
                    notification_type=Notification.TYPE_COMPLETION_SUBMITTED,
                    challenge=challenge,
                )

        return Response(
            CompletionRequestSerializer(completion_request, context={"request": request}).data,
            status=status.HTTP_201_CREATED,
        )


class MyCompletionRequestView(APIView):
    """Lets a member check the status of their latest completion request for a challenge,
    so the app can show Pending/Approved/Rejected instead of the submit button."""

    def get(self, request, id: int):
        challenge = get_object_or_404(Challenge, id=id)
        latest = (
            CompletionRequest.objects.filter(user=request.user, challenge=challenge)
            .order_by("-submitted_at")
            .first()
        )
        if not latest:
            return Response(None)
        return Response(CompletionRequestSerializer(latest, context={"request": request}).data)


class HubCompletionRequestsListView(generics.ListAPIView):
    """Admin-only: completion requests across all of a hub's challenges.
    Defaults to pending (for the Hub Settings badge count); pass
    ?status=approved|rejected|all to see the review history too."""

    serializer_class = CompletionRequestSerializer

    def get_queryset(self):
        hub = get_object_or_404(Hub, id=self.kwargs["id"])
        qs = CompletionRequest.objects.filter(challenge__hub=hub).select_related(
            "user", "user__profile", "challenge", "reviewed_by"
        )
        status_filter = self.request.query_params.get("status", CompletionRequest.STATUS_PENDING)
        if status_filter != "all":
            qs = qs.filter(status=status_filter)
        return qs

    def list(self, request, *args, **kwargs):
        hub = get_object_or_404(Hub, id=self.kwargs["id"])
        is_admin = (
            hub.created_by_id == request.user.id
            or HubMembership.objects.filter(user=request.user, hub=hub, role=HubMembership.ROLE_ADMIN).exists()
        )
        if not is_admin:
            return Response({"detail": "Only hub admins can view completion requests."}, status=status.HTTP_403_FORBIDDEN)
        return super().list(request, *args, **kwargs)


class CompletionRequestReviewView(APIView):
    """Admin approves or rejects a pending completion request.
    Approval auto-creates a pinned-style announcement post in the hub feed."""

    def patch(self, request, id: int):
        completion_request = get_object_or_404(CompletionRequest, id=id)
        challenge = completion_request.challenge

        if not ensure_challenge_admin(request.user, challenge):
            return Response(
                {"detail": "Only the challenge creator or a hub admin can review this."},
                status=status.HTTP_403_FORBIDDEN,
            )
        if completion_request.status != CompletionRequest.STATUS_PENDING:
            return Response({"detail": "This request has already been reviewed."}, status=status.HTTP_400_BAD_REQUEST)

        action = request.data.get("action")
        admin_note = str(request.data.get("admin_note", "")).strip()

        if action == "approve":
            completion_request.status = CompletionRequest.STATUS_APPROVED
            completion_request.admin_note = admin_note
            completion_request.reviewed_at = timezone.now()
            completion_request.reviewed_by = request.user

            content = f"🎉 @{completion_request.user.username} completed \"{challenge.title}\"!"
            if admin_note:
                content += f"\n\n{admin_note}"
            announcement = Post.objects.create(
                author=completion_request.user,
                hub=challenge.hub,
                post_type=Post.TYPE_ANNOUNCEMENT,
                challenge=challenge,
                content=content,
            )
            completion_request.announcement_post = announcement
            completion_request.save()

            Notification.objects.create(
                recipient=completion_request.user,
                sender=request.user,
                notification_type=Notification.TYPE_COMPLETION_APPROVED,
                challenge=challenge,
            )
        elif action == "reject":
            if len(admin_note) < 20:
                return Response(
                    {"detail": "A rejection note of at least 20 characters is required."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            completion_request.status = CompletionRequest.STATUS_REJECTED
            completion_request.admin_note = admin_note
            completion_request.reviewed_at = timezone.now()
            completion_request.reviewed_by = request.user
            completion_request.save()

            Notification.objects.create(
                recipient=completion_request.user,
                sender=request.user,
                notification_type=Notification.TYPE_COMPLETION_REJECTED,
                challenge=challenge,
            )
        else:
            return Response({"detail": "action must be 'approve' or 'reject'."}, status=status.HTTP_400_BAD_REQUEST)

        return Response(CompletionRequestSerializer(completion_request, context={"request": request}).data)


class CommentListCreateView(generics.ListCreateAPIView):
    serializer_class = CommentSerializer
    pagination_class = DefaultPagination

    def get_queryset(self):
        post_id = self.kwargs["id"]
        return Comment.objects.filter(post_id=post_id).select_related("author", "author__profile")

    def list(self, request, *args, **kwargs):
        post = get_object_or_404(Post, id=self.kwargs["id"])
        if not ensure_hub_access(request.user, post.hub):
            return Response({"detail": "You do not have access to this post."}, status=status.HTTP_403_FORBIDDEN)
        return super().list(request, *args, **kwargs)

    def create(self, request, *args, **kwargs):
        post = get_object_or_404(Post, id=self.kwargs["id"])
        if not ensure_hub_access(request.user, post.hub):
            return Response({"detail": "You do not have access to this post."}, status=status.HTTP_403_FORBIDDEN)
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        comment = serializer.save(author=request.user, post=post)
        if post.author != request.user:
            from accounts.models import Notification
            Notification.objects.create(
                recipient=post.author,
                sender=request.user,
                notification_type=Notification.TYPE_COMMENT,
                post=post,
            )
        return Response(CommentSerializer(comment, context={"request": request}).data, status=status.HTTP_201_CREATED)


class CommentDeleteView(generics.DestroyAPIView):
    def get_queryset(self):
        return Comment.objects.filter(author=self.request.user)

    def get_object(self):
        return get_object_or_404(Comment, id=self.kwargs["id"], author=self.request.user)


class PostMediaUploadView(APIView):
    parser_classes = [MultiPartParser, FormParser]

    def post(self, request, id: int):
        post = get_object_or_404(Post, id=id)
        if post.author_id != request.user.id:
            return Response({"detail": "You can only upload media to your own posts."}, status=status.HTTP_403_FORBIDDEN)
        media_file = request.FILES.get("media_file")
        if not media_file:
            return Response({"detail": "media_file is required."}, status=status.HTTP_400_BAD_REQUEST)
        post.media_file = media_file
        post.save(update_fields=["media_file", "updated_at"])
        return Response(PostSerializer(post, context={"request": request}).data, status=status.HTTP_200_OK)


class HubCoverUploadView(APIView):
    parser_classes = [MultiPartParser, FormParser]

    def post(self, request, id: int):
        hub = get_object_or_404(Hub, id=id)
        membership = HubMembership.objects.filter(user=request.user, hub=hub).first()
        if not membership or membership.role != HubMembership.ROLE_ADMIN:
            return Response({"detail": "Only hub admins can update the cover image."}, status=status.HTTP_403_FORBIDDEN)
        cover_image = request.FILES.get("cover_image")
        if not cover_image:
            return Response({"detail": "cover_image is required."}, status=status.HTTP_400_BAD_REQUEST)
        hub.cover_image = cover_image
        hub.save(update_fields=["cover_image"])
        return Response(HubSerializer(hub, context={"request": request}).data, status=status.HTTP_200_OK)


class HubUpdateView(APIView):
    parser_classes = [MultiPartParser, FormParser]

    def patch(self, request, id: int):
        hub = get_object_or_404(Hub, id=id)
        if hub.created_by_id != request.user.id:
            return Response({"detail": "Only the hub creator can update settings."}, status=status.HTTP_403_FORBIDDEN)
        update_fields = []
        if "name" in request.data:
            hub.name = str(request.data["name"])[:150]
            update_fields.append("name")
        if "description" in request.data:
            hub.description = str(request.data["description"])
            update_fields.append("description")
        if "is_public" in request.data:
            hub.is_public = str(request.data["is_public"]).lower() in ("true", "1")
            update_fields.append("is_public")
        if "category" in request.data:
            try:
                from accounts.models import Interest
                hub.category = Interest.objects.get(id=int(request.data["category"]))
                update_fields.append("category")
            except (ValueError, TypeError, Interest.DoesNotExist):
                return Response({"detail": "Invalid category."}, status=status.HTTP_400_BAD_REQUEST)
        if "cover_image" in request.FILES:
            hub.cover_image = request.FILES["cover_image"]
            update_fields.append("cover_image")
        if update_fields:
            hub.save(update_fields=update_fields)
        return Response(HubSerializer(hub, context={"request": request}).data)


class HubDeleteView(APIView):
    def delete(self, request, id: int):
        hub = get_object_or_404(Hub, id=id)
        if hub.created_by_id != request.user.id:
            return Response({"detail": "Only the hub creator can delete it."}, status=status.HTTP_403_FORBIDDEN)
        hub.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class HubMembersView(APIView):
    def get(self, request, id: int):
        hub = get_object_or_404(Hub, id=id)
        if not ensure_hub_access(request.user, hub):
            return Response({"detail": "Access denied."}, status=status.HTTP_403_FORBIDDEN)
        memberships = hub.memberships.select_related("user", "user__profile").order_by("joined_at")
        data = []
        for m in memberships:
            profile = getattr(m.user, "profile", None)
            avatar_url = None
            if profile and profile.profile_picture:
                try:
                    avatar_url = request.build_absolute_uri(profile.profile_picture.url)
                except Exception:
                    pass
            data.append({
                "user_id": m.user.id,
                "username": m.user.username,
                "full_name": profile.full_name if profile else "",
                "avatar_url": avatar_url,
                "role": m.role,
                "is_creator": m.user_id == hub.created_by_id,
            })
        return Response(data)


class HubMemberRemoveView(APIView):
    def delete(self, request, id: int, user_id: int):
        hub = get_object_or_404(Hub, id=id)
        if hub.created_by_id != request.user.id:
            return Response({"detail": "Only the hub creator can remove members."}, status=status.HTTP_403_FORBIDDEN)
        if user_id == request.user.id:
            return Response({"detail": "Cannot remove yourself."}, status=status.HTTP_400_BAD_REQUEST)
        deleted, _ = HubMembership.objects.filter(hub=hub, user_id=user_id).delete()
        if deleted:
            Hub.objects.filter(id=id, members_count__gt=0).update(members_count=F("members_count") - 1)
        return Response({"removed": bool(deleted)})


class SearchView(APIView):
    def get(self, request):
        q = request.query_params.get("q", "").strip()
        if not q:
            return Response({"users": [], "hubs": [], "posts": [], "challenges": []})

        accessible_hubs = Q(is_public=True) | Q(memberships__user=request.user)

        users = (
            User.objects.filter(Q(username__icontains=q) | Q(profile__full_name__icontains=q))
            .select_related("profile")
            .distinct()[:10]
        )

        hubs = (
            Hub.objects.filter(accessible_hubs)
            .filter(Q(name__icontains=q) | Q(description__icontains=q))
            .distinct()
            .select_related("category", "created_by")[:10]
        )

        posts = (
            Post.objects.filter(content__icontains=q)
            .filter(Q(hub__is_public=True) | Q(hub__memberships__user=request.user) | Q(hub__isnull=True, is_public=True))
            .select_related("author", "author__profile", "hub")
            .distinct()[:10]
        )

        accessible_challenge_hubs = Q(hub__is_public=True) | Q(hub__memberships__user=request.user)
        challenges = (
            Challenge.objects.filter(Q(title__icontains=q) | Q(description__icontains=q))
            .filter(accessible_challenge_hubs)
            .select_related("hub")
            .distinct()[:10]
        )

        user_data = []
        for user in users:
            profile = getattr(user, "profile", None)
            pic_url = None
            if profile and profile.profile_picture:
                try:
                    pic_url = request.build_absolute_uri(profile.profile_picture.url)
                except Exception:
                    pass
            user_data.append({
                "id": user.id,
                "username": user.username,
                "full_name": profile.full_name if profile else user.username,
                "profile_picture_url": pic_url,
            })

        post_data = []
        for post in posts:
            profile = getattr(post.author, "profile", None)
            avatar_url = None
            if profile and profile.profile_picture:
                try:
                    avatar_url = request.build_absolute_uri(profile.profile_picture.url)
                except Exception:
                    pass
            media_url = None
            if post.media_file:
                try:
                    media_url = request.build_absolute_uri(post.media_file.url)
                except Exception:
                    pass
            post_data.append({
                "id": post.id,
                "content": post.content[:150],
                "author_id": post.author.id,
                "author_username": post.author.username,
                "author_avatar_url": avatar_url,
                "hub_name": post.hub.name if post.hub else None,
                "hub_id": post.hub.id if post.hub else None,
                "media_url": media_url,
                "likes_count": post.likes.count(),
            })

        challenge_data = [{
            "id": c.id,
            "title": c.title,
            "description": c.description[:100],
            "hub_name": c.hub.name if c.hub else None,
            "hub_id": c.hub.id if c.hub else None,
        } for c in challenges]

        hub_data = HubSerializer(hubs, many=True, context={"request": request}).data
        return Response({"users": user_data, "hubs": hub_data, "posts": post_data, "challenges": challenge_data})


class ExploreListView(generics.ListAPIView):
    """Public media-carrying posts for the Explore (Shorts) vertical feed, newest first."""
    serializer_class = PostSerializer
    pagination_class = DefaultPagination

    def get_queryset(self):
        return (
            Post.objects.filter(is_public=True, hub__is_public=True)
            .exclude(media_file="")
            .select_related("author", "author__profile", "hub")
            .prefetch_related("likes")
            .order_by("-created_at")
        )
