import io
import os

import librosa
import soundfile as sf

from app.audio_preprocessing import preprocess_audio


TARGET_SAMPLE_RATE = 16000


def split_audio(
    audio_data: bytes,
    window_seconds: float = 2.0,
    stride_seconds: float = 1.0
):
    """
    Convert audio to mono 16 kHz,
    apply light preprocessing,
    then split into overlapping windows.

    Window: 2 seconds
    Stride: 1 second
    """

    # Load audio and convert to:
    # - Mono
    # - 16 kHz
    audio, sample_rate = librosa.load(
        io.BytesIO(audio_data),
        sr=TARGET_SAMPLE_RATE,
        mono=True
    )

    # Step 6: Light audio preprocessing
    audio = preprocess_audio(audio, sample_rate)

    # Calculate chunk sizes
    window_size = int(window_seconds * sample_rate)
    stride = int(stride_seconds * sample_rate)

    output_dir = "processed_audio"
    os.makedirs(output_dir, exist_ok=True)

    chunks = []
    chunk_files = []

    start = 0
    chunk_number = 1

    while start + window_size <= len(audio):

        chunk = audio[start:start + window_size]

        filename = f"chunk_{chunk_number:03d}.wav"
        filepath = os.path.join(output_dir, filename)

        sf.write(filepath, chunk, sample_rate)

        chunks.append(chunk)
        chunk_files.append(filepath)

        start += stride
        chunk_number += 1

    return chunks, sample_rate, chunk_files