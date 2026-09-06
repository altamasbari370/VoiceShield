
package com.altamas.voiceshield.audio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavEncoder {

    fun pcmToWav(
        pcmData: ByteArray,
        sampleRate: Int = 16000,
        channels: Short = 1.toShort(),
        bitsPerSample: Short = 16.toShort()
    ): ByteArray {

        val byteRate =
            sampleRate * channels * bitsPerSample / 8

        val blockAlign =
            (channels * bitsPerSample / 8).toShort()

        val totalDataLength =
            pcmData.size + 36

        val output =
            ByteArrayOutputStream()

        // -----------------------------------------------------
        // RIFF HEADER
        // -----------------------------------------------------

        output.write(
            "RIFF".toByteArray(Charsets.US_ASCII)
        )

        writeIntLE(
            output,
            totalDataLength
        )

        output.write(
            "WAVE".toByteArray(Charsets.US_ASCII)
        )

        // -----------------------------------------------------
        // FORMAT CHUNK
        // -----------------------------------------------------

        output.write(
            "fmt ".toByteArray(Charsets.US_ASCII)
        )

        // Size of fmt chunk
        writeIntLE(
            output,
            16
        )

        // Audio format = PCM
        writeShortLE(
            output,
            1.toShort()
        )

        // Number of channels
        writeShortLE(
            output,
            channels
        )

        // Sample rate
        writeIntLE(
            output,
            sampleRate
        )

        // Byte rate
        writeIntLE(
            output,
            byteRate
        )

        // Block alignment
        writeShortLE(
            output,
            blockAlign
        )

        // Bits per sample
        writeShortLE(
            output,
            bitsPerSample
        )

        // -----------------------------------------------------
        // DATA CHUNK
        // -----------------------------------------------------

        output.write(
            "data".toByteArray(Charsets.US_ASCII)
        )

        writeIntLE(
            output,
            pcmData.size
        )

        output.write(
            pcmData
        )

        return output.toByteArray()
    }


    // =========================================================
    // LITTLE-ENDIAN INT
    // =========================================================

    private fun writeIntLE(
        output: ByteArrayOutputStream,
        value: Int
    ) {

        val buffer =
            ByteBuffer
                .allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt(value)

        output.write(
            buffer.array()
        )
    }


    // =========================================================
    // LITTLE-ENDIAN SHORT
    // =========================================================

    private fun writeShortLE(
        output: ByteArrayOutputStream,
        value: Short
    ) {

        val buffer =
            ByteBuffer
                .allocate(2)
                .order(ByteOrder.LITTLE_ENDIAN)

        buffer.putShort(value)

        output.write(
            buffer.array()
        )
    }
}

