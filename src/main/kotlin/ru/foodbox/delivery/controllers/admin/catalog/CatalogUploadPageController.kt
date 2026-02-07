package ru.foodbox.delivery.controllers.admin.catalog

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import ru.foodbox.delivery.services.CatalogService
import ru.foodbox.delivery.services.CsvParserService

@Controller
@RequestMapping("/admin/catalog")
class CatalogUploadPageController(
    private val catalogService: CatalogService,
    private val csvParserService: CsvParserService,
) {

    @GetMapping("/upload")
    fun uploadPage(): String {
        return "upload" // имя HTML-файла без расширения
    }

    @PostMapping("/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): String {

        if (file.isEmpty) {
            return "error"
        }

        val csvs = csvParserService.parseCsv(file)
        catalogService.insertCatalogFromCsv(csvs.products)

        return "success"
    }
}