package com.sundogsoftware.spark.basics

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext

object PurchaseByCustomer {

  def main(args: Array[String]): Unit = {

    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    // Create a SparkContext using the local machine
    val sc = new SparkContext("local", "WordCountBetterSorted")

    // Load each line of my book into an RDD
    val input = sc.textFile(getClass.getResource("/customer-orders.csv").getPath)

    val parsed = input.map(line => {
      val  fields = line.split(",")
      val customerId = fields(0)
      val spent = fields(2).toFloat
      (customerId, spent)
    })

    val totalByCustomer = parsed.reduceByKey((x,y) => x + y).collect()

    totalByCustomer.foreach(println)

  }
}
