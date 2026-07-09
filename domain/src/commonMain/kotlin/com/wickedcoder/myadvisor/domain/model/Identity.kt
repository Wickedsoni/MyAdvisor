package com.wickedcoder.myadvisor.domain.model

data class Issuer(val id: String, val name: String)

data class Category(val id: String, val name: String)

data class MerchantFamily(val id: String, val name: String)

data class Merchant(
    val id: String,
    val familyId: String?,
    val name: String,
    val categoryId: String,
)
