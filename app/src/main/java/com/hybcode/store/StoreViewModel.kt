package com.hybcode.store

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StoreViewModel : ViewModel() {
    fun calculateOrderTotal() {
        TODO("Not yet implemented")
    }

    var products = MutableLiveData<List<Product>>()
}