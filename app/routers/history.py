from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user
from app import db_models
from app.schemas.history import HistoryCreate, HistoryResponse


router = APIRouter(
    prefix="/history",
    tags=["History"]
)


# =========================================================
# CREATE HISTORY
# =========================================================

@router.post(
    "",
    response_model=HistoryResponse
)
def create_history(
    history: HistoryCreate,
    db: Session = Depends(get_db),
    current_user: db_models.User = Depends(get_current_user)
):

    new_history = db_models.CallHistory(
        user_id=current_user.id,

        # Caller information
        caller_name=history.caller_name,
        caller_number=history.caller_number,

        # Overall call detection result
        status=history.status,
        confidence=history.confidence,
        spoof_probability=history.spoof_probability,
        duration_seconds=history.duration_seconds,
        message=history.message
    )

    db.add(new_history)
    db.commit()
    db.refresh(new_history)

    return new_history


# =========================================================
# GET LAST 10 HISTORY RECORDS
# =========================================================

@router.get(
    "",
    response_model=list[HistoryResponse]
)
def get_history(
    db: Session = Depends(get_db),
    current_user: db_models.User = Depends(get_current_user)
):

    history = (
        db.query(db_models.CallHistory)
        .filter(
            db_models.CallHistory.user_id == current_user.id
        )
        .order_by(
            db_models.CallHistory.detected_at.desc()
        )
        .limit(10)
        .all()
    )

    return history


# =========================================================
# DELETE ONE HISTORY RECORD
# =========================================================

@router.delete(
    "/{history_id}"
)
def delete_history(
    history_id: int,
    db: Session = Depends(get_db),
    current_user: db_models.User = Depends(get_current_user)
):

    history = (
        db.query(db_models.CallHistory)
        .filter(
            db_models.CallHistory.id == history_id,
            db_models.CallHistory.user_id == current_user.id
        )
        .first()
    )

    if history is None:
        raise HTTPException(
            status_code=404,
            detail="History record not found"
        )

    db.delete(history)
    db.commit()

    return {
        "message": "History deleted successfully"
    }