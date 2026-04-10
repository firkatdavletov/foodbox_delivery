package ru.foodbox.delivery.modules.legal.application

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.legal.domain.LegalDocument
import ru.foodbox.delivery.modules.legal.domain.LegalDocumentType
import ru.foodbox.delivery.modules.legal.domain.repository.LegalDocumentRepository
import java.time.Clock

@Component
class LegalDocumentBootstrapInitializer(
    private val legalDocumentRepository: LegalDocumentRepository,
    private val clock: Clock,
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments?) {
        val existingByType = legalDocumentRepository.findAll().associateBy(LegalDocument::type)

        LegalDocumentType.entries.forEach { type ->
            val existing = existingByType[type]
            when {
                existing == null -> legalDocumentRepository.save(defaultDocument(type))
                existing.title.isBlank() -> {
                    legalDocumentRepository.save(
                        existing.copy(
                            title = type.defaultTitle,
                            updatedAt = clock.instant(),
                        )
                    )
                }
            }
        }
    }

    private fun defaultDocument(type: LegalDocumentType): LegalDocument {
        val now = clock.instant()
        return LegalDocument(
            type = type,
            title = type.defaultTitle,
            subtitle = null,
            text = "",
            createdAt = now,
            updatedAt = now,
        )
    }
}
