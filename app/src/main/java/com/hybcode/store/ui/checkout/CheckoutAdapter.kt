package com.hybcode.store.ui.checkout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hybcode.store.Currency
import com.hybcode.store.MainActivity
import com.hybcode.store.Product
import com.hybcode.store.R

class CheckoutAdapter(private val activity: MainActivity, private val fragment: CheckoutFragment) :
    RecyclerView.Adapter<CheckoutAdapter.ProductsViewHolder>() {

    var products = mutableListOf<Product>()
    var currency: Currency? = null

    inner class ProductsViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        internal var mProductImage = itemView.findViewById<View>(R.id.productImage) as ImageView
        internal var mProductName = itemView.findViewById<View>(R.id.productName) as TextView
        internal var mProductPrice = itemView.findViewById<View>(R.id.productPrice) as TextView
        internal var mRemoveFromBasketButton = itemView.findViewById<View>(R.id.removeFromBasketButton)
                as ImageButton
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductsViewHolder {
        return ProductsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.basket_product, parent, false))
    }

    override fun onBindViewHolder(holder: ProductsViewHolder, position: Int) {
        val current = products[position]
        Glide.with(activity)
            .load(current.image)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop()
            .override(400, 400).into(holder.mProductImage)
        holder.mProductName.text = current.name
        val price = if (currency?.exchangeRate == null) current.price
        else current.price * currency?.exchangeRate!!
        holder.mProductPrice.text = activity.resources.getString(R.string.product_price, currency?.symbol,
            String.format("%.2f", price))
        holder.mRemoveFromBasketButton.setOnClickListener {
            fragment.removeProduct(current)
        }
    }

    override fun getItemCount() = products.size
}