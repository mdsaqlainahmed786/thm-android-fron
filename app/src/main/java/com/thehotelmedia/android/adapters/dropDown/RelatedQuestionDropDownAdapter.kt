package com.thehotelmedia.android.adapters.dropDown

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.thehotelmedia.android.R


class RelatedQuestionDropDownAdapter(context: Context, private val relatedQuestions: ArrayList<String>) :
    ArrayAdapter<String>(context, 0, relatedQuestions) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, R.layout.question_dropdown_item)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, R.layout.question_dropdown_item)
    }

    private fun createViewFromResource(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
        resource: Int
    ): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
        val textView = view.findViewById<TextView>(R.id.textView)

        textView.text = getItem(position)

        return view
    }
}
