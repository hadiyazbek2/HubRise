import secrets
import string

from django.db import migrations, models


def _gen_code():
    alphabet = string.ascii_uppercase + string.digits
    return "".join(secrets.choice(alphabet) for _ in range(8))


def populate_invite_codes(apps, schema_editor):
    Hub = apps.get_model("community", "Hub")
    used = set()
    for hub in Hub.objects.all():
        code = _gen_code()
        while code in used:
            code = _gen_code()
        hub.invite_code = code
        hub.save(update_fields=["invite_code"])
        used.add(code)


class Migration(migrations.Migration):

    dependencies = [
        ("community", "0015_challengecountconfig_is_cumulative"),
    ]

    operations = [
        migrations.AddField(
            model_name="hub",
            name="invite_code",
            field=models.CharField(blank=True, default="", max_length=8),
            preserve_default=False,
        ),
        migrations.RunPython(populate_invite_codes, migrations.RunPython.noop),
        migrations.AlterField(
            model_name="hub",
            name="invite_code",
            field=models.CharField(max_length=8, unique=True, blank=True),
        ),
    ]
