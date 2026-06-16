from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from accounts.models import Interest, UserProfile

from .models import Hub, HubMembership, Post

User = get_user_model()


class CommunityApiTests(APITestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username="member",
            email="member@example.com",
            password="StrongPass123!",
        )
        self.other_user = User.objects.create_user(
            username="other",
            email="other@example.com",
            password="StrongPass123!",
        )
        self.interest_music = Interest.objects.create(name="Music")
        self.interest_tech = Interest.objects.create(name="Tech")
        self.profile = UserProfile.objects.create(
            user=self.user,
            full_name="Member User",
            date_of_birth="1995-05-05",
        )
        self.profile.interests.add(self.interest_music)

        self.public_hub = Hub.objects.create(
            name="Public Hub",
            description="public",
            category=self.interest_music,
            is_public=True,
            created_by=self.other_user,
        )
        self.private_hub = Hub.objects.create(
            name="Private Hub",
            description="private",
            category=self.interest_tech,
            is_public=False,
            created_by=self.other_user,
        )
        HubMembership.objects.create(user=self.user, hub=self.private_hub)
        self.private_hub.members_count = 1
        self.private_hub.save(update_fields=["members_count"])

        self.post_private = Post.objects.create(
            author=self.other_user,
            hub=self.private_hub,
            content="Private feed post",
        )
        self.post_public = Post.objects.create(
            author=self.other_user,
            hub=self.public_hub,
            content="Global post",
        )

    def authenticate(self):
        self.client.force_authenticate(user=self.user)

    def test_auth_required(self):
        response = self.client.get(reverse("posts-feed"))
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_feed_posts(self):
        self.authenticate()
        response = self.client.get(reverse("posts-feed"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["results"][0]["id"], self.post_private.id)

    def test_global_posts_only_public(self):
        self.authenticate()
        response = self.client.get(reverse("posts-global"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = [row["id"] for row in response.data["results"]]
        self.assertIn(self.post_public.id, ids)
        self.assertNotIn(self.post_private.id, ids)

    def test_hubs_filter_by_interest(self):
        self.authenticate()
        response = self.client.get(reverse("hubs-list"), {"interest": self.interest_music.id})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = [row["id"] for row in response.data["results"]]
        self.assertEqual(ids, [self.public_hub.id])
        self.assertFalse(response.data["results"][0]["is_member"])

    def test_create_hub_and_detail(self):
        self.authenticate()
        create_response = self.client.post(
            reverse("hubs-list"),
            {
                "name": "My New Hub",
                "description": "desc",
                "category": self.interest_music.id,
                "cover_image_url": "https://example.com/cover.jpg",
                "is_public": False,
            },
            format="json",
        )
        self.assertEqual(create_response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(create_response.data["members_count"], 1)
        self.assertTrue(create_response.data["is_member"])

        hub_id = create_response.data["id"]
        self.assertTrue(HubMembership.objects.filter(user=self.user, hub_id=hub_id).exists())

        detail_response = self.client.get(reverse("hubs-detail", kwargs={"id": hub_id}))
        self.assertEqual(detail_response.status_code, status.HTTP_200_OK)
        self.assertEqual(detail_response.data["id"], hub_id)
        self.assertTrue(detail_response.data["is_member"])

    def test_recommended_hubs(self):
        self.authenticate()
        response = self.client.get(reverse("hubs-recommended"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = [row["id"] for row in response.data["results"]]
        self.assertIn(self.public_hub.id, ids)
        self.assertNotIn(self.private_hub.id, ids)

    def test_join_and_leave_hub(self):
        self.authenticate()
        join_response = self.client.post(reverse("hubs-join", kwargs={"id": self.public_hub.id}))
        self.assertEqual(join_response.status_code, status.HTTP_200_OK)
        self.public_hub.refresh_from_db()
        self.assertEqual(self.public_hub.members_count, 1)

        leave_response = self.client.post(reverse("hubs-leave", kwargs={"id": self.public_hub.id}))
        self.assertEqual(leave_response.status_code, status.HTTP_200_OK)
        self.public_hub.refresh_from_db()
        self.assertEqual(self.public_hub.members_count, 0)

    def test_hub_posts_forbidden_when_private_without_membership(self):
        self.authenticate()
        another_private = Hub.objects.create(
            name="Another Private",
            description="p",
            category=self.interest_music,
            is_public=False,
            created_by=self.other_user,
        )
        Post.objects.create(author=self.other_user, hub=another_private, content="No access")
        response = self.client.get(reverse("hubs-posts", kwargs={"id": another_private.id}))
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_hub_detail_forbidden_when_private_without_membership(self):
        self.authenticate()
        another_private = Hub.objects.create(
            name="Private Detail",
            description="private",
            category=self.interest_tech,
            is_public=False,
            created_by=self.other_user,
        )
        response = self.client.get(reverse("hubs-detail", kwargs={"id": another_private.id}))
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_create_post_requires_membership(self):
        self.authenticate()
        response = self.client.post(
            reverse("posts-create"),
            {"hub": self.public_hub.id, "content": "Should fail", "media_url": ""},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

        self.client.post(reverse("hubs-join", kwargs={"id": self.public_hub.id}))
        response = self.client.post(
            reverse("posts-create"),
            {"hub": self.public_hub.id, "content": "Should pass", "media_url": ""},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    def test_toggle_like(self):
        self.authenticate()
        response = self.client.post(reverse("posts-like-toggle", kwargs={"id": self.post_private.id}))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data["liked"])
        response = self.client.post(reverse("posts-like-toggle", kwargs={"id": self.post_private.id}))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data["liked"])
