package com.foodstock.shopping.adapter.`in`

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingCommand
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingUseCase
import org.springframework.transaction.support.TransactionOperations

class TransactionalCompleteShoppingUseCase(
    private val delegate: CompleteShoppingUseCase,
    private val transactionOperations: TransactionOperations
) : CompleteShoppingUseCase {

    override fun complete(command: CompleteShoppingCommand): ShoppingList =
        transactionOperations.execute { delegate.complete(command) }
            ?: error("Complete shopping transaction finished without a result")
}
