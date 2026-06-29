"""create broadcast_signals table

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
        CREATE TABLE IF NOT EXISTS broadcast_signals (
            id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
            match_id     VARCHAR(255) NOT NULL,
            player_id    VARCHAR(255) NOT NULL,
            sentiment    FLOAT       NOT NULL,
            transcribed  JSONB       NOT NULL DEFAULT '[]',
            updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            UNIQUE (match_id, player_id)
        )
    """)
    op.execute("CREATE INDEX IF NOT EXISTS idx_broadcast_signals_match_player ON broadcast_signals (match_id, player_id)")


def downgrade() -> None:
    from alembic import op
    op.execute("DROP TABLE IF EXISTS broadcast_signals")
