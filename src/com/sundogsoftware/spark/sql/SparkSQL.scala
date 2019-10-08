package com.sundogsoftware.spark.sql

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession

object SparkSQL {

  case class Person(ID:Int, name:String, age:Int, numFriends:Int)

  def mapper(line:String): Person = {
    val fields = line.split(',')

    val person:Person = Person(fields(0).toInt, fields(1), fields(2).toInt, fields(3).toInt)
    person
  }

  /** Our main function where the action happens */
  def main(args: Array[String]) {

    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    // Use new SparkSession interface in Spark 2.0
    val spark = SparkSession
      .builder
      .appName("SparkSQL")
      .master("local[*]")
      .getOrCreate()

    val lines = spark.sparkContext.textFile(getClass.getResource("/fakefriends.csv").getPath)
    val people = lines.map(mapper)

    // Infer the schema, and register the DataSet as a table.
    import spark.implicits._
    val schemaPeople = people.toDS

    schemaPeople.printSchema()

    // convert the content of the DataSet to a "sql table"
    schemaPeople.createOrReplaceTempView("people")

    // SQL can be run over DataFrames that have been registered as a table
    val teenagers = spark.sql("SELECT * FROM people WHERE age >= 13 AND age <= 19")

    val results = teenagers.collect()

    results.foreach(println)

    // stop the session object
    spark.stop()
  }
}
