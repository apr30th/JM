package com.example.jm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jm.RecyclerItemAdapter_camera.ViewHolder


class RecyclerItemAdapter_camera(private val context: CameraActivity) : RecyclerView.Adapter<ViewHolder>() {
    var datas = mutableListOf<RecipeData>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_view,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = datas.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(datas[position])
    }

    // 각 항목에 필요한 기능을 구현
    inner class ViewHolder(v : View) : RecyclerView.ViewHolder(v) {
        private val micro_recipe1: TextView = itemView.findViewById(R.id.micro_recipe1)
        private val micro_recipe2: TextView = itemView.findViewById(R.id.micro_recipe2)
        private val air_recipe1: TextView = itemView.findViewById(R.id.air_recipe1)
        private val air_recipe2: TextView = itemView.findViewById(R.id.air_recipe2)
        //private var view: View = v
        fun bind(recipe: RecipeData) {
            micro_recipe1.text = recipe.micro_recipe1
            micro_recipe2.text = recipe.micro_recipe2
            air_recipe1.text = recipe.air_recipe1
            air_recipe2.text = recipe.air_recipe2

            //view.setOnClickListener(listener)
        }
    }
}
