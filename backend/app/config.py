"""FarmVision AI Assistant Configuration.

Loads configuration securely from local environment variables or .env file.
API keys and secrets are never committed or printed in logs.
"""

import os
from pathlib import Path
from typing import List
from dotenv import load_dotenv

# Search for .env in backend directory or project root
_BACKEND_DIR = Path(__file__).resolve().parent.parent
_ENV_FILE = _BACKEND_DIR / ".env"
if _ENV_FILE.exists():
    load_dotenv(dotenv_path=_ENV_FILE)
else:
    load_dotenv()


class Settings:
    """Application settings with secure credential handling."""

    def __init__(self) -> None:
        self.app_name: str = "FarmVision AI Agriculture Assistant Backend"
        self.version: str = "1.0.0"
        self.environment: str = os.getenv("APP_ENV", "development")
        self.host: str = os.getenv("HOST", "0.0.0.0")
        self.port: int = int(os.getenv("PORT", "8000"))

        raw_cors = os.getenv("CORS_ORIGINS", "*")
        if raw_cors == "*":
            self.cors_origins: List[str] = ["*"]
        else:
            self.cors_origins = [origin.strip() for origin in raw_cors.split(",") if origin.strip()]

        # Google Gemini Configuration
        self._gemini_api_key: str = (os.getenv("GEMINI_API_KEY") or "").strip()
        self.gemini_model: str = (os.getenv("GEMINI_MODEL") or "gemini-3.8-flash").strip()

        # Safety & Timeout Limits
        self.request_timeout_seconds: float = float(os.getenv("REQUEST_TIMEOUT_SECONDS", "180.0"))
        self.max_message_length: int = int(os.getenv("MAX_MESSAGE_LENGTH", "4000"))
        self.max_history_items: int = int(os.getenv("MAX_HISTORY_ITEMS", "20"))

    @property
    def gemini_api_key(self) -> str:
        """Secure accessor for the Gemini API key."""
        return self._gemini_api_key

    def is_gemini_configured(self) -> bool:
        """Check whether a real Gemini API key is configured and non-placeholder."""
        if not self._gemini_api_key:
            return False
        placeholder_values = {
            "your_gemini_api_key_here",
            "PLACEHOLDER",
            "CHANGE_ME",
            "TODO",
        }
        return self._gemini_api_key not in placeholder_values

    def __repr__(self) -> str:
        masked_key = "***CONFIGURED***" if self.is_gemini_configured() else "NOT_CONFIGURED"
        return (
            f"Settings(app={self.app_name}, version={self.version}, env={self.environment}, "
            f"gemini_model={self.gemini_model}, gemini_key={masked_key})"
        )


settings = Settings()

