from __future__ import annotations

import hashlib
from typing import Any

import jwt
from django.contrib.auth import authenticate, get_user_model
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError as DjangoValidationError
from rest_framework import serializers
from rest_framework.exceptions import APIException, AuthenticationFailed

from .models import Interest, SocialAccount, UserProfile

User = get_user_model()


class ConflictError(APIException):
    status_code = 409
    default_code = "conflict"
    default_detail = "Request conflicts with existing data."


def profile_picture_url(profile: UserProfile, request) -> str | None:
    if not profile.profile_picture:
        return None
    url = profile.profile_picture.url
    return request.build_absolute_uri(url) if request else url


def create_personal_hub(user) -> None:
    """Auto-create a private Personal hub for every new user."""
    from community.models import Hub, HubMembership
    hub = Hub.objects.create(
        name="Personal",
        description="Your personal space for private posts.",
        category=None,
        is_public=False,
        created_by=user,
        members_count=1,
    )
    HubMembership.objects.create(user=user, hub=hub, role=HubMembership.ROLE_ADMIN)


def generate_unique_username(base: str) -> str:
    candidate = "".join(ch for ch in base if ch.isalnum() or ch in ("_", ".")).strip("_.")
    if not candidate:
        candidate = "user"
    candidate = candidate[:150]

    if not User.objects.filter(username=candidate).exists():
        return candidate

    suffix = 1
    while True:
        next_candidate = f"{candidate[:145]}_{suffix}"
        if not User.objects.filter(username=next_candidate).exists():
            return next_candidate
        suffix += 1


class UserSerializer(serializers.ModelSerializer):
    full_name = serializers.SerializerMethodField()
    date_of_birth = serializers.SerializerMethodField()
    phone_number = serializers.SerializerMethodField()
    profile_picture_url = serializers.SerializerMethodField()
    interests = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = (
            "id",
            "email",
            "username",
            "full_name",
            "date_of_birth",
            "phone_number",
            "profile_picture_url",
            "interests",
        )

    def _get_profile(self, obj: User) -> UserProfile:
        profile, _ = UserProfile.objects.get_or_create(
            user=obj,
            defaults={"full_name": obj.username, "date_of_birth": "2000-01-01"},
        )
        return profile

    def get_full_name(self, obj: User) -> str:
        return self._get_profile(obj).full_name

    def get_date_of_birth(self, obj: User) -> str:
        return self._get_profile(obj).date_of_birth.isoformat()

    def get_phone_number(self, obj: User) -> str:
        return self._get_profile(obj).phone_number

    def get_profile_picture_url(self, obj: User) -> str | None:
        request = self.context.get("request")
        return profile_picture_url(self._get_profile(obj), request)

    def get_interests(self, obj: User) -> list[int]:
        return list(self._get_profile(obj).interests.values_list("id", flat=True))


class SignupSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True, min_length=8)
    full_name = serializers.CharField(max_length=255)
    username = serializers.CharField(max_length=150)
    date_of_birth = serializers.DateField()
    phone_number = serializers.CharField(required=False, allow_blank=True, max_length=30)
    interests = serializers.ListField(child=serializers.IntegerField(min_value=1), allow_empty=True)

    def validate_email(self, value: str) -> str:
        email = value.lower().strip()
        if User.objects.filter(email__iexact=email).exists():
            raise ConflictError({"email": ["A user with this email already exists."]})
        return email

    def validate_username(self, value: str) -> str:
        username = value.strip()
        if not username:
            raise serializers.ValidationError("Username cannot be empty.")
        if User.objects.filter(username__iexact=username).exists():
            raise ConflictError({"username": ["A user with this username already exists."]})
        return username

    def validate_password(self, value: str) -> str:
        try:
            validate_password(value)
        except DjangoValidationError as exc:
            raise serializers.ValidationError(list(exc.messages)) from exc
        return value

    def validate_interests(self, value: list[int]) -> list[int]:
        if not value:
            return value
        existing = set(Interest.objects.filter(id__in=value).values_list("id", flat=True))
        missing = [interest_id for interest_id in value if interest_id not in existing]
        if missing:
            raise serializers.ValidationError(f"Invalid interest IDs: {missing}")
        return value

    def create(self, validated_data: dict[str, Any]) -> User:
        interest_ids = validated_data.pop("interests", [])
        full_name = validated_data.pop("full_name")
        date_of_birth = validated_data.pop("date_of_birth")
        phone_number = validated_data.pop("phone_number", "")
        user = User.objects.create_user(**validated_data)
        profile = UserProfile.objects.create(
            user=user,
            full_name=full_name,
            date_of_birth=date_of_birth,
            phone_number=phone_number,
        )
        if interest_ids:
            profile.interests.set(Interest.objects.filter(id__in=interest_ids))
        create_personal_hub(user)
        return user


class LoginSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True)

    def validate(self, attrs: dict[str, Any]) -> dict[str, Any]:
        email = attrs["email"].strip().lower()
        password = attrs["password"]
        user = User.objects.filter(email__iexact=email).first()
        if not user:
            raise AuthenticationFailed("Invalid email or password.")

        authenticated = authenticate(
            request=self.context.get("request"),
            username=user.username,
            password=password,
        )
        if not authenticated:
            raise AuthenticationFailed("Invalid email or password.")
        if not authenticated.is_active:
            raise AuthenticationFailed("User account is disabled.")

        attrs["user"] = authenticated
        return attrs


class SocialLoginSerializer(serializers.Serializer):
    provider = serializers.ChoiceField(choices=[choice[0] for choice in SocialAccount.PROVIDER_CHOICES])
    id_token = serializers.CharField()

    def _decode_token_without_verification(self, id_token: str) -> dict[str, Any]:
        try:
            return jwt.decode(
                id_token,
                options={"verify_signature": False, "verify_aud": False, "verify_exp": False},
                algorithms=["HS256", "RS256", "ES256"],
            )
        except Exception:
            return {}

    def create(self, validated_data: dict[str, Any]) -> dict[str, Any]:
        provider = validated_data["provider"]
        id_token = validated_data["id_token"].strip()
        token_payload = self._decode_token_without_verification(id_token)

        provider_user_id = str(
            token_payload.get("sub")
            or token_payload.get("user_id")
            or hashlib.sha256(id_token.encode("utf-8")).hexdigest()[:40]
        )
        email = str(token_payload.get("email", "")).lower().strip()
        full_name = str(token_payload.get("name", "")).strip() or "Social User"

        social_account = SocialAccount.objects.filter(
            provider=provider,
            provider_user_id=provider_user_id,
        ).select_related("user").first()

        if social_account:
            return {"user": social_account.user}

        user = User.objects.filter(email__iexact=email).first() if email else None
        if not user:
            username_base = email.split("@")[0] if email else f"{provider}_user"
            user = User.objects.create_user(
                username=generate_unique_username(username_base),
                email=email,
            )
            user.set_unusable_password()
            user.save(update_fields=["password"])

            UserProfile.objects.create(
                user=user,
                full_name=full_name,
                date_of_birth="2000-01-01",
            )
            create_personal_hub(user)

        SocialAccount.objects.create(
            user=user,
            provider=provider,
            provider_user_id=provider_user_id,
            email=email,
            extra_data={"token_claims": token_payload},
        )
        return {"user": user}


class AvailabilityEmailSerializer(serializers.Serializer):
    email = serializers.EmailField()


class AvailabilityUsernameSerializer(serializers.Serializer):
    username = serializers.CharField(max_length=150)


class ProfilePictureUploadSerializer(serializers.Serializer):
    image_file = serializers.FileField()
