package com.x.batchapp.quartz

import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.stereotype.Component

@Component
class BatchJobLauncher(
    private val jobLauncher: JobLauncher,
    private val applicationContext: ApplicationContext
) : QuartzJobBean() {

    private val logger = LoggerFactory.getLogger(BatchJobLauncher::class.java)

    override fun executeInternal(context: JobExecutionContext) {
        val jobName = context.jobDetail.jobDataMap.getString("jobName")

        try {
            val job = applicationContext.getBean(jobName, Job::class.java)
            val jobParameters = createJobParameters()

            logger.info("Starting Quartz-triggered batch job: $jobName")
            val execution = jobLauncher.run(job, jobParameters)
            logger.info("Batch job: $jobName completed with status: ${execution.status}")

        } catch (e: Exception) {
            logger.error("Failed to execute batch job: $jobName", e)
            throw e
        }
    }

    private fun createJobParameters(): JobParameters {
        return JobParametersBuilder()
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters()
    }
}
