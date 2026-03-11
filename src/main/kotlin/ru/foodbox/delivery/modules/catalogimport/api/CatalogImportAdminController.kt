package ru.foodbox.delivery.modules.catalogimport.api

import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import ru.foodbox.delivery.modules.catalogimport.api.dto.CatalogImportExampleResponse
import ru.foodbox.delivery.modules.catalogimport.application.CatalogImportExampleService
import ru.foodbox.delivery.modules.catalogimport.api.dto.CatalogImportResponse
import ru.foodbox.delivery.modules.catalogimport.application.CatalogImportService
import ru.foodbox.delivery.modules.catalogimport.application.command.ExecuteCatalogImportCommand
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType

@RestController
@RequestMapping(path = ["/api/v1/admin/catalog-import", "/api/admin/catalog-import"])
class CatalogImportAdminController(
    private val catalogImportService: CatalogImportService,
    private val catalogImportExampleService: CatalogImportExampleService,
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importCatalog(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("importType") importType: CatalogImportType,
        @RequestParam("importMode") importMode: CatalogImportMode,
    ): CatalogImportResponse {
        val report = catalogImportService.execute(
            ExecuteCatalogImportCommand(
                importType = importType,
                importMode = importMode,
                csvBytes = file.bytes,
            )
        )
        return report.toResponse()
    }

    @GetMapping("/examples")
    fun listExamples(): List<CatalogImportExampleResponse> {
        return catalogImportExampleService.listExamples().map { it.toResponse() }
    }

    @GetMapping("/examples/{importType}/{importMode}", produces = ["text/csv"])
    fun downloadExample(
        @PathVariable importType: CatalogImportType,
        @PathVariable importMode: CatalogImportMode,
    ): ResponseEntity<ByteArrayResource> {
        val example = catalogImportExampleService.getExample(importType, importMode)
        return ResponseEntity.ok()
            .contentType(MediaType("text", "csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${example.fileName}\"")
            .contentLength(example.content.size.toLong())
            .body(ByteArrayResource(example.content))
    }
}
