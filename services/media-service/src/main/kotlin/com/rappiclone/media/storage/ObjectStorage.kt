package com.rappiclone.media.storage

import io.minio.*
import io.minio.http.Method
import org.slf4j.LoggerFactory
import java.io.InputStream

/**
 * Interface abstrata pra object storage.
 * Implementacao concreta usa MinIO (S3-compatible).
 * Permite trocar pra AWS S3, GCS, etc. sem mudar nada.
 */
interface ObjectStorage {
    suspend fun upload(bucket: String, key: String, data: InputStream, contentType: String, size: Long)
    suspend fun delete(bucket: String, key: String)
    suspend fun getUrl(bucket: String, key: String): String
    suspend fun ensureBucket(bucket: String)
}

class MinioObjectStorage(
    private val endpoint: String,
    private val accessKey: String,
    private val secretKey: String,
    private val publicBaseUrl: String
) : ObjectStorage {

    private val logger = LoggerFactory.getLogger(MinioObjectStorage::class.java)

    private val client: MinioClient = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build()

    override suspend fun upload(bucket: String, key: String, data: InputStream, contentType: String, size: Long) {
        client.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(key)
                .stream(data, size, -1)
                .contentType(contentType)
                .build()
        )
        logger.debug("Uploaded: $bucket/$key ($size bytes, $contentType)")
    }

    override suspend fun delete(bucket: String, key: String) {
        client.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucket)
                .`object`(key)
                .build()
        )
        logger.debug("Deleted: $bucket/$key")
    }

    override suspend fun getUrl(bucket: String, key: String): String {
        return "$publicBaseUrl/$bucket/$key"
    }

    override suspend fun ensureBucket(bucket: String) {
        val exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
            // Set bucket policy pra acesso publico de leitura (imagens de produto, avatares)
            val policy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [{
                        "Effect": "Allow",
                        "Principal": {"AWS": ["*"]},
                        "Action": ["s3:GetObject"],
                        "Resource": ["arn:aws:s3:::$bucket/*"]
                    }]
                }
            """.trimIndent()
            client.setBucketPolicy(
                SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build()
            )
            logger.info("Bucket criado com acesso publico de leitura: $bucket")
        }
    }
}
