def analyze_prediction(prediction: dict) -> dict:
    """
    Convert the raw Aurigin prediction into the
    standardized VoiceShield response used by Android.
    """

    if not isinstance(prediction, dict):
        return {
            "status": "NO_RESULT",
            "confidence": 0.0,
            "spoof_probability": 0.0,
            "message": "No valid prediction received"
        }

    global_data = prediction.get("global", {})

    if not isinstance(global_data, dict):
        return {
            "status": "NO_RESULT",
            "confidence": 0.0,
            "spoof_probability": 0.0,
            "message": "Invalid prediction format"
        }

    result = str(
        global_data.get("result", "")
    ).lower().strip()

    try:
        aurigin_confidence = float(
            global_data.get("confidence", 0.0)
        )
    except (TypeError, ValueError):
        aurigin_confidence = 0.0

    # Keep confidence safely between 0 and 1
    aurigin_confidence = max(
        0.0,
        min(1.0, aurigin_confidence)
    )

    if result == "bonafide":
        status = "GENUINE"

        # Aurigin confidence represents confidence
        # that the voice is genuine.
        confidence = aurigin_confidence * 100

        # Remaining probability represents spoof probability.
        spoof_probability = 1.0 - aurigin_confidence

        message = "Voice appears genuine"

    elif result == "spoofed":
        status = "SUSPICIOUS"

        # Aurigin confidence represents confidence
        # that the voice is spoofed.
        confidence = aurigin_confidence * 100

        spoof_probability = aurigin_confidence

        message = "Possible AI-generated or cloned voice detected"

    elif result == "partially_spoofed":
        status = "SUSPICIOUS"

        confidence = aurigin_confidence * 100

        spoof_probability = aurigin_confidence

        message = "Possible partially AI-generated or cloned voice detected"

    else:
        return {
            "status": "NO_RESULT",
            "confidence": 0.0,
            "spoof_probability": 0.0,
            "message": "Unable to determine voice authenticity"
        }

    return {
        "status": status,
        "confidence": round(confidence, 2),
        "spoof_probability": round(spoof_probability, 4),
        "message": message
    }