import os
import json
import pika
import psycopg2
import anthropic
from dotenv import load_dotenv

load_dotenv()

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
DB_URL = os.getenv("DATABASE_URL", "postgresql://scoutgg:scoutgg@localhost:5432/scoutgg")
CLAUDE_MODEL = "claude-sonnet-4-6"

client = anthropic.Anthropic(api_key=os.getenv("ANTHROPIC_API_KEY"))


def build_prompt(riot_data: dict, broadcast: dict) -> str:
    return f"""You are a sports journalist covering the LCS. Generate a 2-3 sentence narrative for this player.
Focus on the human story — what makes them worth watching right now.

Stats: {json.dumps(riot_data, indent=2)}
Broadcast signals: {json.dumps(broadcast, indent=2)}

Return only the narrative. No headers, no bullet points."""


def generate_narrative(match_id: str, player_id: str):
    conn = psycopg2.connect(DB_URL)
    cur = conn.cursor()

    cur.execute(
        "SELECT data FROM riot_data WHERE match_id = %s AND player_id = %s ORDER BY updated_at DESC LIMIT 1",
        (match_id, player_id),
    )
    riot_row = cur.fetchone()

    cur.execute(
        "SELECT sentiment, transcribed FROM broadcast_signals WHERE match_id = %s AND player_id = %s ORDER BY updated_at DESC LIMIT 1",
        (match_id, player_id),
    )
    broadcast_row = cur.fetchone()

    if not riot_row:
        cur.close()
        conn.close()
        return

    riot_data = riot_row[0]
    broadcast = {"sentiment": broadcast_row[0], "transcribed": broadcast_row[1]} if broadcast_row else {}

    prompt = build_prompt(riot_data, broadcast)

    message = client.messages.create(
        model=CLAUDE_MODEL,
        max_tokens=256,
        messages=[{"role": "user", "content": prompt}],
    )

    narrative = message.content[0].text

    cur.execute(
        """INSERT INTO narratives (player_id, match_id, narrative, updated_at)
           VALUES (%s, %s, %s, NOW())
           ON CONFLICT (player_id) DO UPDATE SET narrative = EXCLUDED.narrative, updated_at = NOW()""",
        (player_id, match_id, narrative),
    )
    conn.commit()
    cur.close()
    conn.close()


def on_message(ch, method, properties, body):
    payload = json.loads(body)
    match_id = payload.get("matchId")
    player_id = payload.get("playerId")
    if match_id and player_id:
        generate_narrative(match_id, player_id)
    ch.basic_ack(delivery_tag=method.delivery_tag)


def start_consumer():
    connection = pika.BlockingConnection(pika.ConnectionParameters(host=RABBITMQ_HOST))
    channel = connection.channel()

    channel.exchange_declare(exchange="scout.events", exchange_type="topic", durable=True)

    queue = channel.queue_declare(queue="ai.triggers", durable=True)
    channel.queue_bind(exchange="scout.events", queue="ai.triggers", routing_key="riot.match.updated")
    channel.queue_bind(exchange="scout.events", queue="ai.triggers", routing_key="broadcast.signal.detected")

    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(queue="ai.triggers", on_message_callback=on_message)
    channel.start_consuming()
