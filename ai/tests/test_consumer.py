import json
from unittest.mock import MagicMock, patch, call
import pytest

from consumer import build_prompt, generate_narrative, on_message


class TestBuildPrompt:
    def test_includes_riot_stats(self):
        riot = {"kills": 10, "deaths": 2, "assists": 5}
        result = build_prompt(riot, {})
        assert '"kills": 10' in result

    def test_includes_broadcast_signals(self):
        broadcast = {"sentiment": 0.9, "transcribed": ["great play"]}
        result = build_prompt({}, broadcast)
        assert '"sentiment": 0.9' in result

    def test_requests_two_to_three_sentence_narrative(self):
        result = build_prompt({}, {})
        assert "2-3 sentence" in result

    def test_empty_inputs_produce_valid_prompt(self):
        result = build_prompt({}, {})
        assert isinstance(result, str)
        assert len(result) > 0


class TestGenerateNarrative:
    def _make_cursor(self, riot_row=None, broadcast_row=None):
        cur = MagicMock()
        cur.fetchone.side_effect = [riot_row, broadcast_row]
        return cur

    def _make_conn(self, cur):
        conn = MagicMock()
        conn.cursor.return_value = cur
        return conn

    @patch("consumer.psycopg2.connect")
    def test_skips_when_no_riot_data(self, mock_connect):
        cur = self._make_cursor(riot_row=None, broadcast_row=None)
        mock_connect.return_value = self._make_conn(cur)

        with patch("consumer.client") as mock_client:
            generate_narrative("match-1", "player-1")
            mock_client.messages.create.assert_not_called()

    @patch("consumer.psycopg2.connect")
    def test_generates_and_stores_narrative(self, mock_connect):
        riot_row = ({"kills": 8, "deaths": 1},)
        cur = self._make_cursor(riot_row=riot_row, broadcast_row=None)
        mock_connect.return_value = self._make_conn(cur)

        mock_message = MagicMock()
        mock_message.content = [MagicMock(text="He is unstoppable tonight.")]

        with patch("consumer.client") as mock_client:
            mock_client.messages.create.return_value = mock_message
            generate_narrative("match-1", "player-1")

        insert_call = cur.execute.call_args_list[-1]
        assert "INSERT INTO narratives" in insert_call[0][0]
        assert "player-1" in insert_call[0][1]

    @patch("consumer.psycopg2.connect")
    def test_uses_broadcast_context_when_available(self, mock_connect):
        riot_row = ({"kills": 5},)
        broadcast_row = (0.85, "incredible performance")
        cur = self._make_cursor(riot_row=riot_row, broadcast_row=broadcast_row)
        mock_connect.return_value = self._make_conn(cur)

        captured_prompt = []

        def capture_create(**kwargs):
            captured_prompt.append(kwargs["messages"][0]["content"])
            msg = MagicMock()
            msg.content = [MagicMock(text="narrative")]
            return msg

        with patch("consumer.client") as mock_client:
            mock_client.messages.create.side_effect = capture_create
            generate_narrative("match-1", "player-1")

        assert "0.85" in captured_prompt[0]

    @patch("consumer.psycopg2.connect")
    def test_commits_transaction(self, mock_connect):
        riot_row = ({"kills": 3},)
        cur = self._make_cursor(riot_row=riot_row, broadcast_row=None)
        conn = self._make_conn(cur)
        mock_connect.return_value = conn

        mock_message = MagicMock()
        mock_message.content = [MagicMock(text="A player to watch.")]

        with patch("consumer.client") as mock_client:
            mock_client.messages.create.return_value = mock_message
            generate_narrative("match-1", "player-1")

        conn.commit.assert_called_once()


class TestOnMessage:
    def _make_channel(self):
        ch = MagicMock()
        method = MagicMock()
        method.delivery_tag = "tag-1"
        return ch, method

    def test_acks_message_on_success(self):
        ch, method = self._make_channel()
        body = json.dumps({"matchId": "m1", "playerId": "p1"}).encode()

        with patch("consumer.generate_narrative") as mock_gen:
            on_message(ch, method, None, body)
            mock_gen.assert_called_once_with("m1", "p1")

        ch.basic_ack.assert_called_once_with(delivery_tag="tag-1")

    def test_skips_missing_match_id(self):
        ch, method = self._make_channel()
        body = json.dumps({"playerId": "p1"}).encode()

        with patch("consumer.generate_narrative") as mock_gen:
            on_message(ch, method, None, body)
            mock_gen.assert_not_called()

        ch.basic_ack.assert_called_once_with(delivery_tag="tag-1")

    def test_skips_missing_player_id(self):
        ch, method = self._make_channel()
        body = json.dumps({"matchId": "m1"}).encode()

        with patch("consumer.generate_narrative") as mock_gen:
            on_message(ch, method, None, body)
            mock_gen.assert_not_called()

        ch.basic_ack.assert_called_once_with(delivery_tag="tag-1")
