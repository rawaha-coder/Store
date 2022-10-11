package com.hybcode.store

data class Currency(
    var code: String,
    var symbol: String,
// If the customer is using the base currency of the store then exchangeRate will be null
    var exchangeRate: Double?
)
