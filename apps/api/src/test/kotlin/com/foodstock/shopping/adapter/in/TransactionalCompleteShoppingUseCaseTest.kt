package com.foodstock.shopping.adapter.`in`

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListStatus
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingCommand
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionOperations
import java.time.LocalDateTime
import java.util.UUID

class TransactionalCompleteShoppingUseCaseTest {

    @Test
    fun `runs complete use case inside a transaction`() {
        val expected = ShoppingList(
            id = UUID.randomUUID(),
            houseId = UUID.randomUUID(),
            name = "Weekly Shop",
            status = ShoppingListStatus.COMPLETED,
            createdBy = UUID.randomUUID(),
            createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
            updatedAt = LocalDateTime.parse("2026-01-01T00:00:00")
        )
        val command = CompleteShoppingCommand(
            listId = expected.id,
            userId = expected.createdBy,
            listVersion = expected.version
        )
        var transactionActive = false
        var delegateCalledInsideTransaction = false
        val transactionOperations: TransactionOperations = mock()
        val transactionStatus: TransactionStatus = mock()
        val delegate = object : CompleteShoppingUseCase {
            override fun complete(command: CompleteShoppingCommand): ShoppingList {
                delegateCalledInsideTransaction = transactionActive
                return expected
            }
        }
        whenever(transactionOperations.execute(any<TransactionCallback<ShoppingList>>())).thenAnswer { invocation ->
            val callback = invocation.getArgument<TransactionCallback<ShoppingList>>(0)
            transactionActive = true
            try {
                callback.doInTransaction(transactionStatus)
            } finally {
                transactionActive = false
            }
        }

        val result = TransactionalCompleteShoppingUseCase(delegate, transactionOperations).complete(command)

        assertEquals(expected, result)
        assertTrue(delegateCalledInsideTransaction)
    }
}
