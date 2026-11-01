"""Provider abstraction interface for AI assistants."""

from abc import ABC, abstractmethod
from app.schemas import ChatRequest, ChatResponse


class AssistantServiceBase(ABC):
    """Abstract base class for all assistant providers."""

    @abstractmethod
    async def generate_response(self, request: ChatRequest) -> ChatResponse:
        """Process a user chat request and generate an advisory response."""
        pass

