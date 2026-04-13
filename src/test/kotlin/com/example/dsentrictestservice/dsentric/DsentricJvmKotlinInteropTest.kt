package com.example.dsentrictestservice.dsentric

import io.dsentric.JvmContract
import io.dsentric.annotations.contract
import io.dsentric.annotations.decodable
import io.dsentric.annotations.email
import io.dsentric.annotations.internal
import io.dsentric.annotations.masked
import io.dsentric.annotations.nonEmpty
import io.dsentric.annotations.reserved
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional

class DsentricJvmKotlinInteropTest {
    @Test
    fun kotlinPrimaryContractShouldDecodeNestedDataClasses() {
        val contract = JvmContract.ofPrimary(KotlinUserPayload::class.java)
        val payload = linkedMapOf<String, Any>(
            "email" to "kotlin@example.com",
            "name" to "Kotlin User",
            "address" to mapOf("street" to "1 Kotlin St", "city" to "Leeds", "zipCode" to "LS11AA"),
            "preferences" to mapOf("newsletter" to true),
        )

        assertDoesNotThrow {
            val result = contract.validate(payload)
            assertTrue(result.isValid, "expected nested Kotlin data-class validation to succeed")
            val value = result.getValue().orElseThrow()
            assertEquals("1 Kotlin St", value.address!!.street)
            assertEquals(true, value.preferences!!.newsletter)
        }
    }

    @Test
    fun kotlinPrimaryContractShouldTreatOptionalAnnotatedFieldsAsOptional() {
        val contract = JvmContract.ofPrimary(KotlinUserPayload::class.java)
        val payload = linkedMapOf<String, Any>(
            "email" to "optional@example.com",
            "name" to "Optional Kotlin User",
        )

        val result = contract.validate(payload)

        assertTrue(result.isValid, "expected omitted optional fields not to be treated as required")
    }

    @Test
    fun kotlinPrimaryContractShouldRejectNestedReservedField() {
        val contract = JvmContract.ofPrimary(KotlinUserPayload::class.java)
        val payload = linkedMapOf<String, Any>(
            "email" to "reserved@example.com",
            "name" to "Reserved Kotlin User",
            "preferences" to mapOf("internalSegment" to "secret"),
        )

        val result = contract.validate(payload)

        assertFalse(result.isValid, "expected nested reserved field to be rejected")
        assertTrue(result.getErrors().any { it.path.contains("internalSegment") })
    }

    @Test
    fun kotlinPrimaryContractShouldMaskAndDropNestedSensitiveFieldsOnSanitize() {
        val contract = JvmContract.ofPrimary(KotlinOrderPayload::class.java)
        val payload = linkedMapOf<String, Any>(
            "userId" to 1L,
            "orderNumber" to "ORD-KOTLIN-001",
            "status" to "pending",
            "items" to listOf(mapOf("productId" to 1L, "sku" to "SKU-1", "quantity" to 2, "unitPrice" to 10.0)),
            "totals" to mapOf("subtotal" to 20.0, "tax" to 2.0, "shipping" to 1.0, "total" to 23.0),
            "paymentInfo" to mapOf("method" to "credit_card", "last4" to "1234", "gatewayReference" to "gw-123"),
        )

        val sanitized = contract.sanitize(payload)
        @Suppress("UNCHECKED_CAST")
        val paymentInfo = sanitized["paymentInfo"] as Map<String, Any?>?

        assertNotNull(paymentInfo, "expected nested paymentInfo to survive sanitize")
        assertEquals("****", paymentInfo!!["last4"])
        assertFalse(paymentInfo.containsKey("gatewayReference"))
    }
}

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
    @param:reserved @field:reserved val internalSegment: Optional<String> = Optional.empty(),
)

@decodable
@contract
data class KotlinOrderItem(
    val productId: Long,
    val sku: String,
    val quantity: Int,
    val unitPrice: Double,
)

@decodable
@contract
data class KotlinTotals(
    val subtotal: Double,
    val tax: Double,
    val shipping: Double,
    val total: Double,
)

@decodable
@contract
data class KotlinPaymentInfo(
    val method: String,
    @param:masked("****") @field:masked("****") val last4: String,
    @param:internal @field:internal val gatewayReference: Optional<String> = Optional.empty(),
)

@contract
data class KotlinUserPayload(
    @param:email @field:email val email: String,
    @param:nonEmpty @field:nonEmpty val name: String,
    val address: KotlinAddress? = null,
    val preferences: KotlinPreferences? = null,
    @param:internal @field:internal val internalNotes: Optional<String> = Optional.empty(),
)

@contract
data class KotlinOrderPayload(
    val userId: Long,
    val orderNumber: String,
    val status: String,
    val items: List<KotlinOrderItem>,
    val totals: KotlinTotals,
    val paymentInfo: KotlinPaymentInfo,
)
