package com.emat.vehicle_collector_service.api.web

import com.emat.vehicle_collector_service.assets.domain.AnalysisStatus
import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import com.emat.vehicle_collector_service.assets.domain.GpsSource
import com.emat.vehicle_collector_service.assets.domain.RecognitionBadge
import com.emat.vehicle_collector_service.assets.domain.SuggestionStatus
import com.emat.vehicle_collector_service.assets.domain.ThumbnailSize
import com.emat.vehicle_collector_service.assets.domain.VectorStatus
import com.emat.vehicle_collector_service.assets.domain.VerificationStatus
import com.emat.vehicle_collector_service.assets.infra.AnnotationsInfo
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.CaptureInfo
import com.emat.vehicle_collector_service.assets.infra.ExternalInfo
import com.emat.vehicle_collector_service.assets.infra.RagStatus
import com.emat.vehicle_collector_service.assets.infra.RagSync
import com.emat.vehicle_collector_service.assets.infra.VehicleCandidate
import com.emat.vehicle_collector_service.assets.infra.VehicleRecognition
import com.emat.vehicle_collector_service.session.infra.SessionDocument
import com.emat.vehicle_collector_service.support.PublicApiSpec

class PhotoWebControllerIT extends PublicApiSpec {

    def "list returns the frozen envelope and the required fields for a fresh RAW photo"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/photos?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
                .jsonPath('$.page').isEqualTo(0)
                .jsonPath('$.size').isEqualTo(50)
                .jsonPath('$.totalElements').isEqualTo(1)
                .jsonPath('$.totalPages').isEqualTo(1)
                .jsonPath('$.content[0].assetPublicId').isEqualTo(asset.assetPublicId)
                .jsonPath('$.content[0].originalFilename').isEqualTo("sample.jpg")
                .jsonPath('$.content[0].thumbnailUrl').isEqualTo("/api/web/assets/${asset.assetPublicId}/thumb/640".toString())
                .jsonPath('$.content[0].badge').isEqualTo("RAW")
                .jsonPath('$.content[0].favorite').isEqualTo(false)
                .jsonPath('$.content[0].createdAt').exists()
                .jsonPath('$.content[0].title').doesNotExist()
                .jsonPath('$.content[0].brand').doesNotExist()
                .jsonPath('$.content[0].model').doesNotExist()
                .jsonPath('$.content[0].yearFrom').doesNotExist()
                .jsonPath('$.content[0].yearTo').doesNotExist()
                .jsonPath('$.content[0].takenAt').doesNotExist()
                .jsonPath('$.content[0].countryCode').doesNotExist()
                .jsonPath('$.content[0].city').doesNotExist()
                .jsonPath('$.content[0].errorMessage').doesNotExist()
                .jsonPath('$.content[0].sessionPublicId').doesNotExist()
    }

    def "badge filter matches the exact enum value"() {
        given:
        AssetDocument verified = givenAsset(USER_A, null, [], new CaptureInfo(), new AnnotationsInfo(), recognitionWithBadge(RecognitionBadge.VERIFIED))
        givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/photos?ownerId=${USER_A}&badge=VERIFIED")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
                .jsonPath('$.content[0].assetPublicId').isEqualTo(verified.assetPublicId)
                .jsonPath('$.content[0].badge').isEqualTo("VERIFIED")
    }

    def "unknown badge value returns an empty page, not 400"() {
        given:
        givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/photos?ownerId=${USER_A}&badge=NOT_A_REAL_BADGE")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(0)
                .jsonPath('$.totalElements').isEqualTo(0)
    }

    def "favorite filter narrows to favorites only"() {
        given:
        AssetDocument favorite = givenAsset(USER_A, null, [], new CaptureInfo(), new AnnotationsInfo(null, null, true, null, [], []))
        givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/photos?ownerId=${USER_A}&favorite=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
                .jsonPath('$.content[0].assetPublicId').isEqualTo(favorite.assetPublicId)
                .jsonPath('$.content[0].favorite').isEqualTo(true)
    }

    def "sessionPublicId filter returns only photos of that session"() {
        given:
        SessionDocument session = givenSession(USER_A)
        AssetDocument inSession = givenAsset(USER_A, session.sessionPublicId)
        givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/photos?ownerId=${USER_A}&sessionPublicId=${session.sessionPublicId}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
                .jsonPath('$.content[0].assetPublicId').isEqualTo(inSession.assetPublicId)
    }

    def "unassigned filter returns only photos without a session"() {
        given:
        SessionDocument session = givenSession(USER_A)
        givenAsset(USER_A, session.sessionPublicId)
        AssetDocument unassigned = givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/photos?ownerId=${USER_A}&unassigned=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
                .jsonPath('$.content[0].assetPublicId').isEqualTo(unassigned.assetPublicId)
    }

    def "sessionPublicId wins over unassigned when both are sent"() {
        given:
        SessionDocument session = givenSession(USER_A)
        AssetDocument inSession = givenAsset(USER_A, session.sessionPublicId)
        givenAsset(USER_A, null)

        expect:
        webTestClient.get()
                .uri("/api/web/photos?ownerId=${USER_A}&unassigned=true&sessionPublicId=${session.sessionPublicId}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
                .jsonPath('$.content[0].assetPublicId').isEqualTo(inSession.assetPublicId)
    }

    def "q searches title, filename, brand and model case-insensitively"() {
        given:
        AssetDocument ford = givenAsset(
                USER_A, null, [], new CaptureInfo(),
                new AnnotationsInfo("Ford Escort / Ta' Xbiex", null, false, null, [], []),
                new VehicleRecognition()
        )
        givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/photos?ownerId=${USER_A}&q=ford")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
                .jsonPath('$.content[0].assetPublicId').isEqualTo(ford.assetPublicId)
    }

    def "q with regex special characters does not blow up the query"() {
        given:
        givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/photos?ownerId=${USER_A}&q=" + java.net.URLEncoder.encode("(.*)+[", "UTF-8"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(0)
    }

    def "filters combine with AND"() {
        given:
        SessionDocument session = givenSession(USER_A)
        AssetDocument matchesBoth = givenAsset(
                USER_A, session.sessionPublicId, [], new CaptureInfo(),
                new AnnotationsInfo(null, null, true, null, [], [])
        )
        givenAsset(USER_A, session.sessionPublicId)
        givenAsset(USER_A, null, [], new CaptureInfo(), new AnnotationsInfo(null, null, true, null, [], []))

        expect:
        webTestClient.get()
                .uri("/api/web/photos?ownerId=${USER_A}&favorite=true&sessionPublicId=${session.sessionPublicId}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
                .jsonPath('$.content[0].assetPublicId').isEqualTo(matchesBoth.assetPublicId)
    }

    def "totalElements reflects the whole set, not the page"() {
        given:
        5.times { givenAsset(USER_A, null) }

        expect:
        webTestClient.get().uri("/api/web/photos?ownerId=${USER_A}&page=${page}&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(expectedOnPage)
                .jsonPath('$.page').isEqualTo(page)
                .jsonPath('$.totalElements').isEqualTo(5)
                .jsonPath('$.totalPages').isEqualTo(3)

        where:
        page || expectedOnPage
        0    || 2
        1    || 2
        2    || 1
    }

    def "a foreign ownerId sees an empty page, never another owner's photos"() {
        given:
        givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/photos?ownerId=${USER_B}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(0)
                .jsonPath('$.totalElements').isEqualTo(0)
    }

    def "an unknown sort field falls back to createdAt desc instead of 400"() {
        given:
        givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/photos?ownerId=${USER_A}&sort=bogusField,asc")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content.length()').isEqualTo(1)
    }

    def "thumbnail of a ready photo is served with a private cache header"() {
        given:
        AssetDocument asset = givenAssetWithThumbnail(USER_A, null, ThumbnailSize.THUMB_640)

        expect:
        webTestClient.get().uri("/api/web/assets/${asset.assetPublicId}/thumb/640")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Cache-Control", "private, max-age=31536000, immutable")
    }

    def "thumbnail of a photo without a ready thumbnail returns 404"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/assets/${asset.assetPublicId}/thumb/640")
                .exchange()
                .expectStatus().isNotFound()
    }

    def "thumbnail for an unknown assetPublicId returns 404"() {
        expect:
        webTestClient.get().uri("/api/web/assets/does-not-exist/thumb/640")
                .exchange()
                .expectStatus().isNotFound()
    }

    def "thumbnail for an unsupported size returns 404"() {
        given:
        AssetDocument asset = givenAssetWithThumbnail(USER_A, null, ThumbnailSize.THUMB_640)

        expect:
        webTestClient.get().uri("/api/web/assets/${asset.assetPublicId}/thumb/999")
                .exchange()
                .expectStatus().isNotFound()
    }

    def "detail returns the full contract shape for a fresh RAW photo, no session, no candidates"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/photos/${asset.assetPublicId}?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.assetPublicId').isEqualTo(asset.assetPublicId)
                .jsonPath('$.originalFilename').isEqualTo("sample.jpg")
                .jsonPath('$.badge').isEqualTo("RAW")
                .jsonPath('$.favorite').isEqualTo(false)
                .jsonPath('$.albums.length()').isEqualTo(0)
                .jsonPath('$.recognition').doesNotExist()
                .jsonPath('$.vehicleRecognition.bestCandidate').doesNotExist()
                .jsonPath('$.vehicleRecognition.candidates.length()').isEqualTo(0)
                .jsonPath('$.vehicleRecognition.errorMessage').doesNotExist()
                .jsonPath('$.exif.gps').doesNotExist()
                .jsonPath('$.session').doesNotExist()
                .jsonPath('$.notes').doesNotExist()
                .jsonPath('$.externalInfo.length()').isEqualTo(0)
                .jsonPath('$.file.size').isEqualTo(1024)
                .jsonPath('$.file.hash').isEqualTo("sha256:${asset.file.sha256}".toString())
    }

    def "detail splits bestCandidate out of candidates, no duplicate"() {
        given:
        VehicleCandidate best = candidate(5L, "Ford", 0.90d, true)
        VehicleCandidate other = candidate(7L, "Opel", 0.71d, false)
        AssetDocument asset = givenAsset(
                USER_A, null, [], new CaptureInfo(), new AnnotationsInfo(),
                new VehicleRecognition(RecognitionBadge.TO_REVIEW, new RagStatus(), null, null, [best, other], [:], new RagSync())
        )

        expect:
        webTestClient.get().uri("/api/web/photos/${asset.assetPublicId}?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.vehicleRecognition.bestCandidate.brand').isEqualTo("Ford")
                .jsonPath('$.vehicleRecognition.bestCandidate.isSelected').isEqualTo(true)
                .jsonPath('$.vehicleRecognition.candidates.length()').isEqualTo(1)
                .jsonPath('$.vehicleRecognition.candidates[0].brand').isEqualTo("Opel")
    }

    def "detail exposes exif.gps.lng, not lon"() {
        given:
        AssetDocument asset = givenAsset(
                USER_A, null, [], new CaptureInfo(null, new GeoPoint(35.8989055d, 14.4936972d), null, GpsSource.EXIF, null, null),
                new AnnotationsInfo(), new VehicleRecognition()
        )

        expect:
        webTestClient.get().uri("/api/web/photos/${asset.assetPublicId}?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.exif.gps.lat').isEqualTo(35.8989055d)
                .jsonPath('$.exif.gps.lng').isEqualTo(14.4936972d)
                .jsonPath('$.exif.gps.lon').doesNotExist()
    }

    def "detail resolves the session by sessionPublicId"() {
        given:
        SessionDocument session = givenSession(USER_A)
        AssetDocument asset = givenAsset(USER_A, session.sessionPublicId)

        expect:
        webTestClient.get().uri("/api/web/photos/${asset.assetPublicId}?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.session.publicId').isEqualTo(session.sessionPublicId)
                .jsonPath('$.session.name').isEqualTo(session.sessionName)
                .jsonPath('$.session.hasAudio').isEqualTo(false)
    }

    def "detail for an unknown assetPublicId returns 404"() {
        expect:
        webTestClient.get().uri("/api/web/photos/does-not-exist?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isNotFound()
    }

    def "detail for another owner's photo returns 404, not 403 or empty"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/photos/${asset.assetPublicId}?ownerId=${USER_B}")
                .exchange()
                .expectStatus().isNotFound()
    }

    def "preview serves the 640 thumbnail with a private cache header"() {
        given:
        AssetDocument asset = givenAssetWithThumbnail(USER_A, null, ThumbnailSize.THUMB_640)

        expect:
        webTestClient.get().uri("/api/web/assets/${asset.assetPublicId}/preview")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Cache-Control", "private, max-age=31536000, immutable")
    }

    def "preview falls back to the original when the 640 thumbnail is not ready yet"() {
        given:
        AssetDocument asset = givenAssetWithOriginalFile(USER_A, null, mimeType)

        expect:
        webTestClient.get().uri("/api/web/assets/${asset.assetPublicId}/preview")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(expectedContentType)
                // not immutable: the same URL starts serving the thumbnail once it is generated
                .expectHeader().valueEquals("Cache-Control", "private, max-age=0, must-revalidate")

        where:
        mimeType     || expectedContentType
        "image/jpeg" || "image/jpeg"
        "image/png"  || "image/png"
    }

    def "preview of a HEIC without a thumbnail returns 404, the browser could not render it anyway"() {
        given:
        AssetDocument asset = givenAssetWithOriginalFile(USER_A, null, "image/heic")

        expect:
        webTestClient.get().uri("/api/web/assets/${asset.assetPublicId}/preview")
                .exchange()
                .expectStatus().isNotFound()
    }

    def "preview returns 404 when neither the thumbnail nor the original file is on disk"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/assets/${asset.assetPublicId}/preview")
                .exchange()
                .expectStatus().isNotFound()
    }

    def "detail sends recognition axes as frozen-contract values, never the domain-only ones"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null)

        expect:
        webTestClient.get().uri("/api/web/photos/${asset.assetPublicId}?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.vehicleRecognition.vectorStatus').isEqualTo("PENDING")
                .jsonPath('$.vehicleRecognition.analysisStatus').isEqualTo("RECEIVED")
                .jsonPath('$.vehicleRecognition.suggestionStatus').isEqualTo("NONE")
                .jsonPath('$.vehicleRecognition.verificationStatus').isEqualTo("NONE")
    }

    def "detail of an ERROR photo sends bestCandidate null and keeps the whole history"() {
        given:
        VehicleCandidate stale = candidate(5L, "Ford", 0.90d, true)
        VehicleCandidate alsoStale = candidate(7L, "Opel", 0.71d, false)
        AssetDocument asset = givenAsset(
                USER_A, null, [], new CaptureInfo(), new AnnotationsInfo(),
                new VehicleRecognition(
                        RecognitionBadge.ERROR,
                        new RagStatus(VectorStatus.FAILED, AnalysisStatus.NONE, SuggestionStatus.NONE, VerificationStatus.UNVERIFIED),
                        "VectorError: boom",
                        null,
                        [stale, alsoStale],
                        [:],
                        new RagSync()
                )
        )

        expect:
        webTestClient.get().uri("/api/web/photos/${asset.assetPublicId}?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.vehicleRecognition.bestCandidate').doesNotExist()
                .jsonPath('$.vehicleRecognition.candidates.length()').isEqualTo(2)
                .jsonPath('$.vehicleRecognition.errorMessage').isEqualTo("VectorError: boom")
                .jsonPath('$.vehicleRecognition.vectorStatus').isEqualTo("FAILED")
    }

    def "detail of an audio asset returns 404, the endpoint serves photos only"() {
        given:
        AssetDocument audio = givenAudioAsset(USER_A)

        expect:
        webTestClient.get().uri("/api/web/photos/${audio.assetPublicId}?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isNotFound()
    }

    def "session hasAudio is true when the session actually holds an audio asset"() {
        given:
        SessionDocument session = givenSession(USER_A)
        AssetDocument photo = givenAsset(USER_A, session.sessionPublicId)
        givenAudioAsset(USER_A, session.sessionPublicId)

        expect:
        webTestClient.get().uri("/api/web/photos/${photo.assetPublicId}?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.session.hasAudio').isEqualTo(true)
    }

    def "session hasAudio is false when the session holds photos only"() {
        given:
        SessionDocument session = givenSession(USER_A)
        AssetDocument photo = givenAsset(USER_A, session.sessionPublicId)

        expect:
        webTestClient.get().uri("/api/web/photos/${photo.assetPublicId}?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.session.hasAudio').isEqualTo(false)
    }

    def "detail reports the user-corrected location, not the stale EXIF point"() {
        given:
        AssetDocument asset = givenAsset(
                USER_A, null, [],
                new CaptureInfo(
                        null,
                        new GeoPoint(50.0614d, 19.9366d),
                        new GeoPoint(35.8989055d, 14.4936972d),
                        GpsSource.USER,
                        null,
                        null
                ),
                new AnnotationsInfo(), new VehicleRecognition()
        )

        expect:
        webTestClient.get().uri("/api/web/photos/${asset.assetPublicId}?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.exif.gps.lat').isEqualTo(35.8989055d)
                .jsonPath('$.exif.gps.lng').isEqualTo(14.4936972d)
    }

    def "detail never leaks the name of a session owned by somebody else"() {
        given:
        SessionDocument foreignSession = givenSession(USER_B)
        AssetDocument asset = givenAsset(USER_A, foreignSession.sessionPublicId)

        expect:
        webTestClient.get().uri("/api/web/photos/${asset.assetPublicId}?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.session').doesNotExist()
    }

    def "detail drops externalInfo entries without a url"() {
        given:
        AssetDocument asset = givenAsset(
                USER_A, null, [], new CaptureInfo(),
                new AnnotationsInfo(null, null, false, "Plate CAA-247", [
                        new ExternalInfo("link", "Ford Escort", "https://en.wikipedia.org/wiki/Ford_Escort", null, "wikipedia"),
                        new ExternalInfo("note", "Owner said Mk3", null, "numbers-matching", null)
                ], [])
        )

        expect:
        webTestClient.get().uri("/api/web/photos/${asset.assetPublicId}?ownerId=${USER_A}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.notes').isEqualTo("Plate CAA-247")
                .jsonPath('$.externalInfo.length()').isEqualTo(1)
                .jsonPath('$.externalInfo[0].source').isEqualTo("wikipedia")
                .jsonPath('$.externalInfo[0].title').isEqualTo("Ford Escort")
                .jsonPath('$.externalInfo[0].url').isEqualTo("https://en.wikipedia.org/wiki/Ford_Escort")
    }

    private static VehicleRecognition recognitionWithBadge(RecognitionBadge badge) {
        new VehicleRecognition(badge, new RagStatus(), null, null, [], [:], new RagSync())
    }

    private static VehicleCandidate candidate(Long ragCandidateId, String brand, Double confidence, boolean selected) {
        new VehicleCandidate(
                ragCandidateId,
                null,
                selected,
                "similarity",
                brand,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                [],
                confidence,
                false
        )
    }
}
