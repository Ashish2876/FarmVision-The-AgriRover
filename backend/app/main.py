"""FarmVision AI Agriculture Assistant - FastAPI Backend Service."""

import logging
from typing import Dict, Any
from fastapi import FastAPI, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import settings
from app.schemas import (
    ChatRequest,
    ChatResponse,
    ErrorResponse,
    HealthResponse,
)
from app.services.gemini_service import GeminiAssistantService
from app.services.mock_service import MockAssistantService

# Configure safe logging (never log API keys or payloads with tokens)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("farmvision-backend")

app = FastAPI(
    title="FarmVision AI Agriculture Assistant",
    description="Backend AI gateway mediating Google Gemini API for the FarmVision AgriRover platform.",
    version=settings.version,
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Service singletons
mock_service = MockAssistantService()
gemini_service = GeminiAssistantService()


@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException) -> JSONResponse:
    """Standardized HTTP error handler without secret leaks."""
    error_code = "HTTP_ERROR"
    if exc.status_code == 503:
        error_code = "MISSING_API_KEY"
    elif exc.status_code == 429:
        error_code = "QUOTA_EXHAUSTED"
    elif exc.status_code == 401:
        error_code = "UNAUTHENTICATED"
    elif exc.status_code == 504:
        error_code = "TIMEOUT"
    elif exc.status_code == 400:
        error_code = "INVALID_REQUEST"

    payload = ErrorResponse(
        error_code=error_code,
        message=str(exc.detail),
        details={"status_code": exc.status_code},
    )
    return JSONResponse(status_code=exc.status_code, content=payload.model_dump())


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
    """Validation error handler returning clean details."""
    payload = ErrorResponse(
        error_code="VALIDATION_ERROR",
        message="Request payload failed validation schema.",
        details={"errors": [err.get("msg") for err in exc.errors()]},
    )
    return JSONResponse(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, content=payload.model_dump())


@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """Catch-all error handler preventing stack trace leaks."""
    logger.error("Unhandled exception processing request: %s", type(exc).__name__)
    payload = ErrorResponse(
        error_code="INTERNAL_SERVER_ERROR",
        message="An unexpected server error occurred. Please try again later.",
        details=None,
    )
    return JSONResponse(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, content=payload.model_dump())


@app.get("/health", response_model=HealthResponse, tags=["Diagnostics"])
@app.get("/api/v1/health", response_model=HealthResponse, tags=["Diagnostics"])
async def health_check() -> HealthResponse:
    """Health check endpoint indicating service status and Gemini readiness."""
    return HealthResponse(
        status="ok",
        service="farmvision-ai-assistant",
        version=settings.version,
        gemini_configured=gemini_service.is_configured(),
        model=settings.gemini_model,
    )


@app.post(
    "/api/v1/assistant/chat",
    response_model=ChatResponse,
    responses={
        400: {"model": ErrorResponse},
        401: {"model": ErrorResponse},
        422: {"model": ErrorResponse},
        429: {"model": ErrorResponse},
        503: {"model": ErrorResponse},
    },
    tags=["Assistant"],
)
async def chat(request: ChatRequest) -> ChatResponse:
    """Process farmer inquiry and return agronomic advisory response with provenance."""
    logger.info(
        "Chat request received: conv_id=%s, msg_len=%d, force_mock=%s",
        request.conversation_id,
        len(request.user_message),
        request.force_mock,
    )

    # 1. Mock service if explicitly requested
    if request.force_mock:
        return await mock_service.generate_response(request)

    # 2. Real Gemini provider if configured
    if gemini_service.is_configured():
        return await gemini_service.generate_response(request)

    # 3. If Gemini is not configured and not forcing mock, fail with clear instructions
    # (Per requirement: "If the key is missing, the backend must return a clear configuration error. Do not fake a successful Gemini response.")
    raise HTTPException(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        detail=(
            "Google Gemini API key is not configured on the backend server. "
            "Set the GEMINI_API_KEY environment variable or populate backend/.env with your replacement key. "
            "For offline demonstration mode, set force_mock=true in your request."
        ),
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=(settings.environment == "development"),
    )

