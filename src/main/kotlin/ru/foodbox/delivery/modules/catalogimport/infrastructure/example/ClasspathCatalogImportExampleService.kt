package ru.foodbox.delivery.modules.catalogimport.infrastructure.example

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.catalogimport.application.CatalogImportExampleService
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportExampleDescriptor
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportExampleFile
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportMode
import ru.foodbox.delivery.modules.catalogimport.domain.CatalogImportType

@Service
class ClasspathCatalogImportExampleService : CatalogImportExampleService {

    private val examplesByKey: Map<ExampleKey, String> = linkedMapOf(
        ExampleKey(CatalogImportType.CATEGORY, CatalogImportMode.VALIDATE_ONLY) to
            "catalog-import/examples/category_validate_only.csv",
        ExampleKey(CatalogImportType.CATEGORY, CatalogImportMode.CREATE_ONLY) to
            "catalog-import/examples/category_create_only.csv",
        ExampleKey(CatalogImportType.CATEGORY, CatalogImportMode.UPSERT) to
            "catalog-import/examples/category_upsert.csv",
        ExampleKey(CatalogImportType.PRODUCT, CatalogImportMode.VALIDATE_ONLY) to
            "catalog-import/examples/product_validate_only.csv",
        ExampleKey(CatalogImportType.PRODUCT, CatalogImportMode.CREATE_ONLY) to
            "catalog-import/examples/product_create_only.csv",
        ExampleKey(CatalogImportType.PRODUCT, CatalogImportMode.UPSERT) to
            "catalog-import/examples/product_upsert.csv",
        ExampleKey(CatalogImportType.MODIFIER_GROUP, CatalogImportMode.VALIDATE_ONLY) to
            "catalog-import/examples/modifier_group_validate_only.csv",
        ExampleKey(CatalogImportType.MODIFIER_GROUP, CatalogImportMode.CREATE_ONLY) to
            "catalog-import/examples/modifier_group_create_only.csv",
        ExampleKey(CatalogImportType.MODIFIER_GROUP, CatalogImportMode.UPSERT) to
            "catalog-import/examples/modifier_group_upsert.csv",
        ExampleKey(CatalogImportType.MODIFIER_OPTION, CatalogImportMode.VALIDATE_ONLY) to
            "catalog-import/examples/modifier_option_validate_only.csv",
        ExampleKey(CatalogImportType.MODIFIER_OPTION, CatalogImportMode.CREATE_ONLY) to
            "catalog-import/examples/modifier_option_create_only.csv",
        ExampleKey(CatalogImportType.MODIFIER_OPTION, CatalogImportMode.UPSERT) to
            "catalog-import/examples/modifier_option_upsert.csv",
        ExampleKey(CatalogImportType.PRODUCT_MODIFIER_GROUP_LINK, CatalogImportMode.VALIDATE_ONLY) to
            "catalog-import/examples/product_modifier_group_link_validate_only.csv",
        ExampleKey(CatalogImportType.PRODUCT_MODIFIER_GROUP_LINK, CatalogImportMode.CREATE_ONLY) to
            "catalog-import/examples/product_modifier_group_link_create_only.csv",
        ExampleKey(CatalogImportType.PRODUCT_MODIFIER_GROUP_LINK, CatalogImportMode.UPSERT) to
            "catalog-import/examples/product_modifier_group_link_upsert.csv",
    )

    override fun listExamples(): List<CatalogImportExampleDescriptor> {
        return examplesByKey.map { (key, resourcePath) ->
            CatalogImportExampleDescriptor(
                importType = key.importType,
                importMode = key.importMode,
                fileName = resourcePath.substringAfterLast('/'),
            )
        }
    }

    override fun getExample(importType: CatalogImportType, importMode: CatalogImportMode): CatalogImportExampleFile {
        val resourcePath = examplesByKey[ExampleKey(importType, importMode)]
            ?: throw NotFoundException("Import example not found for $importType/$importMode")

        val resource = ClassPathResource(resourcePath)
        if (!resource.exists()) {
            throw NotFoundException("Import example file not found: $resourcePath")
        }

        return CatalogImportExampleFile(
            fileName = resourcePath.substringAfterLast('/'),
            content = resource.inputStream.use { it.readBytes() },
        )
    }

    private data class ExampleKey(
        val importType: CatalogImportType,
        val importMode: CatalogImportMode,
    )
}
