package com.x.feedapp.user.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Transient
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import org.springframework.data.domain.Persistable
import java.time.Instant

@Table("user")
data class User(
    @field:PrimaryKey
    val username: String,
    val password: String,
    @field:Column("created_at")
    @field:CreatedDate
    var createdAt: Instant? = null,
    @field:Transient
    var isNewUser: Boolean = false
) : Persistable<String> {

    override fun getId(): String = username

    override fun isNew(): Boolean = isNewUser
}
