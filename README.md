# Spark With Scala

## Section 1: Spark Basics

### Spark in a nutshell

Spark is a framework for distributed processing of large data sets. 
It contains functions that lets you import data from a distributed data source like an HDFS file system or S3 and it 
provides a mechanism for very simply and very efficiently processing that data.

The high level architecture of a spark application is comprised of:
1. Driver program (built around the spark context object)
2. Cluster Manager - responsible for distributing the work defined by the driver script among multiple nodes.
3. Executor - The processing unit that performs the tasks as orchestrated by the cluster manager.

Spark works in a "lazy" pattern meaning that nothing actually happens until you actually hit a command that says "I 
want to collect the results and do something with them"
Once spark sees an action like that it will go back and figure out the optimal way to combine all of your previous code
together and come up with a plan that's optimal for actually producing the end result that you want.

#### Spark consists of comonents:

* Spark Core -  deals with the basics of dealing with RDD'S and transforming them and collecting their results and 
tallying them and reducing things together.

There are also libraries built on top of spark to make more complex operation even simpler:

* **Spark Streaming** - That's actually a technology built on top of Spark that can handle a little microbatches of data as
they come in in real time.

* **Spark SQL** - allows a simple SQL like interface to spark

* **MLLib** - Machine learning operations on massive datasets.

* **GraphX** - Graph Analysis Framework

 
### RDD - The Resilient Distributed Dataset
 
#### Transforming RDD's:
 
 * `map` - apply a transformation function on the RDD (produces another RDD)
 
 * `flatmap` - similar to `map` but there is no 1-to-1 correlation between the input RDD and the output RDD
 
 * `filter` - apply a boolean function to trim down an RDD
 
 * `distict` - removes duplicate rows from an RDD
 
 * `sample` - creates a random sample from an RDD (useful for testing)
 
 * Set Operations: `union`, `intersection`, `subtract`, `catesian`  

#### RDD actions:

RDD has a lazy evaluation - nothing actually happens until an action is called on the RDD.

Commonly used actions: 

* `collect` - take the result of an RDD and collects it back down to the driver script

* `count` - count how many rows are in an RDD

* `countByValue` - count unique values

* `take` - take the first `n` results from the RDD

* `reduce` - combine together values by a certain key 

#### Key/Value RDD

Common operations on key/value RDDs:

* `reduceByKey` - Combine values with the same key using some function (for example `rdd.reduceByKey( (x,y) => x + y )`)

* `groupByKey` - group values with the same key

* `sortByKey` - sort RDD by key values

* `keys`, `values` - Create an RDD of just the keys or just the values

### RatingsCounter - Under The Hood

```scala
val sc: SparkContext = new SparkContext("local[*]", "RatingsCounter")
val lines: RDD[String] = sc.textFile(getClass.getResource("/ml-100k/u.data").getPath)
val ratings: RDD[String] = lines.map(x => x.toString().split("\t")(2))
val results: scala.collection.Map[String, Long] = ratings.countByValue()
val sortedResults = results.toSeq.sortBy(_._1)
sortedResults.foreach(println)
``` 

The `textFile` and `map` operations can be easily parallelized because there is a 1-to-1 relation between the input and
the output. 

The `countByValue` operation, however, is a shuffle operation.

Therefore Spark would create an execution plan by dividing the job into 2 stages: 

**Stage 1**: `textFile(...)`, `map`

**Stage 2**: `countByValue`

Each stage is broken into tasks (which may be distributed across a cluster)


## Terminology

* RDD - Resilient Distributed Dataset - a spark construct or a little mini databse of infromation

    ```scala
    val rdd = sc.textFile("README.md")
    ``` 

* Shuffle Operations - operations that require spark to "push data around" on the cluster which can be really expensive.
Typically we would want to minimize shuffle operations.
 
