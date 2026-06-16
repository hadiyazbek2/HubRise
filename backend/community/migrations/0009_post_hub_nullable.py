from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ("community", "0008_comment_model"),
    ]

    operations = [
        migrations.AlterField(
            model_name="post",
            name="hub",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.SET_NULL,
                related_name="posts",
                to="community.hub",
            ),
        ),
    ]
