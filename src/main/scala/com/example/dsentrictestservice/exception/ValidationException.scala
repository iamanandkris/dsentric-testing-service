package com.example.dsentrictestservice.exception

class ValidationException(
  message: String,
  val details: java.util.List[String],
  val language: String
) extends RuntimeException(message):
  def getDetails(): java.util.List[String] = details
  def getLanguage(): String = language
