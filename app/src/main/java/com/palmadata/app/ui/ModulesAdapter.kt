package com.palmadata.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.palmadata.app.R
import com.palmadata.app.data.model.AppModule
import com.palmadata.app.databinding.ItemModuleBinding

class ModulesAdapter(
    private val modules: List<AppModule>,
    private val onModuleClick: (AppModule) -> Unit
) : RecyclerView.Adapter<ModulesAdapter.ModuleViewHolder>() {

    inner class ModuleViewHolder(
        private val binding: ItemModuleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(module: AppModule) {
            val iconRes = module.iconResId ?: R.drawable.ic_module_placeholder
            binding.imgModuleIcon.setImageResource(iconRes)
            binding.root.setOnClickListener {
                onModuleClick(module)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val binding = ItemModuleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ModuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        holder.bind(modules[position])
    }

    override fun getItemCount(): Int = modules.size
}
