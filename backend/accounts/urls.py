from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView

from .views import (
    CheckEmailAvailabilityView,
    CheckUsernameAvailabilityView,
    InterestListView,
    LoginView,
    LogoutView,
    SignupView,
    SocialLoginView,
)

urlpatterns = [
    path("signup/", SignupView.as_view(), name="signup"),
    path("login/", LoginView.as_view(), name="login"),
    path("logout/", LogoutView.as_view(), name="logout"),
    path("social-login/", SocialLoginView.as_view(), name="social-login"),
    path("check-email/", CheckEmailAvailabilityView.as_view(), name="check-email"),
    path("check-username/", CheckUsernameAvailabilityView.as_view(), name="check-username"),
    path("token/refresh/", TokenRefreshView.as_view(), name="token-refresh"),
    path("interests/", InterestListView.as_view(), name="interests"),
]
