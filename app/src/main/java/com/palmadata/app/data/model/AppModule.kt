package com.palmadata.app.data.model

data class AppModule(
    val id: String,
    val name: String,
    val iconResId: Int?,
    val isEnabled: Boolean = true,
    val destinationClass: Class<*>? = null
)