import io
import os

import librosa
import soundfile as sf

from app.audio_preprocessing import preprocess_audio


TARGET_SAMPLE_RATE = 16000
TARGET_DURATION_SECONDS = 5.0


def split_audio(audio_data: bytes):
    """
    Process one 5-second audio recording.

    The Android app now sends a complete 5-second WAV.
    There is no 2-second chunking or overlapping windowing.

    Processing:
    1. Convert to mono
    2. Resample to 16 kHz
    3. Apply light preprocessing
    4. Save one temporary 5-second WAV

    Returns the same structure expected by main.py:
        chunks
        sample_rate
        chunk_files
    """

    # -----------------------------------------------------
    # Load uploaded audio
    # -----------------------------------------------------

    audio, sample_rate = librosa.load(
        io.BytesIO(audio_data),
        sr=TARGET_SAMPLE_RATE,
        mono=True
    )

    # -----------------------------------------------------
    # Light audio preprocessing
    # -----------------------------------------------------

    audio = preprocess_audio(
        audio,
        sample_rate
    )

    # -----------------------------------------------------
    # Limit to 5 seconds
    # -----------------------------------------------------

    target_samples = int(
        TARGET_DURATION_SECONDS * sample_rate
    )

    if len(audio) > target_samples:

        audio = audio[:target_samples]

    # -----------------------------------------------------
    # Ignore extremely short recordings
    # -----------------------------------------------------

    if len(audio) < target_samples:

        raise ValueError(
            "Audio must contain approximately 5 seconds of speech"
        )

    # -----------------------------------------------------
    # Temporary backend directory
    # -----------------------------------------------------

    output_dir = "processed_audio"

    os.makedirs(
        output_dir,
        exist_ok=True
    )

    # -----------------------------------------------------
    # Create ONE processed WAV
    # -----------------------------------------------------

    filename = "processed_5sec.wav"

    filepath = os.path.join(
        output_dir,
        filename
    )

    sf.write(
        filepath,
        audio,
        sample_rate
    )

    # -----------------------------------------------------
    # Keep the existing return structure so main.py
    # does not break.
    # -----------------------------------------------------

    chunks = [
        audio
    ]

    chunk_files = [
        filepath
    ]

    return (
        chunks,
        sample_rate,
        chunk_files
    )