def analyze_predictions(predictions):
    """
    Combine multiple Aurigin prediction responses into
    one overall VoiceShield detection result.

    Each item in `predictions` represents one audio batch
    analyzed by Aurigin.
    """

    if not predictions:
        return {
            "status": "NO_RESULT",
            "average_spoof_probability": 0.0,
            "confidence": 0.0,
            "suspicious_segments": 0,
            "total_segments": 0
        }

    spoof_probabilities = []
    suspicious_segments = 0
    total_segments = 0

    for prediction in predictions:

        if not isinstance(prediction, dict):
            continue

        global_data = prediction.get("global", {})

        if not isinstance(global_data, dict):
            continue

        result = str(
            global_data.get("result", "")
        ).lower().strip()

        try:
            confidence = float(
                global_data.get("confidence", 0.0)
            )
        except (TypeError, ValueError):
            confidence = 0.0

        # Keep confidence safely between 0 and 1.
        confidence = max(
            0.0,
            min(1.0, confidence)
        )

        total_segments += 1

        if result == "spoofed":

            spoof_probability = confidence
            suspicious_segments += 1

        elif result == "partially_spoofed":

            spoof_probability = confidence
            suspicious_segments += 1

        elif result == "bonafide":

            spoof_probability = 1.0 - confidence

        else:

            # Unknown result.
            total_segments -= 1
            continue

        spoof_probabilities.append(
            spoof_probability
        )

    if not spoof_probabilities:
        return {
            "status": "NO_RESULT",
            "average_spoof_probability": 0.0,
            "confidence": 0.0,
            "suspicious_segments": 0,
            "total_segments": 0
        }

    # Average spoof probability across all
    # analyzed batches.
    average_spoof_probability = (
        sum(spoof_probabilities)
        / len(spoof_probabilities)
    )

    suspicious_ratio = (
        suspicious_segments
        / total_segments
        if total_segments > 0
        else 0.0
    )

    # VoiceShield decision.
    if (
        average_spoof_probability >= 0.70
        and suspicious_ratio >= 0.50
    ):
        status = "SUSPICIOUS"
    else:
        status = "GENUINE"

    confidence = average_spoof_probability * 100

    return {
        "status": status,
        "average_spoof_probability": round(
            average_spoof_probability,
            4
        ),
        "confidence": round(
            confidence,
            2
        ),
        "suspicious_segments": suspicious_segments,
        "total_segments": total_segments
    }