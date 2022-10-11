package com.hybcode.store.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.hybcode.store.MainActivity
import com.hybcode.store.StoreViewModel
import com.hybcode.store.databinding.FragmentProductsBinding

class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private lateinit var callingActivity: MainActivity

    private val storeViewModel: StoreViewModel by activityViewModels()
    private lateinit var productsAdapter: ProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        productsAdapter = ProductsAdapter(callingActivity, this)
        binding.productsRecyclerView.layoutManager = LinearLayoutManager(activity)
        binding.productsRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.productsRecyclerView.adapter = productsAdapter

        productsAdapter.products = storeViewModel.products.value?.toMutableList() ?: mutableListOf()
        productsAdapter.notifyItemRangeInserted(0, productsAdapter.products.size)
    }

    fun updateCart(index: Int) {
        val products = productsAdapter.products.toMutableList()
        products[index].inCart = !products[index].inCart
        productsAdapter.products = products
// Call notifyItemChanged to update the add to basket button for that product
        productsAdapter.notifyItemChanged(index)
        storeViewModel.products.value = products
        storeViewModel.calculateOrderTotal()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}