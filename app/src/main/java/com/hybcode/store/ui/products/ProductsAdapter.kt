package com.hybcode.store.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hybcode.store.MainActivity
import com.hybcode.store.Product
import com.hybcode.store.R

class ProductsAdapter(private val activity: MainActivity, private val fragment: ProductsFragment) :
    RecyclerView.Adapter<ProductsAdapter.ProductsViewHolder>() {
    var products = mutableListOf<Product>()
    inner class ProductsViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        internal var mProductImage = itemView.findViewById<View>(R.id.productImage) as ImageView
        internal var mProductName = itemView.findViewById<View>(R.id.productName) as TextView
        internal var mProductPrice = itemView.findViewById<View>(R.id.productPrice) as TextView
        internal var mAddToBasketButton = itemView.findViewById<View>(R.id.addToBasketButton) as Button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductsViewHolder {
        return ProductsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.product, parent, false))
    }

    override fun onBindViewHolder(holder: ProductsViewHolder, position: Int) {
        val current = products[position]
        Glide.with(activity)
            .load(current.image)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop()
            .override(600, 600)
            .into(holder.mProductImage)
        holder.mProductName.text = current.name
// TODO: Set the price of the product here
        if (current.inCart) {
            holder.mAddToBasketButton.text = activity.resources.getString(R.string.remove_from_basket)
            holder.mAddToBasketButton.setBackgroundColor(
                ContextCompat.getColor(activity,
                android.R.color.holo_red_dark))
        } else {
            holder.mAddToBasketButton.text = activity.resources.getString(R.string.add_to_basket)
            holder.mAddToBasketButton.setBackgroundColor(ContextCompat.getColor(activity,
                android.R.color.holo_green_dark))
        }
        holder.mAddToBasketButton.setOnClickListener {
                    fragment.updateCart(position)
                }
    }

    override fun getItemCount() = products.size
}