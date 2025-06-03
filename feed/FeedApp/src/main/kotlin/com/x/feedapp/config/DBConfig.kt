package com.x.feedapp.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.config.AbstractReactiveCassandraConfiguration
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories

@Configuration
@EnableReactiveCassandraRepositories
class DBConfig : AbstractReactiveCassandraConfiguration() {
    override fun getKeyspaceName(): String {
        return "x"
    }
}
