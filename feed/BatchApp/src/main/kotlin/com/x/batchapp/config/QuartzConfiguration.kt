package com.x.batchapp.config

import com.x.batchapp.quartz.BatchJobLauncher
import org.quartz.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QuartzConfiguration {

    @Bean
    fun followCountSyncJobDetail(): JobDetail {
        return JobBuilder.newJob(BatchJobLauncher::class.java)
            .withIdentity("followCountSyncJob")
            .withDescription("Follow count sync batch job")
            .usingJobData("jobName", "followCountSyncJob")
            .storeDurably()
            .build()
    }

    @Bean
    fun followCountSyncTrigger(followCountSyncJobDetail: JobDetail): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(followCountSyncJobDetail)
            .withIdentity("followCountSyncTrigger")
            .withDescription("Trigger follow count sync job every hour")
            .withSchedule(
                CronScheduleBuilder.cronSchedule("0 0 * * * ?")  // Every hour at :00
            )
            .build()
    }

    @Bean
    fun newsFeedCleanupJobDetail(): JobDetail {
        return JobBuilder.newJob(BatchJobLauncher::class.java)
            .withIdentity("newsFeedCleanupJob")
            .withDescription("News feed cache cleanup batch job")
            .usingJobData("jobName", "newsFeedCleanupJob")
            .storeDurably()
            .build()
    }

    @Bean
    fun newsFeedCleanupTrigger(newsFeedCleanupJobDetail: JobDetail): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(newsFeedCleanupJobDetail)
            .withIdentity("newsFeedCleanupTrigger")
            .withDescription("Trigger news feed cleanup job daily at midnight")
            .withSchedule(
                CronScheduleBuilder.cronSchedule("0 0 0 * * ?")  // Daily at midnight
            )
            .build()
    }
}
