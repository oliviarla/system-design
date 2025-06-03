package com.x.feedapp.user.repository

import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface UserRepository: ReactiveCrudRepository<User, Long> {
    // Additional query methods can be defined here if needed
}