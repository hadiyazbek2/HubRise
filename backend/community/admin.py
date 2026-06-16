from django.contrib import admin

from .models import Hub, HubMembership, Post, PostLike


@admin.register(Hub)
class HubAdmin(admin.ModelAdmin):
    list_display = ("id", "name", "category", "members_count", "is_public", "created_by", "created_at")
    list_filter = ("is_public", "category")
    search_fields = ("name", "description", "created_by__username")


@admin.register(Post)
class PostAdmin(admin.ModelAdmin):
    list_display = ("id", "hub", "author", "created_at", "updated_at")
    search_fields = ("content", "author__username", "hub__name")
    list_filter = ("hub",)


@admin.register(PostLike)
class PostLikeAdmin(admin.ModelAdmin):
    list_display = ("id", "post", "user", "created_at")
    list_filter = ("created_at",)


@admin.register(HubMembership)
class HubMembershipAdmin(admin.ModelAdmin):
    list_display = ("id", "hub", "user", "joined_at")
    list_filter = ("joined_at",)
