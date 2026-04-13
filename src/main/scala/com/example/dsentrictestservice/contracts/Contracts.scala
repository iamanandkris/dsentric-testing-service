package com.example.dsentrictestservice.contracts

import io.dsentric.{Contract, ContractValidator, RawDecoder, RawObject}
import io.dsentric.annotations.*

private def nestedDecoder[T](using contract: Contract[T]): RawDecoder[T] = new RawDecoder[T]:
  override def decode(raw: Any): Option[T] =
    raw match
      case map: Map[?, ?] => contract.validate(map.asInstanceOf[RawObject]).toOption
      case _ => None

@contract final case class Address(
  street: Option[String] = None,
  city: Option[String] = None,
  zipCode: Option[String] = None
)
object Address:
  given Contract[Address] = Contract.derived[Address]
  given RawDecoder[Address] = nestedDecoder[Address]

@contract final case class Preferences(
  newsletter: Option[Boolean] = None,
  notifications: Option[Boolean] = None,
  @reserved internalSegment: Option[String] = None
)
object Preferences:
  given Contract[Preferences] = Contract.derived[Preferences]
  given RawDecoder[Preferences] = nestedDecoder[Preferences]

@contract final case class UserPayload(
  @email email: String,
  @nonEmpty name: String,
  @min(0) age: Option[Int] = None,
  address: Option[Address] = None,
  preferences: Option[Preferences] = None,
  @immutable registeredAt: Option[String] = None,
  @internal internalNotes: Option[String] = None
)
object UserPayload:
  given Contract[UserPayload] = Contract.derived[UserPayload]

@contract final case class Price(
  amount: Double,
  currency: String
)
object Price:
  given Contract[Price] = Contract.derived[Price]
  given RawDecoder[Price] = nestedDecoder[Price]

@contract final case class Inventory(
  available: Int,
  reserved: Int
)
object Inventory:
  given Contract[Inventory] = Contract.derived[Inventory]
  given RawDecoder[Inventory] = nestedDecoder[Inventory]

class InventoryValidator extends ContractValidator[ProductPayload]:
  override def validate(value: ProductPayload): List[String] =
    value.inventory match
      case Some(inventory) if inventory.reserved > inventory.available =>
        List("inventory.reserved must not exceed inventory.available")
      case _ => Nil

@validateContract(Array(classOf[InventoryValidator]))
@contract final case class ProductPayload(
  sku: String,
  @nonEmpty name: String,
  description: Option[String] = None,
  category: Option[String] = None,
  price: Price,
  inventory: Option[Inventory] = None,
  tags: Option[List[String]] = None,
  @immutable createdAt: Option[String] = None,
  @internal internalCost: Option[Double] = None
)
object ProductPayload:
  given Contract[ProductPayload] = Contract.derived[ProductPayload]

@contract final case class OrderItem(
  productId: Long,
  sku: String,
  @positive quantity: Int,
  unitPrice: Double
)
object OrderItem:
  given Contract[OrderItem] = Contract.derived[OrderItem]
  given RawDecoder[OrderItem] = nestedDecoder[OrderItem]

@contract final case class Totals(
  subtotal: Option[Double] = None,
  tax: Option[Double] = None,
  shipping: Option[Double] = None,
  total: Double
)
object Totals:
  given Contract[Totals] = Contract.derived[Totals]
  given RawDecoder[Totals] = nestedDecoder[Totals]

@contract final case class PaymentInfo(
  method: String,
  @masked("****") last4: String,
  @internal gatewayReference: Option[String] = None
)
object PaymentInfo:
  given Contract[PaymentInfo] = Contract.derived[PaymentInfo]
  given RawDecoder[PaymentInfo] = nestedDecoder[PaymentInfo]

class OrderTotalsValidator extends ContractValidator[OrderPayload]:
  override def validate(value: OrderPayload): List[String] =
    val computedSubtotal = value.items.map(item => item.quantity * item.unitPrice).sum
    val subtotal = value.totals.subtotal.getOrElse(computedSubtotal)
    val tax = value.totals.tax.getOrElse(0.0d)
    val shipping = value.totals.shipping.getOrElse(0.0d)
    val expectedTotal = subtotal + tax + shipping
    val failures = List.newBuilder[String]
    if math.abs(subtotal - computedSubtotal) > 0.01d then
      failures += "totals.subtotal must equal the sum of line items"
    if math.abs(value.totals.total - expectedTotal) > 0.01d then
      failures += "totals.total must equal subtotal + tax + shipping"
    failures.result()

@validateContract(Array(classOf[OrderTotalsValidator]))
@contract final case class OrderPayload(
  userId: Long,
  @immutable orderNumber: String,
  status: String,
  @nonEmpty items: List[OrderItem],
  totals: Totals,
  shippingAddress: Option[Address] = None,
  paymentInfo: Option[PaymentInfo] = None,
  placedAt: Option[String] = None,
  @reserved internalStatus: Option[String] = None
)
object OrderPayload:
  given Contract[OrderPayload] = Contract.derived[OrderPayload]
