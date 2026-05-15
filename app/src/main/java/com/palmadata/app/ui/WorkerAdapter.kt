package com.palmadata.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.palmadata.app.databinding.ItemWorkerBinding

class WorkerAdapter(
    private val onItemSelected: (String) -> Unit
) : RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder>() {

    private var listaCompleta: List<String> = emptyList()
    private var listaFiltrada: List<String> = emptyList()

    fun submitList(items: List<String>) {
        listaCompleta = items
        listaFiltrada = items
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        listaFiltrada = if (query.isBlank()) {
            listaCompleta
        } else {
            listaCompleta.filter {
                it.contains(query.trim(), ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    fun isEmpty(): Boolean = listaFiltrada.isEmpty()

    inner class WorkerViewHolder(
        private val binding: ItemWorkerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(name: String) {
            binding.tvWorkerName.text = name
            binding.root.setOnClickListener {
                onItemSelected(name)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val binding = ItemWorkerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WorkerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        holder.bind(listaFiltrada[position])
    }

    override fun getItemCount(): Int = listaFiltrada.size
}