package com.rappiclone.infra.config

import com.typesafe.config.ConfigFactory

/**
 * Configuracao tipada do servico via HOCON + env vars.
 * Arquivo application.conf no resources de cada servico, com overlay de env vars.
 */
data class ServiceConfig(
    val name: String,
    val port: Int,
    val database: DatabaseConfig?,
    val redis: RedisConfig?,
    val kafka: KafkaConfig?,
    val mqtt: MqttConfig?
)

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int = 10
)

data class RedisConfig(
    val url: String
)

data class KafkaConfig(
    val bootstrapServers: String,
    val groupId: String
)

data class MqttConfig(
    val host: String,
    val port: Int,
    val clientIdPrefix: String
)

fun loadServiceConfig(): ServiceConfig {
    val config = ConfigFactory.load()
    val svc = config.getConfig("service")

    return ServiceConfig(
        name = svc.getString("name"),
        port = svc.getInt("port"),
        database = if (config.hasPath("database")) {
            val db = config.getConfig("database")
            DatabaseConfig(
                url = db.getString("url"),
                user = db.getString("user"),
                password = db.getString("password"),
                maxPoolSize = if (db.hasPath("maxPoolSize")) db.getInt("maxPoolSize") else 10
            )
        } else null,
        redis = if (config.hasPath("redis")) {
            RedisConfig(url = config.getString("redis.url"))
        } else null,
        kafka = if (config.hasPath("kafka")) {
            val k = config.getConfig("kafka")
            KafkaConfig(
                bootstrapServers = k.getString("bootstrapServers"),
                groupId = k.getString("groupId")
            )
        } else null,
        mqtt = if (config.hasPath("mqtt")) {
            val m = config.getConfig("mqtt")
            MqttConfig(
                host = m.getString("host"),
                port = m.getInt("port"),
                clientIdPrefix = m.getString("clientIdPrefix")
            )
        } else null
    )
}
