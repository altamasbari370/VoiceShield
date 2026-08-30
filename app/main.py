from fastapi import FastAPI, UploadFile, File, HTTPException, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session

from app.audio_processor import split_audio
from app.services.ml_service import predict_chunk
from app.services.decision_service import analyze_predictions
from app.auth import (
    hash_password,
    verify_password,
    create_access_token,
    verify_access_token
)

from app.models import UserRegister, UserLogin
from app.schemas.profile import ProfileCreate, ProfileResponse
from app.database import engine, Base, get_db
from app import db_models


# =========================================================
# FASTAPI APP
# =========================================================

app = FastAPI(title="VoiceShield API")


# =========================================================
# AUTHENTICATION
# =========================================================

security = HTTPBearer()


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db)
):
    """
    Verify JWT token and return the logged-in user.
    """

    token = credentials.credentials

    try:
        payload = verify_access_token(token)

    except ValueError:
        raise HTTPException(
            status_code=401,
            detail="Invalid or expired token"
        )

    user_id = payload.get("sub")

    user = (
        db.query(db_models.User)
        .filter(db_models.User.id == int(user_id))
        .first()
    )

    if user is None:
        raise HTTPException(
            status_code=401,
            detail="User not found"
        )

    return user



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
# AUDIO UPLOAD
# =========================================================
# =========================================================
# AUDIO UPLOAD
# =========================================================



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




@app.post("/upload-audio")
async def upload_audio(
    file: UploadFile = File(...)
):

    audio_data = await file.read()

    # -----------------------------------------------------
    # Split audio into 2-second windows
    # with 1-second stride
    # -----------------------------------------------------

    chunks, sample_rate, chunk_files = split_audio(
        audio_data
    )

    # -----------------------------------------------------
    # Send every chunk to ML model
    # -----------------------------------------------------

    predictions = []

    for chunk_file in chunk_files:

        result = await predict_chunk(
            chunk_file
        )

        predictions.append(result)

    # -----------------------------------------------------
    # Generate overall decision
    # -----------------------------------------------------

    analysis = analyze_predictions(
        predictions
    )

    # -----------------------------------------------------
    # Return complete analysis
    # -----------------------------------------------------

    return {

        "filename": file.filename,

        "content_type": file.content_type,

        "size_bytes": len(audio_data),

        "sample_rate": sample_rate,

        "window_duration_seconds": 2,

        "stride_seconds": 1,

        "number_of_windows": len(chunks),

        # Overall decision for the app
        "analysis": analysis,

        # Detailed results for History / graphs
        "predictions": predictions,

        "message": "Audio processed and analyzed successfully"
    }