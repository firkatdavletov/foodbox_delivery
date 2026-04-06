package ru.foodbox.delivery.modules.media.infrastructure.storage

import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.media.domain.storage.CreateDirectUploadRequest
import ru.foodbox.delivery.modules.media.domain.storage.DirectUpload
import ru.foodbox.delivery.modules.media.domain.storage.ObjectStoragePort
import ru.foodbox.delivery.modules.media.domain.storage.StoredObjectMetadata
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI

@Component
class S3ObjectStorageAdapter(
    private val properties: S3StorageProperties,
) : ObjectStoragePort {

    private val s3ClientDelegate = lazy(::buildS3Client)
    private val s3PresignerDelegate = lazy(::buildS3Presigner)

    private val s3Client: S3Client
        get() = s3ClientDelegate.value

    private val s3Presigner: S3Presigner
        get() = s3PresignerDelegate.value

    override fun bucket(): String {
        validateConfigured()
        return properties.bucket
    }

    override fun createDirectUpload(request: CreateDirectUploadRequest): DirectUpload {
        validateConfigured()

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(properties.bucket)
            .key(request.objectKey)
            .contentType(request.contentType)
            .contentLength(request.fileSize)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(request.expiresIn)
            .putObjectRequest(putObjectRequest)
            .build()

        val presigned = s3Presigner.presignPutObject(presignRequest)
        val requiredHeaders = linkedMapOf<String, String>()
        requiredHeaders["Content-Type"] = request.contentType

        presigned.signedHeaders().forEach { (header, values) ->
            if (!header.equals("host", ignoreCase = true)) {
                requiredHeaders[header] = values.joinToString(",")
            }
        }

        return DirectUpload(
            url = presigned.url().toString(),
            method = "PUT",
            requiredHeaders = requiredHeaders,
            expiresAt = presigned.expiration(),
        )
    }

    override fun getObjectMetadata(objectKey: String): StoredObjectMetadata? {
        validateConfigured()

        return try {
            val response = s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(properties.bucket)
                    .key(objectKey)
                    .build()
            )

            StoredObjectMetadata(
                contentType = response.contentType(),
                contentLength = response.contentLength(),
            )
        } catch (ex: S3Exception) {
            if (ex.statusCode() == 404) {
                null
            } else {
                throw ex
            }
        }
    }

    override fun getObjectBytes(objectKey: String): ByteArray {
        validateConfigured()

        val response = s3Client.getObjectAsBytes(
            GetObjectRequest.builder()
                .bucket(properties.bucket)
                .key(objectKey)
                .build()
        )
        return response.asByteArray()
    }

    override fun putObject(objectKey: String, data: ByteArray, contentType: String) {
        validateConfigured()

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(properties.bucket)
                .key(objectKey)
                .contentType(contentType)
                .contentLength(data.size.toLong())
                .build(),
            RequestBody.fromBytes(data),
        )
    }

    override fun moveObject(sourceKey: String, destinationKey: String) {
        validateConfigured()

        if (sourceKey == destinationKey) {
            return
        }

        s3Client.copyObject(
            CopyObjectRequest.builder()
                .sourceBucket(properties.bucket)
                .sourceKey(sourceKey)
                .destinationBucket(properties.bucket)
                .destinationKey(destinationKey)
                .build()
        )
        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(properties.bucket)
                .key(sourceKey)
                .build()
        )
    }

    override fun buildPublicUrl(objectKey: String): String {
        validateConfigured()

        val baseUrl = properties.publicBaseUrl
            .takeIf { it.isNotBlank() }
            ?: "${properties.endpoint.trimEnd('/')}/${properties.bucket}"

        val normalizedObjectKey = objectKey.trimStart('/')
        return "${baseUrl.trimEnd('/')}/$normalizedObjectKey"
    }

    private fun buildS3Client(): S3Client {
        return S3Client.builder()
            .endpointOverride(URI.create(properties.endpoint))
            .region(Region.of(properties.region))
            .credentialsProvider(credentialsProvider())
            .serviceConfiguration(serviceConfiguration())
            .build()
    }

    private fun buildS3Presigner(): S3Presigner {
        return S3Presigner.builder()
            .endpointOverride(URI.create(properties.endpoint))
            .region(Region.of(properties.region))
            .credentialsProvider(credentialsProvider())
            .serviceConfiguration(serviceConfiguration())
            .build()
    }

    private fun serviceConfiguration(): S3Configuration {
        return S3Configuration.builder()
            .pathStyleAccessEnabled(properties.pathStyleAccess)
            .build()
    }

    private fun credentialsProvider(): StaticCredentialsProvider {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                properties.accessKey,
                properties.secretKey,
            )
        )
    }

    private fun validateConfigured() {
        if (!properties.isConfigured()) {
            throw IllegalStateException("S3 storage is not fully configured")
        }
    }

    @PreDestroy
    fun shutdown() {
        if (s3ClientDelegate.isInitialized()) {
            runCatching { s3Client.close() }
        }

        if (s3PresignerDelegate.isInitialized()) {
            runCatching { s3Presigner.close() }
        }
    }
}
