package services

import models.Product

import scala.io.Source

object CsvParser {
  private def csvRowToProduct(row: String): Product = {
    val values = row.split(",")
    Product(values(0).toLong, values(1), values(2), values(3).toDouble, values(4))
  }

  def parseCsv(path: String): List[Product] = {
    val source = Source.fromFile(path)
    val csvRowsList = source.getLines().toList
    source.close
    val productsList = csvRowsList.map(csvRowToProduct)
    productsList
  }
}