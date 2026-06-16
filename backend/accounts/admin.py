from django.contrib import admin

from .models import Interest, SocialAccount, UserProfile


@admin.register(SocialAccount)
class SocialAccountAdmin(admin.ModelAdmin):
    list_display = ("provider", "provider_user_id", "user", "email", "created_at")
    list_filter = ("provider",)
    search_fields = ("provider_user_id", "email", "user__username", "user__email")


@admin.register(Interest)
class InterestAdmin(admin.ModelAdmin):
    list_display = ("id", "name")
    search_fields = ("name",)


@admin.register(UserProfile)
class UserProfileAdmin(admin.ModelAdmin):
    list_display = ("user", "full_name", "date_of_birth", "phone_number", "updated_at")
    search_fields = ("user__email", "user__username", "full_name", "phone_number")
    filter_horizontal = ("interests",)
