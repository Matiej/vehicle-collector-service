package com.emat.vehicle_collector_service.assets.infra

import com.emat.vehicle_collector_service.support.PublicApiSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.IndexInfo

class AssetIndexesIT extends PublicApiSpec {

    @Autowired
    ReactiveMongoTemplate mongoTemplate

    def "assets collection has the indexes declared on the document"() {
        given:
        givenAsset(USER_A, null)

        when:
        List<IndexInfo> indexes = mongoTemplate.indexOps(AssetDocument.ASSET_COLLECTION_NAME)
                .indexInfo.collectList().block()
        Set<String> indexedFields = indexes.collect { it.indexFields.collect { field -> field.key } }.flatten() as Set

        then:
        indexedFields.containsAll(["assetPublicId", "ownerId", "createdAt", "sessionPublicId",
                                   "file.sha256", "capture.takenAt"])

        and:
        indexes.find { it.name == "assetPublicId" }.unique
    }
}
