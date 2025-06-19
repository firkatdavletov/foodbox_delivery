package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.data.entities.AddressEntity

interface AddressRepository : JpaRepository<AddressEntity, Long>