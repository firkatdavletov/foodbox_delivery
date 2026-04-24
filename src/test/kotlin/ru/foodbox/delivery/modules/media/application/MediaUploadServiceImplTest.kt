package ru.foodbox.delivery.modules.media.application

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategory
import ru.foodbox.delivery.modules.catalog.domain.CatalogProduct
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductVariant
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductVariantRepository
import ru.foodbox.delivery.modules.herobanners.domain.BannerPlacement
import ru.foodbox.delivery.modules.herobanners.domain.BannerStatus
import ru.foodbox.delivery.modules.herobanners.domain.BannerTextAlignment
import ru.foodbox.delivery.modules.herobanners.domain.BannerThemeVariant
import ru.foodbox.delivery.modules.herobanners.domain.HeroBanner
import ru.foodbox.delivery.modules.herobanners.domain.HeroBannerTranslation
import ru.foodbox.delivery.modules.herobanners.domain.PageResult
import ru.foodbox.delivery.modules.herobanners.domain.repository.HeroBannerRepository
import ru.foodbox.delivery.modules.media.application.command.CreateUploadSessionCommand
import ru.foodbox.delivery.modules.media.domain.ImageProcessingJob
import ru.foodbox.delivery.modules.media.domain.ImageProcessingJobStatus
import ru.foodbox.delivery.modules.media.domain.MediaImage
import ru.foodbox.delivery.modules.media.domain.MediaImageStatus
import ru.foodbox.delivery.modules.media.domain.MediaTargetType
import ru.foodbox.delivery.modules.media.domain.repository.ImageProcessingJobRepository
import ru.foodbox.delivery.modules.media.domain.repository.MediaImageRepository
import ru.foodbox.delivery.modules.media.domain.storage.CreateDirectUploadRequest
import ru.foodbox.delivery.modules.media.domain.storage.DirectUpload
import ru.foodbox.delivery.modules.media.domain.storage.ObjectStoragePort
import ru.foodbox.delivery.modules.media.domain.storage.StoredObjectMetadata
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MediaUploadServiceImplTest {

    private val fixedNow = Instant.parse("2026-04-10T09:30:00Z")
    private val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)

    @Test
    fun `createUploadSession for hero banner normalizes payload and creates direct upload`() {
        val bannerId = UUID.randomUUID()
        val heroBannerRepository = StubHeroBannerRepository(
            mapOf(
                bannerId to heroBanner(id = bannerId),
            )
        )
        val mediaImageRepository = InMemoryMediaImageRepository()
        val storagePort = StubObjectStoragePort(now = fixedNow)
        val properties = MediaUploadProperties().apply {
            allowedContentTypes = listOf("image/jpeg", "image/png")
            maxFileSizeBytes = 10_000
            presignDurationMinutes = 180
        }
        val service = buildService(
            mediaImageRepository = mediaImageRepository,
            storagePort = storagePort,
            heroBannerRepository = heroBannerRepository,
            mediaUploadProperties = properties,
        )

        val session = service.createUploadSession(
            CreateUploadSessionCommand(
                targetType = MediaTargetType.HERO_BANNER,
                targetId = bannerId,
                originalFilename = " C:\\uploads\\hero main.PNG ",
                contentType = "IMAGE/PNG; charset=utf-8",
                fileSize = 1024,
            )
        )

        val uploadRequest = assertNotNull(storagePort.lastDirectUploadRequest)
        assertEquals(Duration.ofMinutes(60), uploadRequest.expiresIn)
        assertEquals("image/png", uploadRequest.contentType)
        assertEquals(1024, uploadRequest.fileSize)

        val image = session.mediaImage
        assertEquals(MediaTargetType.HERO_BANNER, image.targetType)
        assertEquals(bannerId, image.targetId)
        assertEquals("hero main.PNG", image.originalFilename)
        assertEquals("image/png", image.contentType)
        assertEquals(MediaImageStatus.PENDING, image.status)
        assertEquals("test-bucket", image.bucket)
        assertEquals(fixedNow, image.createdAt)
        assertEquals(fixedNow, image.updatedAt)
        assertTrue(image.objectKey.startsWith("hero-banners/$bannerId/"))
        assertTrue(image.objectKey.endsWith(".png"))
    }

    @Test
    fun `createUploadSession fails when hero banner is soft deleted`() {
        val bannerId = UUID.randomUUID()
        val heroBannerRepository = StubHeroBannerRepository(
            mapOf(
                bannerId to heroBanner(id = bannerId, deletedAt = fixedNow),
            )
        )
        val service = buildService(
            heroBannerRepository = heroBannerRepository,
        )

        assertThrows<NotFoundException> {
            service.createUploadSession(
                CreateUploadSessionCommand(
                    targetType = MediaTargetType.HERO_BANNER,
                    targetId = bannerId,
                    originalFilename = "hero.jpg",
                    contentType = "image/jpeg",
                    fileSize = 1024,
                )
            )
        }
    }

    @Test
    fun `completeUpload marks image processing and creates only one job`() {
        val imageId = UUID.randomUUID()
        val image = MediaImage(
            id = imageId,
            targetType = MediaTargetType.HERO_BANNER,
            targetId = UUID.randomUUID(),
            bucket = "test-bucket",
            objectKey = "hero-banners/test/hero.jpg",
            originalFilename = "hero.jpg",
            contentType = "image/jpeg",
            fileSize = 2048,
            status = MediaImageStatus.PENDING,
            publicUrl = null,
            thumbKey = null,
            cardKey = null,
            processingError = null,
            createdAt = fixedNow.minusSeconds(60),
            updatedAt = fixedNow.minusSeconds(60),
        )
        val mediaImageRepository = InMemoryMediaImageRepository(image)
        val jobRepository = InMemoryJobRepository()
        val storagePort = StubObjectStoragePort(
            now = fixedNow,
            metadataByObjectKey = mutableMapOf(
                image.objectKey to StoredObjectMetadata(contentType = "image/jpeg", contentLength = image.fileSize),
            ),
        )
        val service = buildService(
            mediaImageRepository = mediaImageRepository,
            jobRepository = jobRepository,
            storagePort = storagePort,
            imageProcessingProperties = ImageProcessingProperties().apply { maxAttempts = 7 },
        )

        val completed = service.completeUpload(imageId)
        assertEquals(MediaImageStatus.PROCESSING, completed.status)
        assertEquals("https://cdn.example.com/${image.objectKey}", completed.publicUrl)
        assertEquals(fixedNow, completed.updatedAt)
        assertEquals(1, jobRepository.jobs.size)
        assertEquals(ImageProcessingJobStatus.PENDING, jobRepository.jobs.first().status)
        assertEquals(7, jobRepository.jobs.first().maxAttempts)

        service.completeUpload(imageId)
        assertEquals(1, jobRepository.jobs.size)
    }

    @Test
    fun `completeUpload fails when uploaded content type mismatches session`() {
        val imageId = UUID.randomUUID()
        val image = MediaImage(
            id = imageId,
            targetType = MediaTargetType.HERO_BANNER,
            targetId = UUID.randomUUID(),
            bucket = "test-bucket",
            objectKey = "hero-banners/test/hero.jpg",
            originalFilename = "hero.jpg",
            contentType = "image/png",
            fileSize = 2048,
            status = MediaImageStatus.PENDING,
            publicUrl = null,
            thumbKey = null,
            cardKey = null,
            processingError = null,
            createdAt = fixedNow.minusSeconds(60),
            updatedAt = fixedNow.minusSeconds(60),
        )
        val mediaImageRepository = InMemoryMediaImageRepository(image)
        val storagePort = StubObjectStoragePort(
            now = fixedNow,
            metadataByObjectKey = mutableMapOf(
                image.objectKey to StoredObjectMetadata(contentType = "image/jpeg", contentLength = image.fileSize),
            ),
        )
        val service = buildService(
            mediaImageRepository = mediaImageRepository,
            storagePort = storagePort,
        )

        assertThrows<IllegalArgumentException> {
            service.completeUpload(imageId)
        }

        val unchanged = assertNotNull(mediaImageRepository.findById(imageId))
        assertEquals(MediaImageStatus.PENDING, unchanged.status)
        assertEquals(null, unchanged.publicUrl)
    }

    private fun buildService(
        mediaImageRepository: MediaImageRepository = InMemoryMediaImageRepository(),
        jobRepository: ImageProcessingJobRepository = InMemoryJobRepository(),
        storagePort: ObjectStoragePort = StubObjectStoragePort(now = fixedNow),
        heroBannerRepository: HeroBannerRepository = StubHeroBannerRepository(),
        mediaUploadProperties: MediaUploadProperties = MediaUploadProperties(),
        imageProcessingProperties: ImageProcessingProperties = ImageProcessingProperties(),
    ): MediaUploadServiceImpl {
        return MediaUploadServiceImpl(
            mediaImageRepository = mediaImageRepository,
            storagePort = storagePort,
            productRepository = NoOpCatalogProductRepository(),
            categoryRepository = NoOpCatalogCategoryRepository(),
            variantRepository = NoOpCatalogProductVariantRepository(),
            heroBannerRepository = heroBannerRepository,
            mediaUploadProperties = mediaUploadProperties,
            objectKeyFactory = MediaObjectKeyFactory(),
            jobRepository = jobRepository,
            imageProcessingProperties = imageProcessingProperties,
            clock = clock,
        )
    }

    private fun heroBanner(
        id: UUID,
        deletedAt: Instant? = null,
    ): HeroBanner {
        return HeroBanner(
            id = id,
            code = "hero-1",
            storefrontCode = "default",
            placement = BannerPlacement.HOME_HERO,
            status = BannerStatus.DRAFT,
            sortOrder = 10,
            desktopImageUrl = "https://cdn.example.com/hero.jpg",
            mobileImageUrl = null,
            primaryActionUrl = null,
            secondaryActionUrl = null,
            themeVariant = BannerThemeVariant.LIGHT,
            textAlignment = BannerTextAlignment.LEFT,
            startsAt = null,
            endsAt = null,
            publishedAt = null,
            version = 0,
            deletedAt = deletedAt,
            createdAt = fixedNow,
            updatedAt = fixedNow,
            translations = listOf(
                HeroBannerTranslation(
                    id = UUID.randomUUID(),
                    locale = "ru",
                    title = "Title",
                    subtitle = null,
                    description = null,
                    desktopImageAlt = "Alt",
                    mobileImageAlt = null,
                    primaryActionLabel = null,
                    secondaryActionLabel = null,
                )
            ),
        )
    }

    private class InMemoryMediaImageRepository(
        initialImage: MediaImage? = null,
    ) : MediaImageRepository {
        private val store = linkedMapOf<UUID, MediaImage>()

        init {
            if (initialImage != null) {
                store[initialImage.id] = initialImage
            }
        }

        override fun findById(id: UUID): MediaImage? = store[id]

        override fun findAllByIds(ids: Collection<UUID>): List<MediaImage> {
            return ids.mapNotNull(store::get)
        }

        override fun findAllByTargetTypeAndTargetIdIn(targetType: MediaTargetType, targetIds: Collection<UUID>): List<MediaImage> {
            return store.values.filter { it.targetType == targetType && it.targetId in targetIds }
        }

        override fun save(mediaImage: MediaImage): MediaImage {
            store[mediaImage.id] = mediaImage
            return mediaImage
        }

        override fun saveAll(mediaImages: List<MediaImage>): List<MediaImage> {
            mediaImages.forEach(::save)
            return mediaImages
        }
    }

    private class InMemoryJobRepository : ImageProcessingJobRepository {
        val jobs = mutableListOf<ImageProcessingJob>()

        override fun save(job: ImageProcessingJob): ImageProcessingJob {
            jobs.removeIf { it.id == job.id }
            jobs.add(job)
            return job
        }

        override fun findById(id: UUID): ImageProcessingJob? = jobs.firstOrNull { it.id == id }

        override fun findByImageId(imageId: UUID): ImageProcessingJob? = jobs.firstOrNull { it.imageId == imageId }

        override fun claimNextPending(now: Instant, batchSize: Int): List<ImageProcessingJob> = emptyList()
    }

    private class StubObjectStoragePort(
        private val now: Instant,
        val metadataByObjectKey: MutableMap<String, StoredObjectMetadata> = mutableMapOf(),
    ) : ObjectStoragePort {
        var lastDirectUploadRequest: CreateDirectUploadRequest? = null

        override fun bucket(): String = "test-bucket"

        override fun createDirectUpload(request: CreateDirectUploadRequest): DirectUpload {
            lastDirectUploadRequest = request
            return DirectUpload(
                url = "https://upload.example.com/${request.objectKey}",
                method = "PUT",
                requiredHeaders = mapOf("Content-Type" to request.contentType),
                expiresAt = now.plus(request.expiresIn),
            )
        }

        override fun getObjectMetadata(objectKey: String): StoredObjectMetadata? = metadataByObjectKey[objectKey]

        override fun getObjectBytes(objectKey: String): ByteArray {
            error("Not used in tests")
        }

        override fun putObject(objectKey: String, data: ByteArray, contentType: String) {
            error("Not used in tests")
        }

        override fun moveObject(sourceKey: String, destinationKey: String) {
            error("Not used in tests")
        }

        override fun buildPublicUrl(objectKey: String): String = "https://cdn.example.com/$objectKey"
    }

    private class StubHeroBannerRepository(
        private val bannersById: Map<UUID, HeroBanner> = emptyMap(),
    ) : HeroBannerRepository {
        override fun save(banner: HeroBanner): HeroBanner = banner

        override fun saveAll(banners: List<HeroBanner>): List<HeroBanner> = banners

        override fun findById(id: UUID): HeroBanner? = bannersById[id]

        override fun findAllByIds(ids: Collection<UUID>): List<HeroBanner> = ids.mapNotNull(bannersById::get)

        override fun findActiveForStorefront(
            storefrontCode: String,
            placement: BannerPlacement,
            now: Instant,
        ): List<HeroBanner> = emptyList()

        override fun findAllAdmin(
            storefrontCode: String?,
            placement: BannerPlacement?,
            status: BannerStatus?,
            search: String?,
            page: Int,
            size: Int,
        ): PageResult<HeroBanner> {
            return PageResult(
                content = emptyList(),
                page = page,
                size = size,
                totalElements = 0,
                totalPages = 0,
            )
        }
    }

    private class NoOpCatalogProductRepository : CatalogProductRepository {
        override fun findAllActive(categoryId: UUID?, query: String?): List<CatalogProduct> = emptyList()
        override fun findAllByIds(ids: Collection<UUID>): List<CatalogProduct> = emptyList()
        override fun findAllActiveByIds(ids: Collection<UUID>): List<CatalogProduct> = emptyList()
        override fun findAllByIsActive(isActive: Boolean): List<CatalogProduct> = emptyList()
        override fun findById(id: UUID): CatalogProduct? = null
        override fun findActiveById(id: UUID): CatalogProduct? = null
        override fun findByExternalId(externalId: String): CatalogProduct? = null
        override fun findBySku(sku: String): CatalogProduct? = null
        override fun findAllByExternalIdIn(externalIds: Collection<String>): List<CatalogProduct> = emptyList()
        override fun findAllBySlugIn(slugs: Collection<String>): List<CatalogProduct> = emptyList()
        override fun findAllBySkuIn(skus: Collection<String>): List<CatalogProduct> = emptyList()
        override fun save(product: CatalogProduct): CatalogProduct = product
    }

    private class NoOpCatalogCategoryRepository : CatalogCategoryRepository {
        override fun findAll(activeOnly: Boolean, limit: Int): List<CatalogCategory> = emptyList()
        override fun findAllByIsActive(isActive: Boolean): List<CatalogCategory> = emptyList()
        override fun findById(id: UUID): CatalogCategory? = null
        override fun findByExternalId(externalId: String): CatalogCategory? = null
        override fun findBySlug(slug: String): CatalogCategory? = null
        override fun findAllByExternalIdIn(externalIds: Collection<String>): List<CatalogCategory> = emptyList()
        override fun findAllBySlugIn(slugs: Collection<String>): List<CatalogCategory> = emptyList()
        override fun save(category: CatalogCategory): CatalogCategory = category
    }

    private class NoOpCatalogProductVariantRepository : CatalogProductVariantRepository {
        override fun findById(id: UUID): CatalogProductVariant? = null
        override fun findAllByProductId(productId: UUID): List<CatalogProductVariant> = emptyList()
        override fun findAllActiveByProductId(productId: UUID): List<CatalogProductVariant> = emptyList()
        override fun findAllByProductIds(productIds: Collection<UUID>): List<CatalogProductVariant> = emptyList()
        override fun findAllActiveByProductIds(productIds: Collection<UUID>): List<CatalogProductVariant> = emptyList()
        override fun findAllBySkuIn(skus: Collection<String>): List<CatalogProductVariant> = emptyList()
        override fun deleteAllByProductId(productId: UUID) = Unit
        override fun saveAll(variants: List<CatalogProductVariant>): List<CatalogProductVariant> = variants
    }
}
