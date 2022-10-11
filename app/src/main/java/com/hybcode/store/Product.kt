package com.hybcode.store

data class Product(
// In this app the images used are saved within the app as drawable resources.
// In a live app you may instead prefer to store a link (as a String) to an image stored online (e.g. on your website server)
var image: Int,
var name: String,
// Note the price of each product should be input in the base currency of the store
var price: Double,
var inCart: Boolean = false
)
