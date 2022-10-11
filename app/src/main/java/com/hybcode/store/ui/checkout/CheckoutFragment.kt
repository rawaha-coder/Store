package com.hybcode.store.ui.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.hybcode.store.*
import com.hybcode.store.databinding.FragmentCheckoutBinding

class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!
    private val storeViewModel: StoreViewModel by activityViewModels()
    private lateinit var callingActivity: MainActivity
    private lateinit var checkoutAdapter: CheckoutAdapter
    private var amount: Double? = null
    private var currency: Currency? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        callingActivity = activity as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkoutAdapter = CheckoutAdapter(callingActivity, this)
        binding.cartRecyclerView.layoutManager = LinearLayoutManager(activity)
        binding.cartRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.cartRecyclerView.adapter = checkoutAdapter

        storeViewModel.products.observe(viewLifecycleOwner) { products ->
            products?.let {
                val basket = it.filter { p ->
                    p.inCart
                }
                if (basket.isEmpty()) binding.emptyCart.visibility = View.VISIBLE
                else binding.emptyCart.visibility = View.GONE
                val adapterBasket = checkoutAdapter.products
                when {
                    basket.size > adapterBasket.size -> {
                        val newProducts = basket - adapterBasket
                        for (p in newProducts) {
                            checkoutAdapter.products.add(p)
                            checkoutAdapter.notifyItemInserted(checkoutAdapter.products.size - 1)
                        }
                    }
                    basket.size < adapterBasket.size -> {
                        val removedProducts = adapterBasket - basket
                        for (p in removedProducts) {
                            val index = checkoutAdapter.products.indexOf(p)
                            checkoutAdapter.products.removeAt(index)
                            checkoutAdapter.notifyItemRemoved(index)
                        }
                    }
                    adapterBasket.isEmpty() && basket.isNotEmpty() -> {
                        checkoutAdapter.products = basket.toMutableList()
                        checkoutAdapter.notifyItemRangeInserted(0, basket.size)
                    }
                    basket.isEmpty() -> {
                        checkoutAdapter.notifyItemRangeRemoved(0, basket.size)
                        checkoutAdapter.products = mutableListOf()
                    }
                }
                updateOrderTotal()
            }
        }

        storeViewModel.orderTotal.observe(viewLifecycleOwner) {
            amount = it
            updateOrderTotal()
        }

        storeViewModel.currency.observe(viewLifecycleOwner) { c ->
            c?.let {
                currency = it
// Detect whether the selected currency different than the currency currently being used
                if (checkoutAdapter.currency == null || it.symbol != checkoutAdapter.currency?.symbol) {
                    checkoutAdapter.currency = it
                    checkoutAdapter.notifyItemRangeChanged(0, checkoutAdapter.itemCount)
                }
                updateOrderTotal()
            }
        }

        binding.payButton.setOnClickListener {
            callingActivity.initiatePayment()
        }
    }

    fun removeProduct(product: Product) {
        product.inCart = !product.inCart
        val products = storeViewModel.products.value?.toMutableList() ?: mutableListOf()
        val position = products.indexOf(product)
        if (position != -1) {
            products[position] = product
            storeViewModel.products.value = products
            storeViewModel.calculateOrderTotal()
        }
    }

    private fun updateOrderTotal() {
        if (currency == null || amount == null) return
        val total = currency!!.symbol + String.format("%.2f", amount)
        binding.orderTotal.text = resources.getString(R.string.order_total, total)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}