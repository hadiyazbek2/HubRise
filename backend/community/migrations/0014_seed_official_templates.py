from django.db import migrations


STAGE = "stage"
COUNT = "count"
STREAK = "streak"

OFFICIAL_TEMPLATES = [
    {
        "name": "Learn Python from Zero",
        "category": "learning",
        "progress_model": STAGE,
        "description": "Go from zero to building real projects in Python.",
        "stages": ["Setup", "Syntax", "Functions", "OOP", "Files", "APIs", "Projects", "Deploy"],
    },
    {
        "name": "Read 12 Books in a Year",
        "category": "reading",
        "progress_model": COUNT,
        "description": "One book a month, every month.",
        "count_target": 12, "count_unit_label": "books",
    },
    {
        "name": "Meditate Every Day (30 Days)",
        "category": "mindfulness",
        "progress_model": STREAK,
        "description": "Build a daily meditation habit over 30 days.",
        "streak_target_days": 30, "streak_grace_days": 1,
    },
    {
        "name": "Lose 10% Body Fat",
        "category": "fitness",
        "progress_model": STAGE,
        "description": "A measured, milestone-based fat loss journey.",
        "stages": ["Baseline", "Month 1", "Month 2", "Month 3", "Final Measurement", "Celebration"],
    },
    {
        "name": "Earn Your First $1,000",
        "category": "finance",
        "progress_model": COUNT,
        "description": "Track every dollar toward your first $1,000 earned.",
        "count_target": 1000, "count_unit_label": "USD",
    },
    {
        "name": "Run a Half Marathon",
        "category": "fitness",
        "progress_model": STAGE,
        "description": "Train up from your first run to race day.",
        "stages": ["Week 1", "Week 3", "Week 5", "Week 7", "10km run", "15km run", "Race Day"],
    },
    {
        "name": "Build a 30-Day Coding Habit",
        "category": "learning",
        "progress_model": STREAK,
        "description": "Write code every day for 30 days straight.",
        "streak_target_days": 30, "streak_grace_days": 0,
    },
    {
        "name": "Start a Business (MVP)",
        "category": "career",
        "progress_model": STAGE,
        "description": "From idea to your first paying customer.",
        "stages": ["Idea", "Research", "MVP Build", "First User", "First Revenue", "Pitch Deck"],
    },
    {
        "name": "Write a Novel (NaNoWriMo)",
        "category": "creative",
        "progress_model": COUNT,
        "description": "50,000 words, one draft.",
        "count_target": 50000, "count_unit_label": "words",
    },
    {
        "name": "Learn a New Language (A2)",
        "category": "learning",
        "progress_model": STAGE,
        "description": "Reach conversational A2 level in a new language.",
        "stages": ["100 words", "Grammar basics", "Short conversation", "Reading", "Speaking test"],
    },
    {
        "name": "Publish 30 Social Media Posts",
        "category": "creative",
        "progress_model": COUNT,
        "description": "Build a consistent posting habit.",
        "count_target": 30, "count_unit_label": "posts",
    },
    {
        "name": "Digital Detox (7 Days)",
        "category": "mindfulness",
        "progress_model": STREAK,
        "description": "Seven days of intentional disconnection.",
        "streak_target_days": 7, "streak_grace_days": 0,
    },
    {
        "name": "Save $500 Emergency Fund",
        "category": "finance",
        "progress_model": COUNT,
        "description": "Build your first safety net.",
        "count_target": 500, "count_unit_label": "USD",
    },
    {
        "name": "Read the Entire Quran",
        "category": "mindfulness",
        "progress_model": STAGE,
        "description": "One Juz at a time, thirty stages to completion.",
        "stages": [f"Juz {i}" for i in range(1, 31)],
    },
    {
        "name": "Complete a 75-Day Hard",
        "category": "fitness",
        "progress_model": STREAK,
        "description": "75 days, no excuses, no missed days.",
        "streak_target_days": 75, "streak_grace_days": 0,
    },
]


def seed_templates(apps, schema_editor):
    ChallengeTemplate = apps.get_model("community", "ChallengeTemplate")
    TemplateStage = apps.get_model("community", "TemplateStage")

    for entry in OFFICIAL_TEMPLATES:
        template = ChallengeTemplate.objects.create(
            name=entry["name"],
            category=entry["category"],
            progress_model=entry["progress_model"],
            description=entry.get("description", ""),
            is_official=True,
            count_target=entry.get("count_target"),
            count_unit_label=entry.get("count_unit_label", ""),
            streak_target_days=entry.get("streak_target_days"),
            streak_grace_days=entry.get("streak_grace_days", 0),
        )
        for index, stage_title in enumerate(entry.get("stages", []), start=1):
            TemplateStage.objects.create(
                template=template, order_index=index, title=stage_title,
            )


def remove_templates(apps, schema_editor):
    ChallengeTemplate = apps.get_model("community", "ChallengeTemplate")
    ChallengeTemplate.objects.filter(is_official=True).delete()


class Migration(migrations.Migration):

    dependencies = [
        ("community", "0013_challengetemplate_templatestage"),
    ]

    operations = [
        migrations.RunPython(seed_templates, remove_templates),
    ]
