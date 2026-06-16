from django.contrib.auth import get_user_model
from django.db.models import Q
from django.shortcuts import get_object_or_404
from rest_framework import generics, status
from rest_framework.exceptions import PermissionDenied
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken

from community.models import HubMembership, Post
from community.serializers import PostSerializer
from community.views import DefaultPagination
from .models import Notification, UserFollow, UserProfile
from .serializers import (
    AvailabilityEmailSerializer,
    AvailabilityUsernameSerializer,
    LoginSerializer,
    ProfilePictureUploadSerializer,
    SignupSerializer,
    SocialLoginSerializer,
    UserSerializer,
    profile_picture_url,
)

User = get_user_model()


def build_auth_response(user, request):
    refresh = RefreshToken.for_user(user)
    return {
        "access_token": str(refresh.access_token),
        "refresh_token": str(refresh),
        "user": UserSerializer(user, context={"request": request}).data,
    }


class SignupView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = SignupSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()
        return Response(build_auth_response(user, request), status=status.HTTP_201_CREATED)


class LoginView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = LoginSerializer(data=request.data, context={"request": request})
        serializer.is_valid(raise_exception=True)
        user = serializer.validated_data["user"]
        return Response(build_auth_response(user, request), status=status.HTTP_200_OK)


class SocialLoginView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = SocialLoginSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        result = serializer.save()
        user = result["user"]
        return Response(build_auth_response(user, request), status=status.HTTP_200_OK)


