"""create narratives table

Revision ID: 001
Revises:
Create Date: 2026-06-27
"""

revision = "001"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    from alembic import op
    op.execute("""
        CREATE TABLE IF NOT EXISTS narratives (
            id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
            player_id   VARCHAR(255) NOT NULL UNIQUE,
            match_id    VARCHAR(255) NOT NULL,
            narrative   TEXT        NOT NULL,
            updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )
    """)
    op.execute("CREATE INDEX IF NOT EXISTS idx_narratives_player_id ON narratives (player_id)")


def downgrade() -> None:
    from alembic import op
    op.execute("DROP TABLE IF EXISTS narratives")
