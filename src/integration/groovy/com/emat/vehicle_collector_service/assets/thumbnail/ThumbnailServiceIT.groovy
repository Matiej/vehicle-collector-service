package com.emat.vehicle_collector_service.assets.thumbnail

import com.emat.vehicle_collector_service.assets.domain.AssetStatus
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.support.PublicApiSpec
import org.springframework.beans.factory.annotation.Autowired

import java.nio.file.Files
import java.nio.file.Path

class ThumbnailServiceIT extends PublicApiSpec {

    @Autowired
    ThumbnailService thumbnailService

    def "successful generation moves the asset to THUMBS_READY"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null)
        writeSampleImage(asset.file.storageKeyPath)

        when:
        thumbnailService.generateAndSave(asset.id, asset.assetPublicId, asset.file.storageKeyPath).block()

        then:
        AssetDocument stored = assetRepository.findByAssetPublicId(asset.assetPublicId).block()
        stored.file.status == AssetStatus.THUMBS_READY
        stored.file.failureReason == null
        !stored.file.thumbnails.isEmpty()
    }

    def "missing original leaves the asset FAILED with a reason"() {
        given:
        AssetDocument asset = givenAsset(USER_A, null)

        when:
        thumbnailService.generateAndSave(asset.id, asset.assetPublicId, asset.file.storageKeyPath).block()

        then:
        thrown(Exception)

        and:
        AssetDocument stored = assetRepository.findByAssetPublicId(asset.assetPublicId).block()
        stored.file.status == AssetStatus.FAILED
        stored.file.failureReason?.trim()
        !stored.file.failureReason.contains(asset.file.storageKeyPath)
    }

    private void writeSampleImage(String storageKeyPath) {
        Path target = Path.of(appData.getAssetsDir()).resolve(storageKeyPath)
        Files.createDirectories(target.parent)
        Files.write(target, getClass().getResourceAsStream("/assets/sample.jpg").bytes)
    }
}
