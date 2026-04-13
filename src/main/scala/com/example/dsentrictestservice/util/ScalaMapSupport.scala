package com.example.dsentrictestservice.util

import scala.jdk.CollectionConverters.*

object ScalaMapSupport:
  def toScalaMap(javaMap: java.util.Map[String, Object]): Map[String, Any] =
    Option(javaMap).map(_.asScala.toMap.view.mapValues(toScalaValue).toMap).getOrElse(Map.empty)

  def toJavaMap(scalaMap: Map[String, Any]): java.util.LinkedHashMap[String, Object] =
    val javaMap = new java.util.LinkedHashMap[String, Object]()
    scalaMap.foreach { case (key, value) => javaMap.put(key, toJavaValue(value)) }
    javaMap

  def deepMerge(current: Map[String, Any], patch: Map[String, Any]): Map[String, Any] =
    patch.foldLeft(current) { case (acc, (key, value)) =>
      (acc.get(key), value) match
        case (Some(existing: Map[?, ?]), nested: Map[?, ?]) =>
          acc.updated(
            key,
            deepMerge(
              existing.asInstanceOf[Map[String, Any]],
              nested.asInstanceOf[Map[String, Any]]
            )
          )
        case _ => acc.updated(key, value)
    }

  private def toScalaValue(value: Any): Any =
    value match
      case null => null
      case map: java.util.Map[?, ?] =>
        map.asInstanceOf[java.util.Map[String, Object]].asScala.toMap.view.mapValues(toScalaValue).toMap
      case list: java.util.List[?] =>
        list.asScala.toList.map(toScalaValue)
      case other => other

  private def toJavaValue(value: Any): Object =
    value match
      case null => null
      case map: Map[?, ?] =>
        toJavaMap(map.asInstanceOf[Map[String, Any]])
      case list: Iterable[?] =>
        list.map(toJavaValue).toList.asJava
      case opt: Option[?] =>
        opt.map(toJavaValue).orNull
      case other => other.asInstanceOf[Object]
