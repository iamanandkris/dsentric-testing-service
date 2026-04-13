package com.example.dsentrictestservice.service

import com.example.dsentrictestservice.contracts.*
import com.example.dsentrictestservice.exception.ValidationException
import com.example.dsentrictestservice.util.ScalaMapSupport
import io.dsentric.{Contract, ContractViolations, RawObject}
import org.springframework.stereotype.Component

import scala.jdk.CollectionConverters.*

@Component
class ContractFacade:
  def validateUser(payload: java.util.Map[String, Object], language: String): java.util.Map[String, Object] =
    validate(payload, summon[Contract[UserPayload]], language)

  def validateProduct(payload: java.util.Map[String, Object], language: String): java.util.Map[String, Object] =
    validate(payload, summon[Contract[ProductPayload]], language)

  def validateOrder(payload: java.util.Map[String, Object], language: String): java.util.Map[String, Object] =
    validate(payload, summon[Contract[OrderPayload]], language)

  def patchUser(current: java.util.Map[String, Object], patch: java.util.Map[String, Object], language: String): java.util.Map[String, Object] =
    applyPatch(current, patch, summon[Contract[UserPayload]], language)

  def patchProduct(current: java.util.Map[String, Object], patch: java.util.Map[String, Object], language: String): java.util.Map[String, Object] =
    applyPatch(current, patch, summon[Contract[ProductPayload]], language)

  def patchOrder(current: java.util.Map[String, Object], patch: java.util.Map[String, Object], language: String): java.util.Map[String, Object] =
    applyPatch(current, patch, summon[Contract[OrderPayload]], language)

  def sanitizeUser(payload: java.util.Map[String, Object]): java.util.Map[String, Object] =
    sanitize(payload, summon[Contract[UserPayload]])

  def sanitizeProduct(payload: java.util.Map[String, Object]): java.util.Map[String, Object] =
    sanitize(payload, summon[Contract[ProductPayload]])

  def sanitizeOrder(payload: java.util.Map[String, Object]): java.util.Map[String, Object] =
    sanitize(payload, summon[Contract[OrderPayload]])

  def userSchema(): java.util.Map[String, Object] =
    schema(summon[Contract[UserPayload]])

  def productSchema(): java.util.Map[String, Object] =
    schema(summon[Contract[ProductPayload]])

  def orderSchema(): java.util.Map[String, Object] =
    schema(summon[Contract[OrderPayload]])

  def validateUserDraft(payload: java.util.Map[String, Object], language: String): java.util.Map[String, Object] =
    validateDraft(payload, summon[Contract[UserPayload]], language)

  def validateProductDraft(payload: java.util.Map[String, Object], language: String): java.util.Map[String, Object] =
    validateDraft(payload, summon[Contract[ProductPayload]], language)

  def validateOrderDraft(payload: java.util.Map[String, Object], language: String): java.util.Map[String, Object] =
    validateDraft(payload, summon[Contract[OrderPayload]], language)

  private def validate[T](payload: java.util.Map[String, Object], contract: Contract[T], language: String): java.util.Map[String, Object] =
    val raw = ScalaMapSupport.toScalaMap(payload)
    contract.validate(raw) match
      case Right(_) => ScalaMapSupport.toJavaMap(raw)
      case Left(violations) => throw validationException(violations, language)

  private def applyPatch[T](
    currentPayload: java.util.Map[String, Object],
    patchPayload: java.util.Map[String, Object],
    contract: Contract[T],
    language: String
  ): java.util.Map[String, Object] =
    val current = ScalaMapSupport.toScalaMap(currentPayload)
    val patch = ScalaMapSupport.toScalaMap(patchPayload)
    val merged = ScalaMapSupport.deepMerge(current, patch)
    contract.validatePatch(current, patch) match
      case Right(_) => ScalaMapSupport.toJavaMap(merged)
      case Left(violations) => throw validationException(violations, language)

  private def sanitize[T](payload: java.util.Map[String, Object], contract: Contract[T]): java.util.Map[String, Object] =
    ScalaMapSupport.toJavaMap(contract.sanitize(ScalaMapSupport.toScalaMap(payload)))

  private def schema[T](contract: Contract[T]): java.util.Map[String, Object] =
    ScalaMapSupport.toJavaMap(contract.jsonSchema)

  private def validateDraft[T](payload: java.util.Map[String, Object], contract: Contract[T], language: String): java.util.Map[String, Object] =
    val raw = ScalaMapSupport.toScalaMap(payload)
    contract.validatePartial(raw) match
      case Right(_) => ScalaMapSupport.toJavaMap(raw)
      case Left(violations) => throw validationException(violations, language)

  private def validationException(violations: ContractViolations, language: String): ValidationException =
    val details = violations.violations.map(v => s"${v.path.toString}: ${v.message}").toList.asJava
    new ValidationException("Validation failed", details, language)
