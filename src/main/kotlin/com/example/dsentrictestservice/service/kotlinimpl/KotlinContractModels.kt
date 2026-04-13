package com.example.dsentrictestservice.service.kotlinimpl

import io.dsentric.ContractValidator
import io.dsentric.JvmContract
import io.dsentric.annotations.contract
import io.dsentric.annotations.decodable
import io.dsentric.annotations.email
import io.dsentric.annotations.immutable
import io.dsentric.annotations.internal
import io.dsentric.annotations.masked
import io.dsentric.annotations.min
import io.dsentric.annotations.nonEmpty
import io.dsentric.annotations.positive
import io.dsentric.annotations.reserved
import io.dsentric.annotations.validateContract
import java.util.Optional
import scala.jdk.javaapi.CollectionConverters

private fun scalaListOf(vararg values: String): scala.collection.immutable.List<String> =
    CollectionConverters.asScala(values.toList()).toList() as scala.collection.immutable.List<String>

@decodable
@contract
data class KotlinAddress(
    val street: String? = null,
    val city: String? = null,
    val zipCode: String? = null,
)

@decodable
@contract
data class KotlinPreferences(
    val newsletter: Boolean? = null,
    val notifications: Boolean? = null,
    @param:reserved @field:reserved val internalSegment: String? = null,
)

@contract
data class KotlinUserPayload(
    @param:email @field:email val email: String,
    @param:nonEmpty @field:nonEmpty val name: String,
    @param:min(0) @field:min(0) val age: Int? = null,
    val address: KotlinAddress? = null,
    val preferences: KotlinPreferences? = null,
    @param:immutable @field:immutable val registeredAt: Optional<String> = Optional.empty(),
    @param:internal @field:internal val internalNotes: Optional<String> = Optional.empty(),
)

@decodable
@contract
data class KotlinPrice(
    val amount: Double,
    val currency: String,
)

@decodable
@contract
data class KotlinInventory(
    val available: Int,
    val reserved: Int,
)

class KotlinInventoryValidator : ContractValidator<KotlinProductPayload> {
    override fun validate(value: KotlinProductPayload): scala.collection.immutable.List<String> {
        val available = value.inventory?.available
        val reserved = value.inventory?.reserved
        return if (available != null && reserved != null && reserved > available) {
            scalaListOf("inventory.reserved must not exceed inventory.available")
        } else {
            scalaListOf()
        }
    }
}

@validateContract(KotlinInventoryValidator::class)
@contract
data class KotlinProductPayload(
    val sku: String,
    @param:nonEmpty @field:nonEmpty val name: String,
    val description: String? = null,
    val category: String? = null,
    val price: KotlinPrice,
    val inventory: KotlinInventory? = null,
    val tags: List<String>? = null,
    @param:immutable @field:immutable val createdAt: Optional<String> = Optional.empty(),
    @param:internal @field:internal val internalCost: Optional<Double> = Optional.empty(),
)

@decodable
@contract
data class KotlinOrderItem(
    val productId: Long,
    val sku: String,
    @param:positive @field:positive val quantity: Int,
    val unitPrice: Double,
)

@decodable
@contract
data class KotlinTotals(
    val subtotal: Double? = null,
    val tax: Double? = null,
    val shipping: Double? = null,
    val total: Double,
)

@decodable
@contract
data class KotlinPaymentInfo(
    val method: String,
    @param:masked("****") @field:masked("****") val last4: String,
    @param:internal @field:internal val gatewayReference: Optional<String> = Optional.empty(),
)

class KotlinOrderTotalsValidator : ContractValidator<KotlinOrderPayload> {
    override fun validate(value: KotlinOrderPayload): scala.collection.immutable.List<String> {
        val computedSubtotal = value.items.sumOf { item ->
            val quantity = item.quantity
            val unitPrice = item.unitPrice
            quantity * unitPrice
        }
        val subtotal = value.totals.subtotal ?: computedSubtotal
        val tax = value.totals.tax ?: 0.0
        val shipping = value.totals.shipping ?: 0.0
        val total = value.totals.total
        val expectedTotal = subtotal + tax + shipping
        val errors = ArrayList<String>()
        if (kotlin.math.abs(subtotal - computedSubtotal) > 0.01) {
            errors += "totals.subtotal must equal the sum of line items"
        }
        if (kotlin.math.abs(total - expectedTotal) > 0.01) {
            errors += "totals.total must equal subtotal + tax + shipping"
        }
        return CollectionConverters.asScala(errors).toList() as scala.collection.immutable.List<String>
    }
}

@validateContract(KotlinOrderTotalsValidator::class)
@contract
data class KotlinOrderPayload(
    val userId: Long,
    @param:immutable @field:immutable val orderNumber: String,
    val status: String,
    @param:nonEmpty @field:nonEmpty val items: List<KotlinOrderItem>,
    val totals: KotlinTotals,
    val shippingAddress: KotlinAddress? = null,
    val paymentInfo: KotlinPaymentInfo? = null,
    val placedAt: Optional<String> = Optional.empty(),
    @param:reserved @field:reserved val internalStatus: Optional<String> = Optional.empty(),
)

object KotlinContractModels {
    @JvmField
    val USER_CONTRACT: JvmContract<KotlinUserPayload> = JvmContract.ofPrimary(KotlinUserPayload::class.java)

    @JvmField
    val PRODUCT_CONTRACT: JvmContract<KotlinProductPayload> = JvmContract.ofPrimary(KotlinProductPayload::class.java)

    @JvmField
    val ORDER_CONTRACT: JvmContract<KotlinOrderPayload> = JvmContract.ofPrimary(KotlinOrderPayload::class.java)
}
