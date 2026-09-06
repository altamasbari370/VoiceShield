from fastapi import FastAPI, UploadFile, File, HTTPException, Depends
from sqlalchemy.orm import Session
from pathlib import Path
from dotenv import load_dotenv


# =========================================================
# ENVIRONMENT
# =========================================================

BASE_DIR = Path(__file__).resolve().parent.parent
load_dotenv(BASE_DIR / ".env")


# =========================================================
# SERVICES
# =========================================================

from app.services.ml_service import predict_audio_bytes
from app.services.decision_service import analyze_prediction


# =========================================================
# ROUTERS
# =========================================================

from app.routers.history import router as history_router


# =========================================================
# AUTH DEPENDENCY
# =========================================================

from app.dependencies import get_current_user


# =========================================================
# AUTHENTICATION
# =========================================================

from app.auth import (
    hash_password,
    verify_password,
    create_access_token
)


# =========================================================
# REQUEST MODELS
# =========================================================

from app.models import (
    UserRegister,
    UserLogin,
    ChangePasswordRequest
)


# =========================================================
# PROFILE SCHEMAS
# =========================================================

from app.schemas.profile import (
    ProfileCreate,
    ProfileResponse
)


# =========================================================
# DATABASE
# =========================================================

from app.database import (
    engine,
    Base,
    get_db
)

from app import db_models


# =========================================================
# FASTAPI APP
# =========================================================

app = FastAPI(title="SwarRakshak")

app.include_router(history_router)


# =========================================================
# CREATE DATABASE TABLES
# =========================================================

Base.metadata.create_all(bind=engine)


# =========================================================
# ROOT
# =========================================================

@app.get("/")
def root():

    return {
        "message": "VoiceShield backend is running!"
    }


# =========================================================
# REGISTER
# =========================================================

@app.post("/auth/register")
def register(
    user: UserRegister,
    db: Session = Depends(get_db)
):

    email = user.email.lower().strip()

    # -----------------------------------------------------
    # Password validation
    # -----------------------------------------------------

    if len(user.password) < 8:

        raise HTTPException(
            status_code=400,
            detail="Password must be at least 8 characters"
        )

    # -----------------------------------------------------
    # Check if user already exists
    # -----------------------------------------------------

    existing_user = (
        db.query(db_models.User)
        .filter(db_models.User.email == email)
        .first()
    )

    if existing_user:

        raise HTTPException(
            status_code=400,
            detail="User already exists"
        )

    # -----------------------------------------------------
    # Hash password
    # -----------------------------------------------------

    hashed_password = hash_password(
        user.password
    )

    # -----------------------------------------------------
    # Create database user
    # -----------------------------------------------------

    new_user = db_models.User(
        email=email,
        password_hash=hashed_password
    )

    db.add(new_user)

    db.commit()

    db.refresh(new_user)

    return {

        "message": "User registered successfully",

        "email": new_user.email,

        "user_id": new_user.id
    }


# =========================================================
# LOGIN
# =========================================================

@app.post("/auth/login")
def login(
    user: UserLogin,
    db: Session = Depends(get_db)
):

    email = user.email.lower().strip()

    # -----------------------------------------------------
    # Find user in PostgreSQL
    # -----------------------------------------------------

    stored_user = (
        db.query(db_models.User)
        .filter(db_models.User.email == email)
        .first()
    )

    # -----------------------------------------------------
    # User doesn't exist
    # -----------------------------------------------------

    if stored_user is None:

        raise HTTPException(
            status_code=401,
            detail="Invalid email or password"
        )

    # -----------------------------------------------------
    # Verify password
    # -----------------------------------------------------

    if not verify_password(
        user.password,
        stored_user.password_hash
    ):

        raise HTTPException(
            status_code=401,
            detail="Invalid email or password"
        )

    # -----------------------------------------------------
    # Create JWT token
    # -----------------------------------------------------

    access_token = create_access_token(
        {
            "sub": str(stored_user.id),
            "email": stored_user.email
        }
    )

    return {

        "message": "Login successful",

        "access_token": access_token,

        "token_type": "bearer",

        "user_id": stored_user.id,

        "email": stored_user.email
    }


# =========================================================
# CHANGE PASSWORD
# =========================================================

