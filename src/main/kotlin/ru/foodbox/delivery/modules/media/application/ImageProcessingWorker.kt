package ru.foodbox.delivery.modules.media.application

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.media.domain.repository.ImageProcessingJobRepository
import java.time.Instant

@Component
class ImageProcessingWorker(
    private val jobRepository: ImageProcessingJobRepository,
    private val imageProcessingService: ImageProcessingService,
    private val properties: ImageProcessingProperties,
) {

    private val log = LoggerFactory.getLogger(ImageProcessingWorker::class.java)

    @Scheduled(fixedDelayString = "\${media.processing.worker-poll-interval-ms:5000}")
    fun poll() {
        val jobs = jobRepository.claimNextPending(Instant.now(), properties.workerBatchSize)
        if (jobs.isEmpty()) {
            return
        }

        log.info("Claimed {} image processing jobs", jobs.size)

        for (job in jobs) {
            try {
                imageProcessingService.processJob(job)
            } catch (ex: Exception) {
                log.error("Unhandled error processing job {}", job.id, ex)
            }
        }
    }
}
