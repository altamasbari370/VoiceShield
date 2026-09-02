import os
import httpx


ML_API_URL = os.getenv(
    "ML_API_URL",
    "https://swarrakshak-ml.onrender.com/predict"
)


async def predict_chunk(
    chunk_file_path: str
):
    """
    Send one 2-second WAV chunk to the
    SwarRakshak ML FastAPI server.
    """

    try:

        async with httpx.AsyncClient(timeout=30.0) as client:

            with open(chunk_file_path, "rb") as audio_file:

                files = {
                    "file": (
                        chunk_file_path.split("\\")[-1],
                        audio_file,
                        "audio/wav"
                    )
                }

                response = await client.post(
                    ML_API_URL,
                    files=files
                )

        response.raise_for_status()

        return response.json()

    except httpx.HTTPError as e:

        raise RuntimeError(
            f"ML API request failed: {str(e)}"
        )