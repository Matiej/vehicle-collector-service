package com.emat.vehicle_collector_service.assets

object AssetUploadValidatorFixtures {

    /**
     * JPEG: standardowy plik z nagłówkiem FF D8 FF E0
     * (początek "Start of Image" + marker APP0 / JFIF)
     */
    val JPEG_magicBytes = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
        0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
    )

    /**
     * PNG: sygnatura 8 bajtów — 89 50 4E 47 0D 0A 1A 0A
     */
    val PNG_magicBytes = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47,
        0x0D, 0x0A, 0x1A.toByte(), 0x0A
    )

    /**
     * HEIC: format Apple HEIF/HEIC — "ftypheic" (część rodziny MP4)
     */
    val HEIC_magicBytes = byteArrayOf(
        0x00, 0x00, 0x00, 0x18, // box size (24)
        0x66, 0x74, 0x79, 0x70, // "ftyp"
        0x68, 0x65, 0x69, 0x63, // "heic"
        0x6D, 0x69, 0x66, 0x31  // "mif1" (compatible brand)
    )

    /**
     * MP3: plik MP3 z tagiem ID3v2 — "ID3"
     */
    val MP3_magicBytes = byteArrayOf(
        0x49, 0x44, 0x33, // "ID3"
        0x04, 0x00, 0x00, 0x00, 0x00, 0x21, 0x76
    )

    /**
     * MP4: klasyczny nagłówek MP4 — "ftypisom" z brandem mp42
     */
    val MP4_magicBytes = byteArrayOf(
        0x00, 0x00, 0x00, 0x18, // length (24)
        0x66, 0x74, 0x79, 0x70, // "ftyp"
        0x69, 0x73, 0x6F, 0x6D, // "isom"
        0x6D, 0x70, 0x34, 0x32  // "mp42"
    )

    /**
     * WAV: format RIFF/WAVE
     * "RIFF....WAVE"
     */
    val WAV_magicBytes = byteArrayOf(
        0x52, 0x49, 0x46, 0x46, // "RIFF"
        0x24, 0x08, 0x00, 0x00, // chunk size
        0x57, 0x41, 0x56, 0x45  // "WAVE"
    )


}
