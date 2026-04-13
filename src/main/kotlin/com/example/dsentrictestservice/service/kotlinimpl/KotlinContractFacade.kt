package com.example.dsentrictestservice.service.kotlinimpl

import com.example.dsentrictestservice.service.jvm.JvmContractSupport
import com.example.dsentrictestservice.service.jvm.JvmLanguageContractFacade
import org.springframework.stereotype.Component

@Component
class KotlinContractFacade : JvmLanguageContractFacade {
    override fun validateUser(payload: Map<String, Any?>, language: String): Map<String, Any?> =
        JvmContractSupport.validate(payload, KotlinContractModels.USER_CONTRACT, language)

    override fun validateProduct(payload: Map<String, Any?>, language: String): Map<String, Any?> =
        JvmContractSupport.validate(payload, KotlinContractModels.PRODUCT_CONTRACT, language)

    override fun validateOrder(payload: Map<String, Any?>, language: String): Map<String, Any?> =
        JvmContractSupport.validate(payload, KotlinContractModels.ORDER_CONTRACT, language)

    override fun patchUser(current: Map<String, Any?>, patch: Map<String, Any?>, language: String): Map<String, Any?> =
        JvmContractSupport.validatePatch(current, patch, deepMerge(current, patch), KotlinContractModels.USER_CONTRACT, language)

    override fun patchProduct(current: Map<String, Any?>, patch: Map<String, Any?>, language: String): Map<String, Any?> =
        JvmContractSupport.validatePatch(current, patch, deepMerge(current, patch), KotlinContractModels.PRODUCT_CONTRACT, language)

    override fun patchOrder(current: Map<String, Any?>, patch: Map<String, Any?>, language: String): Map<String, Any?> =
        JvmContractSupport.validatePatch(current, patch, deepMerge(current, patch), KotlinContractModels.ORDER_CONTRACT, language)

    override fun sanitizeUser(payload: Map<String, Any?>): Map<String, Any?> =
        JvmContractSupport.sanitize(payload, KotlinContractModels.USER_CONTRACT)

    override fun sanitizeProduct(payload: Map<String, Any?>): Map<String, Any?> =
        JvmContractSupport.sanitize(payload, KotlinContractModels.PRODUCT_CONTRACT)

    override fun sanitizeOrder(payload: Map<String, Any?>): Map<String, Any?> =
        JvmContractSupport.sanitize(payload, KotlinContractModels.ORDER_CONTRACT)

    override fun userSchema(): Map<String, Any?> =
        JvmContractSupport.schema(KotlinContractModels.USER_CONTRACT)

    override fun productSchema(): Map<String, Any?> =
        JvmContractSupport.schema(KotlinContractModels.PRODUCT_CONTRACT)

    override fun orderSchema(): Map<String, Any?> =
        JvmContractSupport.schema(KotlinContractModels.ORDER_CONTRACT)

    override fun validateUserDraft(payload: Map<String, Any?>, language: String): Map<String, Any?> =
        JvmContractSupport.validateDraft(payload, KotlinContractModels.USER_CONTRACT, language)

    override fun validateProductDraft(payload: Map<String, Any?>, language: String): Map<String, Any?> =
        JvmContractSupport.validateDraft(payload, KotlinContractModels.PRODUCT_CONTRACT, language)

    override fun validateOrderDraft(payload: Map<String, Any?>, language: String): Map<String, Any?> =
        JvmContractSupport.validateDraft(payload, KotlinContractModels.ORDER_CONTRACT, language)

    @Suppress("UNCHECKED_CAST")
    private fun deepMerge(current: Map<String, Any?>, patch: Map<String, Any?>): Map<String, Any?> {
        val merged = LinkedHashMap(current)
        for ((key, incoming) in patch) {
            val existing = merged[key]
            if (existing is Map<*, *> && incoming is Map<*, *>) {
                merged[key] = deepMerge(existing as Map<String, Any?>, incoming as Map<String, Any?>)
            } else {
                merged[key] = incoming
            }
        }
        return merged
    }
}