@app.post("/auth/change-password")
def change_password(
    request: ChangePasswordRequest,
    current_user: db_models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):

    # -----------------------------------------------------
    # Validate new password length
    # -----------------------------------------------------

    if len(request.new_password) < 8:

        raise HTTPException(
            status_code=400,
            detail="New password must be at least 8 characters"
        )

    # -----------------------------------------------------
    # Make sure new password is different
    # -----------------------------------------------------

    if request.current_password == request.new_password:

        raise HTTPException(
            status_code=400,
            detail="New password must be different from current password"
        )

    # -----------------------------------------------------
    # Verify current password
    # -----------------------------------------------------

    if not verify_password(
        request.current_password,
        current_user.password_hash
    ):

        raise HTTPException(
            status_code=401,
            detail="Current password is incorrect"
        )

    # -----------------------------------------------------
    # Hash new password
    # -----------------------------------------------------

    current_user.password_hash = hash_password(
        request.new_password
    )

    # -----------------------------------------------------
    # Save new password to PostgreSQL
    # -----------------------------------------------------

    db.commit()

    db.refresh(current_user)

    return {

        "message": "Password changed successfully"
    }


# =========================================================
# GET PROFILE
# =========================================================

@app.get(
    "/profile",
    response_model=ProfileResponse
)
def get_profile(
    current_user: db_models.User = Depends(get_current_user)
):

    return {

        "user_id": current_user.id,

        "email": current_user.email,

        "name": current_user.name,

        "age": current_user.age,

        "gender": current_user.gender,

        "profile_picture": current_user.profile_picture,

        "profile_completed": current_user.profile_completed
    }


# =========================================================
# CREATE / UPDATE PROFILE
# =========================================================

@app.post(
    "/profile",
    response_model=ProfileResponse
)
def create_profile(
    profile: ProfileCreate,
    current_user: db_models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):

    current_user.name = profile.name.strip()

    current_user.age = profile.age

    current_user.gender = profile.gender.strip()

    current_user.profile_picture = profile.profile_picture

    current_user.profile_completed = True

    db.commit()

    db.refresh(current_user)

    return {

        "user_id": current_user.id,

        "email": current_user.email,

        "name": current_user.name,

        "age": current_user.age,

        "gender": current_user.gender,

        "profile_picture": current_user.profile_picture,

        "profile_completed": current_user.profile_completed
    }


# =========================================================
# AUDIO UPLOAD / AI DETECTION
# =========================================================

@app.post("/upload-audio")
async def upload_audio(
    file: UploadFile = File(...)
):
    """
    Receive one complete 5-second WAV recording.

    Architecture:

        Android microphone
                ↓
        5-second audio in RAM
                ↓
        FastAPI
                ↓
        Aurigin API
                ↓
        Prediction returned to Android

    There is NO:
        - 2-second chunking
        - 1-second overlapping window
        - processed_audio storage
        - local audio-file processing
    """

    # -----------------------------------------------------
    # Validate file type
    # -----------------------------------------------------

    if file.content_type not in (
        "audio/wav",
        "audio/x-wav",
        "audio/wave",
        "application/octet-stream"
    ):

        raise HTTPException(
            status_code=400,
            detail="Only WAV audio files are supported"
        )

    # -----------------------------------------------------
    # Read audio into backend RAM
    # -----------------------------------------------------

    audio_data = await file.read()

    if not audio_data:

        raise HTTPException(
            status_code=400,
            detail="Audio file is empty"
        )

    # -----------------------------------------------------
    # Basic size protection
    #
    # A 5-second mono 16 kHz 16-bit WAV is normally around
    # 160 KB plus a small WAV header.
    #
    # This limit prevents accidentally sending huge files
    # to the external AI API.
    # -----------------------------------------------------

    MAX_AUDIO_SIZE = 2 * 1024 * 1024

    if len(audio_data) > MAX_AUDIO_SIZE:

        raise HTTPException(
            status_code=400,
            detail="Audio file is too large. Please send a 5-second WAV recording."
        )

    # -----------------------------------------------------
    # Send the complete recording directly to Aurigin
    # -----------------------------------------------------

    try:

        prediction = await predict_audio_bytes(
            audio_data=audio_data,
            filename=file.filename or "recording.wav"
        )

    except RuntimeError as e:

        raise HTTPException(
            status_code=502,
            detail=str(e)
        )

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=f"Audio analysis failed: {str(e)}"
        )

    # -----------------------------------------------------
    # Analyze prediction
    # -----------------------------------------------------

    analysis = analyze_prediction(prediction)

    # -----------------------------------------------------
    # Return prediction
    # -----------------------------------------------------

    return {
        "filename": file.filename,
        "content_type": file.content_type,
        "size_bytes": len(audio_data),
        "duration_seconds": prediction.get(
            "audio_duration",
            5
        ),
        "prediction": prediction,
        "analysis": analysis,
        "message": "Audio analyzed successfully"
    }