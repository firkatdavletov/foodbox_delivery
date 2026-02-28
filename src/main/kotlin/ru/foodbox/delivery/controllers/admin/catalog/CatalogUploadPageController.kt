package ru.foodbox.delivery.controllers.admin.catalog

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import ru.foodbox.delivery.controllers.admin.body.ImportFileResponseBody
import ru.foodbox.delivery.controllers.admin.body.SaveCategoryRequestBody
import ru.foodbox.delivery.controllers.admin.body.SaveCategoryResponseBody
import ru.foodbox.delivery.controllers.admin.body.SaveProductRequestBody
import ru.foodbox.delivery.controllers.admin.body.SaveProductResponseBody
import ru.foodbox.delivery.controllers.catalog.body.DeleteCategoryResponseBody
import ru.foodbox.delivery.controllers.catalog.body.DeleteProductResponseBody
import ru.foodbox.delivery.services.CatalogService
import ru.foodbox.delivery.services.ImportCsvService

@Controller
@RequestMapping("/admin/catalog")
class CatalogUploadPageController(
    private val catalogService: CatalogService,
    private val importCsvService: ImportCsvService,
) {

    @PostMapping("/import")
    fun importFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam mode: String,
        @RequestParam(defaultValue = "insert") importMode: String,
    ): ResponseEntity<ImportFileResponseBody> {

        if (file.isEmpty) {
            return ResponseEntity.ok(ImportFileResponseBody(false, "Файл пустой", 400))
        }

        val response = when (mode) {
            "products" -> importCsvService.importCsvProducts(file, importMode)
            "categories" -> importCsvService.importCsvCategories(file, importMode)
            else -> ImportFileResponseBody(false, "Неизвестный режим: $mode", 400)
        }
        return ResponseEntity.ok(response)
    }

    @PostMapping("/category")
    fun saveCategory(@RequestBody body: SaveCategoryRequestBody): ResponseEntity<SaveCategoryResponseBody> {
        val response = catalogService.saveCategory(body.category)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/product")
    fun saveProduct(@RequestBody body: SaveProductRequestBody): ResponseEntity<SaveProductResponseBody> {
        val response = catalogService.saveProduct(body.product)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/category")
    fun deleteCategory(@RequestParam categoryId: Long): ResponseEntity<DeleteCategoryResponseBody> {
        catalogService.deleteCategory(categoryId)
        return ResponseEntity.ok(DeleteCategoryResponseBody())
    }

    @DeleteMapping("/product")
    fun deleteProduct(@RequestParam id: Long): ResponseEntity<DeleteProductResponseBody> {
        catalogService.deleteProduct(id)
        return ResponseEntity.ok(DeleteProductResponseBody())
    }
}
