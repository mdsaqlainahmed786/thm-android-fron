package com.thehotelmedia.android.adapters.dropDown

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.customClasses.ColorFilterTransformation


data class Businesses(val name: String, val iconUrl: String, val id: String) {
    override fun toString(): String {
        return name
    }
}


/**
 * Modify this adapter carefully, as it is used in multiple places.
 */

class BusinessTypeAdapter(context: Context, private val business: List<Businesses>) : ArrayAdapter<Businesses>(context, 0, business) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, R.layout.dropdown_item)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, R.layout.dropdown_item)
    }

    private fun createViewFromResource(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
        resource: Int
    ): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
        val business = getItem(position)

        val imageView = view.findViewById<ImageView>(R.id.imageView)
        val textView = view.findViewById<TextView>(R.id.textView)

        if (business != null) {
            if (business.iconUrl.isNotEmpty()){
                Glide.with(context)
                    .load(business.iconUrl)
                    .placeholder(R.drawable.ic_selected_home)
                    .transform(ColorFilterTransformation(ContextCompat.getColor(context, R.color.icon_color_60)))
                    .into(imageView)

                imageView.visibility = View.VISIBLE
            }else{
                imageView.visibility = View.GONE
            }


            textView.text = business.name
        }

        return view
    }
}
