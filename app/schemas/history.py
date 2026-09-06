from datetime import datetime

from pydantic import BaseModel


class HistoryCreate(BaseModel):
    caller_name: str | None = None
    caller_number: str | None = None

    status: str
    confidence: float
    spoof_probability: float
    duration_seconds: float = 0.0
    message: str | None = None


class HistoryResponse(BaseModel):
    id: int

    caller_name: str | None
    caller_number: str | None

    status: str
    confidence: float
    spoof_probability: float
    duration_seconds: float
    message: str | None
    detected_at: datetime

    class Config:
        from_attributes = True