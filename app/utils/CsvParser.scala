package utils

import java.nio.file.{Files, Path}

import javax.inject.Singleton
import models.Product

import scala.io.Source

@Singleton
class CsvParser {
  private def csvRowToProduct(row: String): Product = {
    val values = row.split(",")
    Product(values(0).toLong, values(1), values(2), values(3).toDouble, values(4), values(5))
  }

  def parseCsv(path: Path): List[Product] = {
    val startTime = System.currentTimeMillis()
    val source = Source.fromFile(path.toString)
    val csvRowsList = source.getLines().toList
    source.close
    if (Files.deleteIfExists(path)) println(s"${path.getFileName} deleted")
    val productsList = csvRowsList.map(csvRowToProduct)
    println(s"${path.getFileName} parsed in ${(System.currentTimeMillis() - startTime) / 1000.0} sec")
    productsList
  }
}