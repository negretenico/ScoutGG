import json
from unittest.mock import MagicMock, patch, call
import pytest

from worker import extract_signals, store_signals, publish_trigger, transcribe_chunk, capture_chunk, run


class TestExtractSignals:
    def test_returns_sentiment_score(self):
        result = extract_signals("Amazing play by Faker tonight!")
        assert "sentiment" in result
        assert isinstance(result["sentiment"], float)

    def test_returns_transcribed_list(self):
        result = extract_signals("Some broadcast text here")
        assert "transcribed" in result
        assert isinstance(result["transcribed"], list)

    def test_truncates_long_transcription(self):
        long_text = "a" * 1000
        result = extract_signals(long_text)
        assert len(result["transcribed"][0]) <= 500

    def test_empty_transcription_handled(self):
        result = extract_signals("")
        assert result["sentiment"] is not None
        assert isinstance(result["transcribed"], list)


class TestStoreSignals:
    def _make_conn_and_cursor(self):
        cur = MagicMock()
        conn = MagicMock()
        conn.cursor.return_value = cur
        return conn, cur

    @patch("worker.psycopg2.connect")
    def test_inserts_broadcast_signal(self, mock_connect):
        conn, cur = self._make_conn_and_cursor()
        mock_connect.return_value = conn

        signals = {"sentiment": 0.8, "transcribed": ["great play"]}
        store_signals("match-1", "player-1", signals)

        insert_call = cur.execute.call_args
        assert "INSERT INTO broadcast_signals" in insert_call[0][0]
        args = insert_call[0][1]
        assert args[0] == "match-1"
        assert args[1] == "player-1"
        assert args[2] == 0.8

    @patch("worker.psycopg2.connect")
    def test_commits_transaction(self, mock_connect):
        conn, cur = self._make_conn_and_cursor()
        mock_connect.return_value = conn

        store_signals("match-1", "player-1", {"sentiment": 0.5, "transcribed": []})
        conn.commit.assert_called_once()

    @patch("worker.psycopg2.connect")
    def test_closes_connection(self, mock_connect):
        conn, cur = self._make_conn_and_cursor()
        mock_connect.return_value = conn

        store_signals("match-1", "player-1", {"sentiment": 0.5, "transcribed": []})
        cur.close.assert_called_once()
        conn.close.assert_called_once()


class TestTranscribeChunk:
    def test_returns_text_from_model(self):
        import worker
        worker.model.transcribe.return_value = {"text": "amazing teamfight"}
        result = transcribe_chunk("/tmp/chunk.wav")
        assert result == "amazing teamfight"

    def test_passes_audio_path_to_model(self):
        import worker
        worker.model.transcribe.return_value = {"text": ""}
        transcribe_chunk("/tmp/test.wav")
        worker.model.transcribe.assert_called_with("/tmp/test.wav")


class TestCaptureChunk:
    @patch("worker.subprocess.run")
    def test_calls_yt_dlp_with_url_and_duration(self, mock_run):
        capture_chunk("https://youtube.com/stream", 30, "/tmp/out.wav")
        mock_run.assert_called_once()
        cmd = mock_run.call_args[0][0]
        assert "yt-dlp" in cmd
        assert "/tmp/out.wav" in cmd

    @patch("worker.subprocess.run")
    def test_raises_on_failure(self, mock_run):
        import subprocess
        mock_run.side_effect = subprocess.CalledProcessError(1, "yt-dlp")
        with pytest.raises(subprocess.CalledProcessError):
            capture_chunk("https://youtube.com/stream", 30, "/tmp/out.wav")


class TestRun:
    @patch("worker.time")
    @patch("worker.publish_trigger")
    @patch("worker.store_signals")
    @patch("worker.transcribe_chunk", return_value="great play by Ruler")
    @patch("worker.capture_chunk")
    @patch("worker.pika.BlockingConnection")
    def test_processes_one_chunk_then_stops(
        self, mock_conn, mock_capture, mock_transcribe, mock_store, mock_publish, mock_time
    ):
        # Break the infinite loop after the first iteration
        mock_time.sleep.side_effect = KeyboardInterrupt

        with pytest.raises(KeyboardInterrupt):
            run("match-99")

        mock_capture.assert_called_once()
        mock_transcribe.assert_called_once()
        mock_store.assert_called_once()
        mock_publish.assert_called_once()

    @patch("worker.time")
    @patch("worker.publish_trigger")
    @patch("worker.store_signals")
    @patch("worker.transcribe_chunk", side_effect=Exception("ffmpeg error"))
    @patch("worker.capture_chunk")
    @patch("worker.pika.BlockingConnection")
    def test_continues_after_chunk_error(
        self, mock_conn, mock_capture, mock_transcribe, mock_store, mock_publish, mock_time
    ):
        # First sleep continues, second breaks the loop
        mock_time.sleep.side_effect = [None, KeyboardInterrupt]

        with pytest.raises(KeyboardInterrupt):
            run("match-99")

        # store and publish never called due to error
        mock_store.assert_not_called()
        mock_publish.assert_not_called()


class TestPublishTrigger:
    def test_publishes_to_correct_routing_key(self):
        channel = MagicMock()
        publish_trigger(channel, "match-1", "player-1")

        channel.basic_publish.assert_called_once()
        kwargs = channel.basic_publish.call_args[1]
        assert kwargs["routing_key"] == "broadcast.signal.detected"

    def test_payload_contains_match_and_player(self):
        channel = MagicMock()
        publish_trigger(channel, "match-99", "player-42")

        kwargs = channel.basic_publish.call_args[1]
        payload = json.loads(kwargs["body"])
        assert payload["matchId"] == "match-99"
        assert payload["playerId"] == "player-42"

    def test_message_is_persistent(self):
        import pika
        channel = MagicMock()
        publish_trigger(channel, "m", "p")

        kwargs = channel.basic_publish.call_args[1]
        assert kwargs["properties"].delivery_mode == 2
