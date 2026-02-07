package ru.foodbox.delivery.services.dto

import com.opencsv.bean.CsvBindByName
import ru.foodbox.delivery.data.entities.CategoryEntity
import java.math.BigDecimal

data class ProductCsvDto(

    @CsvBindByName(column = "name")
    var name: String? = null,

    @CsvBindByName(column = "description")
    var description: String? = null,

    @CsvBindByName(column = "price")
    var price: BigDecimal? = null,

    @CsvBindByName(column = "image_url")
    var imageUrl: String? = null,

    @CsvBindByName(column = "category")
    var category: String? = null
)

