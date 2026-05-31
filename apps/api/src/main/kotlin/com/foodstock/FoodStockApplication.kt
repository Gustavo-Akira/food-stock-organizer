package com.foodstock

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FoodStockApplication

fun main(args: Array<String>) {
    runApplication<FoodStockApplication>(*args)
}
