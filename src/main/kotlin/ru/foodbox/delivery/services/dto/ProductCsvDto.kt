package ru.foodbox.delivery.services.dto

import com.opencsv.bean.CsvBindByName
import ru.foodbox.delivery.data.entities.CategoryEntity
import ru.foodbox.delivery.services.model.UnitOfMeasure
import java.math.BigDecimal

data class ProductCsvDto(

    @CsvBindByName(column = "name")
    var title: String? = null,

    @CsvBindByName(column = "description")
    var description: String? = null,

    @CsvBindByName(column = "price")
    var price: Long? = null,

    @CsvBindByName(column = "image_url")
    var imageUrl: String? = null,

    @CsvBindByName(column = "category")
    var categoryId: Long? = null,

    @CsvBindByName(column = "unit")
    val unit: UnitOfMeasure? = null,

    @CsvBindByName(column = "weight")
    val displayWeight: String? = null,

    @CsvBindByName(column = "step")
    val countStep: Int? = null,
)

