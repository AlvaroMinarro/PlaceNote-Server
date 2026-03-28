package com.placenote.server.db

import com.placenote.server.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    lateinit var dataSource: HikariDataSource
        private set

    @Volatile
    private var initialized = false

    fun init(config: DatabaseConfig) {
        synchronized(this) {
            if (initialized) return
            dataSource = createDataSource(config)
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .validateMigrationNaming(true)
                .load()
                .migrate()
            Database.connect(dataSource)
            initialized = true
        }
    }

    private fun createDataSource(config: DatabaseConfig): HikariDataSource {
        val hc = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            username = config.username
            password = config.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            validate()
        }
        return HikariDataSource(hc)
    }
}
