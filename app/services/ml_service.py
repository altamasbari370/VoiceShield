import os
import httpx

AURIGIN_API_URL = "https://api.aurigin.ai/v1/predict"


async def predict_audio_bytes(
    audio_data: bytes,
    filename: str = "recording.wav"
):
    """
    Send one complete 5-second WAV recording directly
    to the Aurigin voice deepfake detection API.

    Audio is kept in memory and sent directly to Aurigin.
    No temporary audio file is created.
    """

    # Read the API key when the function runs,
    # after the application environment has been loaded.
    aurigin_api_key = os.getenv("AURIGIN_API_KEY")

    if not aurigin_api_key:
        raise RuntimeError(
            "AURIGIN_API_KEY environment variable is not configured"
        )

    if not audio_data:
        raise RuntimeError("Audio data is empty")

    try:
        async with httpx.AsyncClient(timeout=60.0) as client:

            files = {
                "file": (
                    filename,
                    audio_data,
                    "audio/wav"
                )
            }

            headers = {
                "x-api-key": aurigin_api_key
            }

            response = await client.post(
                AURIGIN_API_URL,
                headers=headers,
                files=files
            )

        response.raise_for_status()

        return response.json()

    except httpx.HTTPStatusError as e:

        try:
            error_detail = e.response.json()
        except Exception:
            error_detail = e.response.text

        raise RuntimeError(
            f"Aurigin API returned "
            f"{e.response.status_code}: {error_detail}"
        )

    except httpx.RequestError as e:

        raise RuntimeError(
            f"Aurigin API request failed: {str(e)}"
        )

    except Exception as e:

        raise RuntimeError(
            f"Aurigin prediction failed: {str(e)}"
        )