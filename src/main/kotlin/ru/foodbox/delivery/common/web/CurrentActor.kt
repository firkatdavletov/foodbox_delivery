package ru.foodbox.delivery.common.web

sealed interface CurrentActor {
    fun ownerType(): String
    fun ownerValue(): String

    data class User(
        val userId: Long,
    ) : CurrentActor {
        override fun ownerType(): String = "USER"
        override fun ownerValue(): String = userId.toString()
    }

    data class Guest(
        val installId: String
    ) : CurrentActor {
        override fun ownerType(): String = "INSTALLATION"
        override fun ownerValue(): String = installId
    }
}