def analyze_predictions(predictions):
    """
    Analyze chunk-level ML predictions and generate
    one overall VoiceShield decision.
    """

    if not predictions:
        return {
            "status": "NO_RESULT",
            "average_spoof_probability": 0.0,
            "confidence": 0.0,
            "suspicious_chunks": 0,
            "total_chunks": 0
        }

    total_chunks = len(predictions)

    # -----------------------------------------------------
    # Extract spoof probabilities
    # -----------------------------------------------------

    spoof_probabilities = [
        float(result["spoof_probability"])
        for result in predictions
    ]

    # -----------------------------------------------------
    # Calculate average spoof probability
    # -----------------------------------------------------

    average_spoof_probability = (
        sum(spoof_probabilities) / total_chunks
    )

    # -----------------------------------------------------
    # Count suspicious chunks
    # -----------------------------------------------------

    suspicious_chunks = sum(
        1
        for result in predictions
        if result["prediction"].upper() == "FAKE"
    )

    # -----------------------------------------------------
    # Suspicious ratio
    # -----------------------------------------------------

    suspicious_ratio = (
        suspicious_chunks / total_chunks
    )

    # -----------------------------------------------------
    # Initial decision rule
    #
    # Suspicious when:
    # - average spoof probability >= 0.70
    #   AND
    # - at least 50% of chunks are FAKE
    # -----------------------------------------------------

    if (
        average_spoof_probability >= 0.70
        and suspicious_ratio >= 0.50
    ):
        status = "SUSPICIOUS"
    else:
        status = "GENUINE"

    # -----------------------------------------------------
    # Confidence displayed to the user
    # -----------------------------------------------------

    confidence = average_spoof_probability * 100

    return {
        "status": status,
        "average_spoof_probability": round(
            average_spoof_probability, 4
        ),
        "confidence": round(
            confidence, 2
        ),
        "suspicious_chunks": suspicious_chunks,
        "total_chunks": total_chunks
    }