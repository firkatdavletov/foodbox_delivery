package ru.foodbox.delivery.common.utils

import java.math.BigDecimal

object PriceFormat {
    fun transformForApi(bd: BigDecimal): Long = bd.multiply(BigDecimal(100)).longValueExact()
}