import numpy as np
import noisereduce as nr


def preprocess_audio(audio, sample_rate):
    """
    Light preprocessing for microphone-recorded speech.

    Steps:
    1. Convert to float32
    2. Remove DC offset
    3. Light noise reduction
    4. Normalize volume
    """

    # Convert audio to float32
    audio = np.asarray(audio, dtype=np.float32)

    # Remove DC offset
    audio = audio - np.mean(audio)

    # Light noise reduction
    reduced_audio = nr.reduce_noise(
    y=audio,
    sr=sample_rate,
    stationary=False,
    prop_decrease=0.8
)

    # Normalize volume
    max_value = np.max(np.abs(reduced_audio))

    if max_value > 0:
        reduced_audio = reduced_audio / max_value

    return reduced_audio.astype(np.float32)