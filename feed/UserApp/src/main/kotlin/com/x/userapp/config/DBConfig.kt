package com.x.userapp.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.config.AbstractReactiveCassandraConfiguration
import org.springframework.data.cassandra.config.EnableReactiveCassandraAuditing
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories

@Configuration
@EnableReactiveCassandraRepositories
@EnableReactiveCassandraAuditing
class DBConfig : AbstractReactiveCassandraConfiguration() {
    override fun getKeyspaceName(): String {
        return "feed_app"
    }
}