class CheckEmailAvailabilityView(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        serializer = AvailabilityEmailSerializer(data=request.query_params)
        serializer.is_valid(raise_exception=True)
        email = serializer.validated_data["email"].strip().lower()
        available = not User.objects.filter(email__iexact=email).exists()
        return Response({"available": available}, status=status.HTTP_200_OK)


class CheckUsernameAvailabilityView(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        serializer = AvailabilityUsernameSerializer(data=request.query_params)
        serializer.is_valid(raise_exception=True)
        username = serializer.validated_data["username"].strip()
        available = bool(username) and not User.objects.filter(username__iexact=username).exists()
        return Response({"available": available}, status=status.HTTP_200_OK)


class ProfilePictureUploadView(APIView):
    parser_classes = [MultiPartParser, FormParser]

    def post(self, request, user_id: int):
        if request.user.id != user_id:
            raise PermissionDenied("You can only update your own profile picture.")

        serializer = ProfilePictureUploadSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        profile, _ = UserProfile.objects.get_or_create(
            user=request.user,
            defaults={"full_name": request.user.username, "date_of_birth": "2000-01-01"},
        )
        profile.profile_picture = serializer.validated_data["image_file"]
        profile.save(update_fields=["profile_picture", "updated_at"])

        return Response(
            {"profile_picture_url": profile_picture_url(profile, request)},
            status=status.HTTP_200_OK,
        )


class UserPublicProfileView(APIView):
    def get(self, request, user_id: int):
        user = get_object_or_404(User, id=user_id)
        profile = getattr(user, "profile", None)
        if request.user == user:
            post_count = Post.objects.filter(author=user).count()
        else:
            post_count = Post.objects.filter(
                Q(author=user) & (
                    Q(hub__is_public=True) |
                    Q(hub__memberships__user=request.user) |
                    Q(hub__isnull=True, is_public=True)
                )
            ).distinct().count()
        hubs_count = HubMembership.objects.filter(user=user).count()
        followers_count = UserFollow.objects.filter(following=user).count()
        following_count = UserFollow.objects.filter(follower=user).count()
        is_following = (
            request.user != user
            and UserFollow.objects.filter(follower=request.user, following=user).exists()
        )
        return Response({
            "id": user.id,
            "username": user.username,
            "full_name": profile.full_name if profile else user.username,
            "bio": profile.bio if profile else "",
            "profile_picture_url": profile_picture_url(profile, request) if profile else None,
            "post_count": post_count,
            "hubs_count": hubs_count,
            "followers_count": followers_count,
            "following_count": following_count,
            "is_following": is_following,
        })

    def patch(self, request, user_id: int):
        if request.user.id != user_id:
            return Response({"detail": "You can only update your own profile."}, status=status.HTTP_403_FORBIDDEN)
        profile, _ = UserProfile.objects.get_or_create(
            user=request.user,
            defaults={"full_name": request.user.username, "date_of_birth": "2000-01-01"},
        )
        update_fields = []
        full_name = request.data.get("full_name")
        bio = request.data.get("bio")
        if full_name is not None:
            profile.full_name = str(full_name)[:255]
            update_fields.append("full_name")
        if bio is not None:
            profile.bio = str(bio)[:200]
            update_fields.append("bio")
        if update_fields:
            profile.save(update_fields=update_fields)
        return Response({"full_name": profile.full_name, "bio": profile.bio})


class FollowToggleView(APIView):
    def post(self, request, user_id: int):
        if request.user.id == user_id:
            return Response({"detail": "You cannot follow yourself."}, status=status.HTTP_400_BAD_REQUEST)
        target = get_object_or_404(User, id=user_id)
        follow, created = UserFollow.objects.get_or_create(follower=request.user, following=target)
        if not created:
            follow.delete()
        else:
            Notification.objects.create(
                recipient=target,
                sender=request.user,
                notification_type=Notification.TYPE_FOLLOW,
            )
        is_following = created
        followers_count = UserFollow.objects.filter(following=target).count()
        return Response(
            {"is_following": is_following, "followers_count": followers_count},
            status=status.HTTP_200_OK,
        )


class UserPostsView(generics.ListAPIView):
    serializer_class = PostSerializer
    pagination_class = DefaultPagination

    def get_queryset(self):
        user = get_object_or_404(User, id=self.kwargs["user_id"])
        if self.request.user == user:
            qs = Post.objects.filter(author=user)
        else:
            qs = Post.objects.filter(
                Q(author=user) & (
                    Q(hub__is_public=True) |
                    Q(hub__memberships__user=self.request.user) |
                    Q(hub__isnull=True, is_public=True)
                )
            ).distinct()
        return qs.select_related("author", "hub").prefetch_related("likes").order_by("-created_at")


class LogoutView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        try:
            refresh_token = request.data.get("refresh_token")
            if not refresh_token:
                return Response(
                    {"error": "refresh_token is required"},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            token = RefreshToken(refresh_token)
            token.blacklist()
            return Response(
                {"message": "Successfully logged out"},
                status=status.HTTP_200_OK,
            )
        except Exception as e:
            return Response(
                {"error": str(e)},
                status=status.HTTP_400_BAD_REQUEST,
            )


class NotificationListView(APIView):
    def get(self, request):
        notifications = (
            Notification.objects.filter(recipient=request.user)
            .select_related("sender", "sender__profile")
            .prefetch_related()[:30]
        )
        data = []
        for n in notifications:
            profile = getattr(n.sender, "profile", None)
            pic_url = None
            if profile and profile.profile_picture:
                try:
                    pic_url = request.build_absolute_uri(profile.profile_picture.url)
                except Exception:
                    pass
            if n.notification_type == Notification.TYPE_FOLLOW:
                message = f"@{n.sender.username} started following you"
            elif n.notification_type == Notification.TYPE_LIKE:
                message = f"@{n.sender.username} liked your post"
            else:
                message = f"@{n.sender.username} commented on your post"
            data.append({
                "id": n.id,
                "type": n.notification_type,
                "sender_id": n.sender.id,
                "sender_username": n.sender.username,
                "sender_avatar": pic_url,
                "post_id": n.post_id,
                "is_read": n.is_read,
                "created_at": n.created_at.isoformat(),
                "message": message,
            })
        Notification.objects.filter(recipient=request.user, is_read=False).update(is_read=True)
        return Response(data)


class NotificationUnreadCountView(APIView):
    def get(self, request):
        count = Notification.objects.filter(recipient=request.user, is_read=False).count()
        return Response({"count": count})
