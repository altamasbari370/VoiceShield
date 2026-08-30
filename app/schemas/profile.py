from pydantic import BaseModel, Field
from typing import Optional


# =========================================================
# Profile Create / Update Request
# =========================================================

class ProfileCreate(BaseModel):

    name: str = Field(
        ...,
        min_length=2,
        max_length=100
    )

    age: int = Field(
        ...,
        ge=1,
        le=120
    )

    gender: str = Field(
        ...,
        min_length=1,
        max_length=30
    )

    profile_picture: Optional[str] = None


# =========================================================
# Profile Response
# =========================================================

class ProfileResponse(BaseModel):

    user_id: int

    email: str

    name: Optional[str] = None

    age: Optional[int] = None

    gender: Optional[str] = None

    profile_picture: Optional[str] = None

    profile_completed: bool