package com.x.feedapp.feed.service

import com.x.feedapp.feed.controller.dto.CreateFeedRequest
import com.x.feedapp.feed.controller.dto.UpdateFeedRequest
import com.x.feedapp.feed.domain.Feed
import com.x.feedapp.feed.domain.toFeedByUser
import com.x.feedapp.feed.repository.FeedByUserRepository
import com.x.feedapp.feed.repository.FeedDBRepository
import com.x.feedapp.feed.repository.FeedRedisRepository
import com.x.feedapp.feed.repository.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.cassandra.core.query.CassandraPageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.RuntimeException
import java.time.Instant

@Service
class FeedService(
    private val feedDBRepository: FeedDBRepository,
    private val feedByUserRepository: FeedByUserRepository,
    private val feedRedisRepository: FeedRedisRepository,
    private val idGenerationService: IdGenerationService,
    private val kafkaProducer: KafkaProducer
) {

    private val logger: Logger? = LoggerFactory.getLogger(FeedService::class.java)

    fun createFeed(createFeedRequest: CreateFeedRequest, username: String): Mono<Feed> {
        val content = createFeedRequest.content
        if (content.isEmpty() || content.length > 300) {
            return Mono.error(RuntimeException("Content length must be between 1 and 300 characters"))
        }

        return idGenerationService.generateId()
            .map { feedId -> Feed(feedId = feedId, username = username, content = content).apply { markAsNew() } }
            .flatMap { feed ->
                feedDBRepository.save(feed)
                    .flatMap { savedFeed ->
                        feedByUserRepository.save(toFeedByUser(savedFeed).apply { markAsNew() })
                            .doOnNext { feedByUser ->
                                kafkaProducer.sendFeedCreationMessage("$username ${feedByUser.id}")
                            }.doOnNext {
                                feedRedisRepository.addFeedToCache(username, savedFeed.feedId)
                            }
                            .thenReturn(savedFeed)
                    }
            }
    }

    fun updateFeed(feedId: String, updateFeedRequest: UpdateFeedRequest, username: String): Mono<Feed> {
        return feedDBRepository.existsById(feedId)
            .flatMap { exists ->
                if (exists) {
                    val feed = Feed(
                        feedId = feedId,
                        username = username,
                        content = updateFeedRequest.content
                    )
                    feedDBRepository.save(feed)
                } else {
                    Mono.error(RuntimeException("Feed not found"))
                }
            }
    }

    fun deleteFeed(id: String, username: String): Mono<Boolean> {
        return feedDBRepository.findById(id)
            .flatMap { feed ->
                if (feed.username == username) {
                    feedDBRepository.deleteByFeedId(id)
                        .thenReturn(true)
                } else {
                    Mono.just(false)
                }
            }.doOnSuccess { result ->
                if (result) {
                    feedRedisRepository.removeFeedFromCache(username, id)
                }
            }
    }

    fun getFeed(id: String): Mono<Feed> {
        return feedDBRepository.findById(id)
    }

    fun getFeedsByUser(username: String, pageRequest: CassandraPageRequest): Mono<Slice<Feed>> {
        return feedByUserRepository.findAllByKeyUsername(username, pageRequest)
            .flatMap { slice ->
                Flux.fromIterable(slice.content)
                    .flatMap { feedByUser -> feedDBRepository.findById(feedByUser.key.feedId) }
                    .collectList()
                    .map { feeds -> SliceImpl(feeds, slice.pageable, slice.hasNext()) }
            }
    }

    fun findMyFeeds(pageRequest: CassandraPageRequest): Mono<Slice<Feed>> {
        return ReactiveSecurityContextHolder.getContext()
            .map { context -> context.authentication.principal as String }
            .flatMap { username -> getFeedsByUser(username, pageRequest) }
    }

    fun getFeedIdsBetweenDates(username: String, start: Instant, end: Instant): Flux<String> {
        val fromId = idGenerationService.generateMinSnowflakeId(start)
        val toId = idGenerationService.generateMaxSnowflakeId(end)
        return Mono.zip(fromId, toId)
            .flatMapMany { tuple ->
                feedByUserRepository
                    .findByKeyUsernameAndKeyFeedIdBetween(username, tuple.t1, tuple.t2)
            }
            .map { it.key.feedId }
    }
}
