# Apache Spark with Scala - Hands On with Big Data (Udemy Course)

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

## Section 2: Advanced Examples of Spark Programs

### Multiple data sources

There are 3 main approaches to combining multiple dataset - each depending on size and needs

1. Using standard scala data structures (Map, Set, Seq etc..). This approach works well assuming the dataset is small 
enough to be loaded in-mempry. 

2. Broadcast variables: allow us to take a chunk of data and explicitly sending it to all the nodes in our cluster so 
that its ready for whenever it needs it (without having to transmit it across the network.

3. RDD's - Loading multiple RDD will also make them all available to all of the nodes in the cluster, and there 
are many operations that allow joining RDD's (by keys, values etc).

This can be achieved by calling the `sc.broadcast()`, for example:

```scala
/** Load up a Map of movie IDs to movie names. */
def loadMovieNames() : Map[Int, String] = ???

// ...

// Create a broadcast variable of our ID -> movie name map
var nameDict = sc.broadcast(loadMovieNames)

// ...

// Fold in the movie names from the broadcast variable
val sortedMoviesWithNames = sortedMovies.map( x  => (nameDict.value(x._2), x._1) )

```

### Caching and Persisting

Any time we will perform more than 1 action on an rdd, we must cache it to prevent having to re-create the RDD.

`persist` gives the option to cache something to the disk instead of just to the memory (requires more resources to get
to the specific state, but is better if we want to be more fault-tolerant)

## Section 3: Running spark on a cluster

### Using spark on Amazon Elastic MapReduce

In order to set up a spark task and execute it or EMR, we need to build a jar file containing the classes of the driver 
program. 

However we can assume that any EMR cluster already has all of Sparks versions on it, so when creating our `build.sbt` 
we can use the `"provided"` tag.

```sbt
name := "PopularMovies"

version := 1.0 

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % 2.4.3 % "provided"
)
```  

The jar for the project can be built using `sbt-assembly`.

**project/assembly.sbt**

```sbt
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
```

### Spark Submit

spark-submit is the command we use to execute a driver program (in this case packed in a jar file).

parameters:

* `--master`: has to be set based on what kind of a cluster we are using.

* `--num-executors`: how many executor node we want to use (2 by default)

* `--executor-memory`: how much memory is available to each executor (make sure not to use more memory than we have).

* `--total-executor-core`: limit the number of cores the script can consume.

#### configuration hierarchy:

1. Hardcoded setting in the driver script (e.g. `val sc = new SparkContext("local[*]", "MyDriverScript")`)   

2. Command line arguments passed when calling `spark-submit` (e.g. `--num-executors`)

3. Default spark configuration files (provided for example by AWS EMR).  

### Spark vs Hadoop?

Spark and hadoop are **NOT** mutually exclusive.

Hadoop itself is just a technology for managing a cluster.

Yarn, one of the components of hadoop, is the cluster manager which spark can run on top of. 

MapReduce is a way of running distributed jobs on a hadoop cluster.

### Partitioning

Sometimes we need to "hint" to spark on whats the best way to distribute the data.

We want to use the `partitionBy` command whenever we are running an expensive operation that would benefit from 
partitioning.

`partitionBy(n)` tells spark to "break" the operation into `n` tasks.

*reminder*: Spark breaks down the DAG into stages (between where it needs to shuffle data), and each stage is broken 
up into individual tasks that are distributed to each node in the cluster (executors).

Common  operations that can benefit from partitioning: `join`, `cogroup`, `groupWith`, `leftOuterJoin`, 
`rightOuterJoin`, `groupByKey`, `reduceByKey`, `combineByKey` & `lookup`.

Once we specify a partitioning on an RDD, it will be preserved in the result of that operation.

#### How many partitions?

* We want to make sure that we have at least as many partitions as we have executors so that we can split up the jobs 
efficiently.

* Too few partitions won't take full advantage of the cluster

* Too many partitions will result in too much overhead from shuffling data

* `partitionBy(100)` is usually a reasonable place to start on large operations

  

### Best practices for running on a cluster

* Use an empty, default sparkConf object in the driver script. This means we will use the default EMR settings. 

* Only change the conf if you know what you are doing.

* Executor may fail on exceeded memory (too much data on a partition, or too few executors). 
    
    memory can be adjusted using the `--executor-memory` attribute

* Scripts and data should ideally be stored somewhere that can be easily accessed by EMR (e.g. s3 or hdfs).

* **REMEMBER TO TERMINATE THE CLUSTER WHEN DONE**. 

### Troubleshooting Cluster Jobs

* SparkUI - as web interface opened on port 4040 of the ,aster machine

* Logs - logs are distributed, and can be collected using `yarn logs --applicationID <app ID>`, or they can be dumped 
to an s3 bucket when using EMR.

* Memory problems can occur when we are asking too much of each executor, common solutions can be:

    * more executors
    
    * more memory on each executor
    
    * use `partitionBy` to demand less work from individual executor by using smaller partitions.  

## Section 4: Spark SQL

### DataFrames

RDD's can contain anything (unstructured data). 

DataFrames extend RDD's to structured data, which can wrap data more compactly and lets spark optimize better.

**DataFrame properties:** 

* Contain Row objects

* Can run SQL Queries

* Has a schema (leading to more efficient storage)

* Read and Write to JSON, Hive, Parquet

* Communicates with JDBC/ODBC, Tableau

### DataSets vs DataFrame

A DataFrame is a DataSet of Row objects.

DataSets can explicitly wrap a given type (e.g. `DataSet[Person]`, `DataSet[String,Double]`) 

DataFrame schema is inferred at runtime; but a dataset can be inferred at **compile time**.

RDD's can be converted to DataSets (with the method `rdd.toDS()`).

The Trend in Spark is to use RDD's less and DataSets more.

DataSets are more efficient:

* They can seriallize very efficiently

* optimal execution plans can be determined at compile time

### Using DataSets and DataFrames in scripts

Instead of creating a `SparkContext` we need to create a `SparkSession`.

* We can get a `SparkContext` from the `SparkSession`

* Remember to stop the session when done

### Examples for commonly used operations:

* `df.show()` - show the top 20 results

* `df.select("fieldName")` - extract a specific column from a dataframe

* `df.filter(df("fieldName") > 200)`

* `df.groupBy(df("fieldName")).mean()`

* `df.rdd().map(mapperFunction)`

* add a column with a certain operation:
    ```scala
    import org.apache.spark.sql.functions.udf 
    
    val df: DataFrame = ???  // some dataframe
    val square = (x => x*x)
    val squaredDf = df.withColumn("square", square('value'))
    ```

## Section 5: MLLIB

Sparks machine Learning library (MLLIB) can be used for:

* Feature extraction

* Basic statistics

* Linear Regression, Logistic Regression

*  Support Vector Machine

* Naive Bayes Classifier

* Decision Trees

and much more...

## Section 6: Spark Streaming

### Intro to Spark Streaming

Spark Stream is a tool used to analyze streams of data as it comes in real time.

Data is aggregated and analyzed at some given interval

Can take data fed to some port, Amazon Kinesis, HDFS, Kafka, and others.

"Checkpointing" stores state to disk periodically for fault tolerance.  

A `DStream` object breaks up the stream into distinct RDD's

"Windowed Operations" can combine results from multiple batches over some slide window:

* `window`
* `reduceByWindow`
* `reduceByKeyAndWindow`

`updateStateByKey` maintains a state across many batches as time goes on

### Structured Streaming

Uses DataSets as its primary API.

We can think of it as a DataSet that keeps getting appended to forever, and we can query it whenever.

```scala
val inputDF = spark.readStream.json("s3://logs")
inputDf.groupBy($"action", window($"time", "1 hour")).count()
.writeStream.format("jdbc").start("jdbc://mysql/...")
```


## Terminology

* RDD - Resilient Distributed Dataset - a spark construct or a little mini databse of infromation

    ```scala
    val rdd = sc.textFile("README.md")
    ``` 

* Shuffle Operations - operations that require spark to "push data around" on the cluster which can be really expensive.
Typically we would want to minimize shuffle operations.
 
* Broadcast Variable - a certain object (can be a scala class or data structure), that is available (in-memory) to each
node in the cluster. 

* Accumulator - a shared object across the entire spark cluster that maintains a state (for example count) that allows 
all executors to increment a shared variable across the whole cluster in a thread-safe way.

* DataFrames (`DataSet[Row]`) a data structure used by spark to perform operations on structured data 