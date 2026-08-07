package com.emat.vehicle_collector_service.support

import com.emat.vehicle_collector_service.VehicleCollectorServiceApplication
import com.emat.vehicle_collector_service.assets.domain.AssetStatus
import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.assets.domain.LocationSource
import com.emat.vehicle_collector_service.assets.domain.ThumbnailSize
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.AssetRepository
import com.emat.vehicle_collector_service.assets.infra.Thumbnail
import com.emat.vehicle_collector_service.configuration.AppData
import com.emat.vehicle_collector_service.session.domain.SessionMode
import com.emat.vehicle_collector_service.session.domain.SessionStatus
import com.emat.vehicle_collector_service.session.infra.SessionDocument
import com.emat.vehicle_collector_service.session.infra.SessionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.MongoDBContainer
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Supplier

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt

@SpringBootTest(
        classes = VehicleCollectorServiceApplication,
        properties = ["spring.config.location=classpath:/application-integration.yml"]
)
@AutoConfigureWebTestClient
abstract public class PublicApiSpec extends Specification {

    static final String USER_A = "11111111-1111-1111-1111-111111111111"
    static final String USER_B = "22222222-2222-2222-2222-222222222222"

    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0")

    static {
        MONGO.start()
    }

    @DynamicPropertySource
    static void mongoUri(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", { MONGO.replicaSetUrl } as Supplier)
    }

    @Autowired
    protected WebTestClient webTestClient

    @Autowired
    protected AssetRepository assetRepository

    @Autowired
    protected SessionRepository sessionRepository

    @Autowired
    protected AppData appData

    def setup() {
        assetRepository.deleteAll().block()
        sessionRepository.deleteAll().block()
    }

    protected WebTestClient asUser(String ownerId) {
        webTestClient.mutateWith(
                mockJwt()
                        .jwt { jwt -> jwt.subject(ownerId) }
                        .authorities(new SimpleGrantedAuthority("ROLE_REGULAR_USER"))
        )
    }

    protected SessionDocument givenSession(String ownerId, SessionStatus status = SessionStatus.CREATED) {
        sessionRepository.save(
                new SessionDocument(
                        null,
                        "session of $ownerId",
                        "sess_it_${shortId()}",
                        ownerId,
                        SessionMode.BULK,
                        null,
                        status,
                        "integration-test",
                        null,
                        null,
                        null
                )
        ).block()
    }

    protected AssetDocument givenAsset(String ownerId, String sessionPublicId, List<Thumbnail> thumbnails = []) {
        assetRepository.save(
                new AssetDocument(
                        null,
                        "asset_it_${shortId()}",
                        ownerId,
                        sessionPublicId,
                        null,
                        AssetType.IMAGE,
                        "image/jpeg",
                        "sample.jpg",
                        "image/2026/8/${shortId()}.jpg",
                        LocationSource.UNKNOWN,
                        null,
                        null,
                        AssetStatus.RAW,
                        thumbnails,
                        null,
                        null,
                        null
                )
        ).block()
    }

    protected AssetDocument givenAssetWithThumbnail(String ownerId, String sessionPublicId) {
        String thumbnailKeyPath = "thumbnails/${shortId()}_320.jpg"
        AssetDocument asset = givenAsset(ownerId, sessionPublicId, [new Thumbnail(ThumbnailSize.THUMB_320, thumbnailKeyPath)])
        writeThumbnailFile(thumbnailKeyPath)
        asset
    }

    private void writeThumbnailFile(String thumbnailKeyPath) {
        Path path = Path.of(appData.getAssetsDir()).resolve(thumbnailKeyPath)
        Files.createDirectories(path.parent)
        Files.write(path, [0xFF, 0xD8, 0xFF, 0xD9] as byte[])
    }

    private static String shortId() {
        UUID.randomUUID().toString().take(8)
    }
}
