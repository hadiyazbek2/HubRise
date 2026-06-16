from django.contrib.auth import get_user_model
from django.core.files.uploadedfile import SimpleUploadedFile
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Interest, SocialAccount

User = get_user_model()


class AuthApiTests(APITestCase):
    def setUp(self):
        self.interest_a = Interest.objects.create(name="Music")
        self.interest_b = Interest.objects.create(name="Tech")

    def test_signup_returns_expected_contract(self):
        response = self.client.post(
            reverse("signup"),
            {
                "email": "john@example.com",
                "password": "StrongPass123!",
                "full_name": "John Doe",
                "username": "john",
                "date_of_birth": "1999-06-01",
                "phone_number": "+12345",
                "interests": [self.interest_a.id, self.interest_b.id],
            },
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertIn("access_token", response.data)
        self.assertIn("refresh_token", response.data)
        self.assertEqual(response.data["user"]["email"], "john@example.com")
        self.assertEqual(response.data["user"]["interests"], [self.interest_a.id, self.interest_b.id])

    def test_signup_conflict_on_existing_email(self):
        User.objects.create_user(username="existing", email="dup@example.com", password="StrongPass123!")
        response = self.client.post(
            reverse("signup"),
            {
                "email": "dup@example.com",
                "password": "StrongPass123!",
                "full_name": "Dup User",
                "username": "newname",
                "date_of_birth": "2000-01-01",
                "interests": [],
            },
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_409_CONFLICT)

    def test_login_with_email(self):
        User.objects.create_user(
            username="sara",
            email="sara@example.com",
            password="StrongPass123!",
        )
        response = self.client.post(
            reverse("login"),
            {"email": "sara@example.com", "password": "StrongPass123!"},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("access_token", response.data)
        self.assertEqual(response.data["user"]["username"], "sara")

    def test_login_invalid_credentials_returns_401(self):
        User.objects.create_user(username="bad", email="bad@example.com", password="StrongPass123!")
        response = self.client.post(
            reverse("login"),
            {"email": "bad@example.com", "password": "wrong-pass"},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_social_login_with_id_token_contract(self):
        response = self.client.post(
            reverse("social-login"),
            {"provider": "google", "id_token": "not-a-real-token-for-prototype"},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("access_token", response.data)
        self.assertIn("refresh_token", response.data)
        self.assertTrue(SocialAccount.objects.filter(provider="google").exists())

    def test_check_email_and_username_availability(self):
        User.objects.create_user(username="taken", email="taken@example.com", password="StrongPass123!")
        email_response = self.client.get(reverse("check-email"), {"email": "taken@example.com"})
        username_response = self.client.get(reverse("check-username"), {"username": "taken"})
        self.assertEqual(email_response.status_code, status.HTTP_200_OK)
        self.assertFalse(email_response.data["available"])
        self.assertEqual(username_response.status_code, status.HTTP_200_OK)
        self.assertFalse(username_response.data["available"])

    def test_upload_profile_picture(self):
        user = User.objects.create_user(username="picuser", email="pic@example.com", password="StrongPass123!")
        login = self.client.post(
            reverse("login"),
            {"email": "pic@example.com", "password": "StrongPass123!"},
            format="json",
        )
        token = login.data["access_token"]
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")

        upload = SimpleUploadedFile("profile.jpg", b"fake-image-data", content_type="image/jpeg")
        response = self.client.post(
            reverse("profile-picture-upload-global", kwargs={"user_id": user.id}),
            {"image_file": upload},
            format="multipart",
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("profile_picture_url", response.data)
