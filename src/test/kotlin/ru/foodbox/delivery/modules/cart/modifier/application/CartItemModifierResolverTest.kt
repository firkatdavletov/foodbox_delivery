package ru.foodbox.delivery.modules.cart.modifier.application

import org.junit.jupiter.api.Test
import ru.foodbox.delivery.modules.catalog.modifier.application.CatalogProductModifiersService
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierApplicationScope
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierGroup
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierOption
import ru.foodbox.delivery.modules.catalog.modifier.domain.ModifierPriceType
import ru.foodbox.delivery.modules.catalog.modifier.domain.ProductModifierGroup
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierGroupRepository
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ModifierOptionRepository
import ru.foodbox.delivery.modules.catalog.modifier.domain.repository.ProductModifierGroupRepository
import ru.foodbox.delivery.modules.cart.application.command.AddCartItemModifierCommand
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CartItemModifierResolverTest {

    @Test
    fun `resolves modifier snapshots for product`() {
        val productId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val resolver = CartItemModifierResolver(
            catalogProductModifiersService = CatalogProductModifiersService(
                productModifierGroupRepository = FakeProductModifierGroupRepository(
                    links = listOf(
                        ProductModifierGroup(
                            id = UUID.randomUUID(),
                            productId = productId,
                            modifierGroupId = groupId,
                            sortOrder = 10,
                            isActive = true,
                        )
                    ),
                ),
                modifierGroupRepository = FakeModifierGroupRepository(
                    groups = listOf(
                        ModifierGroup(
                            id = groupId,
                            code = "syrup",
                            name = "Syrup",
                            minSelected = 0,
                            maxSelected = 2,
                            isRequired = false,
                            isActive = true,
                            sortOrder = 10,
                        )
                    ),
                ),
                modifierOptionRepository = FakeModifierOptionRepository(
                    options = listOf(
                        ModifierOption(
                            id = optionId,
                            groupId = groupId,
                            code = "vanilla",
                            name = "Vanilla",
                            description = "Vanilla syrup",
                            priceType = ModifierPriceType.FIXED,
                            price = 150,
                            applicationScope = ModifierApplicationScope.PER_ITEM,
                            isDefault = false,
                            isActive = true,
                            sortOrder = 0,
                        )
                    ),
                ),
            ),
        )

        val resolved = resolver.resolve(
            productId = productId,
            commands = listOf(
                AddCartItemModifierCommand(
                    modifierGroupId = groupId,
                    modifierOptionId = optionId,
                    quantity = 2,
                )
            ),
        )

        assertEquals(1, resolved.size)
        assertEquals(groupId, resolved.single().modifierGroupId)
        assertEquals(optionId, resolved.single().modifierOptionId)
        assertEquals("syrup", resolved.single().groupCodeSnapshot)
        assertEquals("vanilla", resolved.single().optionCodeSnapshot)
        assertEquals(150L, resolved.single().priceSnapshot)
        assertEquals(2, resolved.single().quantity)
    }

    @Test
    fun `rejects missing required modifier group`() {
        val productId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        val resolver = CartItemModifierResolver(
            catalogProductModifiersService = CatalogProductModifiersService(
                productModifierGroupRepository = FakeProductModifierGroupRepository(
                    links = listOf(
                        ProductModifierGroup(
                            id = UUID.randomUUID(),
                            productId = productId,
                            modifierGroupId = groupId,
                            sortOrder = 0,
                            isActive = true,
                        )
                    ),
                ),
                modifierGroupRepository = FakeModifierGroupRepository(
                    groups = listOf(
                        ModifierGroup(
                            id = groupId,
                            code = "card",
                            name = "Card",
                            minSelected = 1,
                            maxSelected = 1,
                            isRequired = true,
                            isActive = true,
                            sortOrder = 0,
                        )
                    ),
                ),
                modifierOptionRepository = FakeModifierOptionRepository(
                    options = listOf(
                        ModifierOption(
                            id = UUID.randomUUID(),
                            groupId = groupId,
                            code = "birthday",
                            name = "Birthday card",
                            description = null,
                            priceType = ModifierPriceType.FREE,
                            price = 0,
                            applicationScope = ModifierApplicationScope.PER_LINE,
                            isDefault = false,
                            isActive = true,
                            sortOrder = 0,
                        )
                    ),
                ),
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            resolver.resolve(productId = productId, commands = emptyList())
        }
    }

    private class FakeProductModifierGroupRepository(
        private val links: List<ProductModifierGroup>,
    ) : ProductModifierGroupRepository {
        override fun findAllByProductId(productId: UUID): List<ProductModifierGroup> {
            return links.filter { it.productId == productId }
        }

        override fun findAllByProductIds(productIds: Collection<UUID>): List<ProductModifierGroup> {
            return links.filter { it.productId in productIds }
        }

        override fun deleteAllByProductId(productId: UUID) = error("Not used")

        override fun saveAll(productModifierGroups: List<ProductModifierGroup>): List<ProductModifierGroup> = error("Not used")
    }

    private class FakeModifierGroupRepository(
        private val groups: List<ModifierGroup>,
    ) : ModifierGroupRepository {
        override fun findAll(): List<ModifierGroup> = groups
        override fun findAllByIsActive(isActive: Boolean): List<ModifierGroup> = groups.filter { it.isActive == isActive }
        override fun findAllByCodes(codes: Collection<String>): List<ModifierGroup> = groups.filter { it.code in codes }
        override fun findAllByIds(ids: Collection<UUID>): List<ModifierGroup> = groups.filter { it.id in ids }
        override fun findById(id: UUID): ModifierGroup? = groups.firstOrNull { it.id == id }
        override fun findByCode(code: String): ModifierGroup? = groups.firstOrNull { it.code == code }
        override fun save(group: ModifierGroup): ModifierGroup = error("Not used")
    }

    private class FakeModifierOptionRepository(
        private val options: List<ModifierOption>,
    ) : ModifierOptionRepository {
        override fun findAllByGroupIds(groupIds: Collection<UUID>): List<ModifierOption> {
            return options.filter { it.groupId in groupIds }
        }

        override fun deleteAllByGroupId(groupId: UUID) = error("Not used")

        override fun saveAll(options: List<ModifierOption>): List<ModifierOption> = error("Not used")
    }
}
