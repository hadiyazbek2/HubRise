from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("accounts", "0002_interest_alter_socialaccount_provider_userprofile"),
    ]

    operations = [
        migrations.AddField(
            model_name="userprofile",
            name="bio",
            field=models.CharField(blank=True, default="", max_length=200),
        ),
    ]
