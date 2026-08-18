package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.api.dto.AssetsOwnerQuery
import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.session.SessionService
import com.emat.vehicle_collector_service.support.PublicApiSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort

class AdminQueriesIT extends PublicApiSpec {

    @Autowired
    AssetsService assetsService

    @Autowired
    SessionService sessionService

    def "assets query without any filter does not blow up on an empty and-operator"() {
        given:
        givenAsset(USER_A, null)
        givenAsset(USER_B, null)

        when:
        def page = assetsService.getAllAssets(query(null, 0, 50)).block()

        then:
        page.content.size() == 2
        page.totalElements == 2L
        page.totalPages == 1
    }

    def "assets query without owner filter sees every owner and paginates for real"() {
        given:
        3.times { givenAsset(USER_A, null) }
        2.times { givenAsset(USER_B, null) }

        when:
        def page = assetsService.getAllAssets(query(null, 1, 2)).block()

        then:
        page.content.size() == 2
        page.totalElements == 5L
        page.totalPages == 3
        page.page == 1
    }

    def "type filter still narrows the unfiltered query"() {
        given:
        2.times { givenAsset(USER_A, null) }

        when:
        def page = assetsService.getAllAssets(query(AssetType.AUDIO, 0, 50)).block()

        then:
        page.content.isEmpty()
        page.totalElements == 0L
    }

    def "sessions query without owner filter returns an envelope over all owners"() {
        given:
        2.times { givenSession(USER_A) }
        givenSession(USER_B)

        when:
        def page = sessionService.listSessions(0, 2, Sort.Direction.DESC).block()

        then:
        page.content.size() == 2
        page.totalElements == 3L
        page.totalPages == 2
    }

    private static AssetsOwnerQuery query(AssetType type, int page, int size) {
        new AssetsOwnerQuery(type, page, size, Sort.Direction.DESC)
    }
}
