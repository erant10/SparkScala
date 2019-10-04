package com.sundogsoftware.spark.advanced

import org.apache.log4j._
import org.apache.spark._

/** Find the superhero with the most co-appearances. */
object MostPopularSuperhero {

  // Function to extract the hero ID and number of connections from each line
  def countCoOccurences(line: String) = {
    var elements = line.split("\\s+")
    (elements(0).toInt, elements.length - 1)
  }

  // Function to extract hero ID -> hero name tuples (or None in case of failure)
  def parseNames(line: String): Option[(Int, String)] = {
    var fields = line.split('\"')
    if (fields.length > 1) {
      return Some(fields(0).trim().toInt, fields(1))
    } else {
      return None // flatmap will just discard None results, and extract data from Some results.
    }
  }

  /** Our main function where the action happens */
  def main(args: Array[String]) {

    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    // Create a SparkContext using every core of the local machine
    val sc = new SparkContext("local[*]", "MostPopularSuperhero")

    // Build up a hero ID -> name RDD
    val names = sc.textFile(getClass.getResource("/marvel-names.txt").getPath)
    val namesRdd = names.flatMap(parseNames)

    // Load up the superhero co-apperarance data
    val lines = sc.textFile(getClass.getResource("/marvel-graph.txt").getPath)

    // Convert to (heroID, number of connections) RDD
    val pairings = lines.map(countCoOccurences)

    // Combine entries that span more than one line
    val totalFriendsByCharacter = pairings.reduceByKey((x, y) => x + y)

    // Flip it to # of connections, hero ID
    val flipped = totalFriendsByCharacter.map(x => (x._2, x._1))

    // Find the max # of connections
    flipped.sortByKey(false).take(10).foreach(pair => {

      // Look up the name (lookup returns an array of results, so we need to access the first result with (0)).
      val name = namesRdd.lookup(pair._2).head

      println(s"$name has ${pair._1}' co-appearances")
    })
  }

}
