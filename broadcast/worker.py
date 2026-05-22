import os
import json
import time
import tempfile
import subprocess
import psycopg2
import pika
import whisper
from dotenv import load_dotenv

load_dotenv()

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
DB_URL = os.getenv("DATABASE_URL", "postgresql://scoutgg:scoutgg@localhost:5432/scoutgg")
YOUTUBE_URL = os.getenv("LCS_STREAM_URL", "")
CHUNK_SECONDS = 30

model = whisper.load_model("base")


def publish_trigger(channel, match_id: str, player_id: str):
    payload = json.dumps({"matchId": match_id, "playerId": player_id})
    channel.basic_publish(
        exchange="scout.events",
        routing_key="broadcast.signal.detected",
        body=payload,
        properties=pika.BasicProperties(delivery_mode=2),
    )


def extract_signals(transcription: str) -> dict:
    # TODO: replace with real NLP extraction
    return {
        "sentiment": 0.75,
        "transcribed": [transcription[:500]],
    }


def store_signals(match_id: str, player_id: str, signals: dict):
    conn = psycopg2.connect(DB_URL)
    cur = conn.cursor()
    cur.execute(
        """INSERT INTO broadcast_signals (match_id, player_id, sentiment, transcribed, updated_at)
           VALUES (%s, %s, %s, %s, NOW())
           ON CONFLICT (match_id, player_id) DO UPDATE
           SET sentiment = EXCLUDED.sentiment, transcribed = EXCLUDED.transcribed, updated_at = NOW()""",
        (match_id, player_id, signals["sentiment"], json.dumps(signals["transcribed"])),
    )
    conn.commit()
    cur.close()
    conn.close()


def transcribe_chunk(audio_path: str) -> str:
    result = model.transcribe(audio_path)
    return result["text"]


def capture_chunk(url: str, duration: int, output_path: str):
    subprocess.run(
        ["yt-dlp", "-x", "--audio-format", "wav", "--postprocessor-args",
         f"-t {duration}", "-o", output_path, url],
        check=True,
        capture_output=True,
    )


def run(match_id: str):
    connection = pika.BlockingConnection(pika.ConnectionParameters(host=RABBITMQ_HOST))
    channel = connection.channel()
    channel.exchange_declare(exchange="scout.events", exchange_type="topic", durable=True)

    print(f"broadcast worker started for match {match_id}")

    while True:
        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
            chunk_path = f.name

        try:
            capture_chunk(YOUTUBE_URL, CHUNK_SECONDS, chunk_path)
            text = transcribe_chunk(chunk_path)
            signals = extract_signals(text)

            # TODO: resolve real player IDs from transcript mentions
            placeholder_player_id = "all"
            store_signals(match_id, placeholder_player_id, signals)
            publish_trigger(channel, match_id, placeholder_player_id)

            print(f"processed chunk — sentiment: {signals['sentiment']}")
        except Exception as e:
            print(f"chunk error: {e}")
        finally:
            if os.path.exists(chunk_path):
                os.remove(chunk_path)

        time.sleep(1)


if __name__ == "__main__":
    active_match_id = os.getenv("MATCH_ID", "debug-match")
    run(active_match_id)
