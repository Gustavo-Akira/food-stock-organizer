package com.foodstock.shopping.config

import com.foodstock.shopping.adapter.out.InventoryRestockAdapter
import com.foodstock.shopping.adapter.out.InventoryRunningOutAdapter
import com.foodstock.shopping.adapter.out.MemberRoleAdapter
import com.foodstock.shopping.adapter.out.ShoppingListJpaRepository
import com.foodstock.shopping.domain.service.ShoppingListService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ShoppingConfig(
    private val shoppingListJpaRepository: ShoppingListJpaRepository,
    private val inventoryRunningOutAdapter: InventoryRunningOutAdapter,
    private val memberRoleAdapter: MemberRoleAdapter,
    private val inventoryRestockAdapter: InventoryRestockAdapter,
    private val clock: Clock
) {
    @Bean
    fun shoppingListService(): ShoppingListService = ShoppingListService(
        shoppingListRepository = shoppingListJpaRepository,
        runningOutItemsQueryPort = inventoryRunningOutAdapter,
        memberRolePort = memberRoleAdapter,
        restockItemsPort = inventoryRestockAdapter,
        clock = clock
    )
}
