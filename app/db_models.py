from sqlalchemy import Column, Integer, String, Boolean
from app.database import Base


class User(Base):

    __tablename__ = "users"

    id = Column(
        Integer,
        primary_key=True,
        index=True
    )

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

    # =====================================================
    # PROFILE INFORMATION
    # =====================================================

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