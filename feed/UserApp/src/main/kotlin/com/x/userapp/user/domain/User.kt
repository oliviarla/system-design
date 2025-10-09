package com.x.userapp.user.domain

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
    @field:Column("following_count")
    var followingCount: Int = 0,
    @field:Column("follower_count")
    var followerCount: Int = 0,
) : Persistable<String> {

    @Transient
    private var isNewUser: Boolean = false

    override fun getId(): String = username

    override fun isNew(): Boolean = isNewUser

    fun markAsNew() {
        isNewUser = true
    }
}
