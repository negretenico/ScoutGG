"""
Stub out native/heavy dependencies that don't have MSYS2 wheels.
These are patched at the module level in individual tests anyway.
"""
import sys
from unittest.mock import MagicMock

sys.modules.setdefault('psycopg2', MagicMock())
sys.modules.setdefault('whisper', MagicMock())
sys.modules.setdefault('yt_dlp', MagicMock())
