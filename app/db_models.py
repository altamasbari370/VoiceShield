from sqlalchemy import (
    Column,
    Integer,
    String,
    Boolean,
    Float,
    DateTime,
    ForeignKey,
    func
)

from app.database import Base


# =========================================================
# USER MODEL
# =========================================================

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)

    email = Column(
        String,
        unique=True,
        index=True,
        nullable=False
    )

    password_hash = Column(
        String,
        nullable=False
    )

    name = Column(
        String,
        nullable=True
    )

    age = Column(
        Integer,
        nullable=True
    )

    gender = Column(
        String,
        nullable=True
    )

    profile_picture = Column(
        String,
        nullable=True
    )

    profile_completed = Column(
        Boolean,
        default=False,
        nullable=False
    )


# =========================================================
# CALL HISTORY MODEL
# =========================================================

class CallHistory(Base):
    __tablename__ = "call_history"

    id = Column(
        Integer,
        primary_key=True,
        index=True
    )

    user_id = Column(
        Integer,
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
        index=True
    )

    # Caller information
    caller_name = Column(
        String,
        nullable=True
    )

    caller_number = Column(
        String,
        nullable=True
    )

    # Overall detection result
    status = Column(
        String,
        nullable=False
    )

    confidence = Column(
        Float,
        nullable=False
    )

    spoof_probability = Column(
        Float,
        nullable=False
    )

    duration_seconds = Column(
        Float,
        nullable=False,
        default=0.0
    )

    message = Column(
        String,
        nullable=True
    )

    detected_at = Column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False
    )