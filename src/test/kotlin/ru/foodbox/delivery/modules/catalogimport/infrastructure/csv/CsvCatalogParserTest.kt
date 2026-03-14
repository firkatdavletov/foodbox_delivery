package ru.foodbox.delivery.modules.catalogimport.infrastructure.csv

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CsvCatalogParserTest {

    private val parser = CsvCatalogParser()

    @Test
    fun `normalizes russian product headers with required optional marks`() {
        val csv = """
            Название товара (обязательное),Внешний ID категории (обязательное),SKU варианта (обязательное для варианта),Товар активен (необязательное),Порядок сортировки товара (необязательное),Код группы опции 1 (необязательное),Название группы опции 1 (необязательное),Код значения опции 1 (необязательное),Название значения опции 1 (необязательное)
            Футболка,cat-clothes,TSHIRT-BLACK-S,true,10,color,Цвет,black,Черный
        """.trimIndent()

        val row = parser.parse(csv.toByteArray()).single()

        assertEquals("Футболка", row.get("product_title"))
        assertEquals("cat-clothes", row.get("category_external_id"))
        assertEquals("TSHIRT-BLACK-S", row.get("variant_sku"))
        assertEquals("true", row.get("product_is_active"))
        assertEquals("10", row.get("product_sort_order"))
        assertEquals("color", row.get("option1_group_code"))
        assertEquals("Цвет", row.get("option1_group_title"))
        assertEquals("black", row.get("option1_value_code"))
        assertEquals("Черный", row.get("option1_value_title"))
    }

    @Test
    fun `keeps technical headers available`() {
        val csv = """
            product_external_id,product_title,category_external_id
            prd-1,Товар,cat-1
        """.trimIndent()

        val row = parser.parse(csv.toByteArray()).single()

        assertEquals("prd-1", row.get("product_external_id"))
        assertEquals("Товар", row.get("product_title"))
        assertEquals("cat-1", row.get("category_external_id"))
    }

    @Test
    fun `normalizes russian category headers`() {
        val csv = """
            Внешний ID категории в каталоге (обязательное),Название категории (обязательное),Слаг категории (обязательное),Внешний ID родительской категории (необязательное),Описание категории (необязательное),Категория активна (необязательное),Порядок сортировки категории (необязательное)
            cat-fruits,Фрукты,fruits,cat-root,Раздел фруктов,true,20
        """.trimIndent()

        val row = parser.parse(csv.toByteArray()).single()

        assertEquals("cat-fruits", row.get("external_id"))
        assertEquals("Фрукты", row.get("name"))
        assertEquals("fruits", row.get("slug"))
        assertEquals("cat-root", row.get("parent_external_id"))
        assertEquals("Раздел фруктов", row.get("description"))
        assertEquals("true", row.get("is_active"))
        assertEquals("20", row.get("sort_order"))
    }
}
