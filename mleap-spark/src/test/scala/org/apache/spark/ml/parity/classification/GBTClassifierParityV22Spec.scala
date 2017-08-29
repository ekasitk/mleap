package org.apache.spark.ml.parity.classification

import org.apache.spark.ml.classification.GBTClassifier
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.ml.parity.SparkParityBase
import org.apache.spark.ml.{Pipeline, Transformer}
import org.apache.spark.sql._

/**
  * Created by hollinwilkins on 10/30/16.
  */
class GBTClassifierParityV22Spec extends SparkParityBase {
  override val dataset: DataFrame = baseDataset.select("fico_score_group_fnl", "dti", "approved")
  override val sparkTransformer: Transformer = new Pipeline().setStages(Array(new StringIndexer().
    setInputCol("fico_score_group_fnl").
    setOutputCol("fico_index"),
    new VectorAssembler().
      setInputCols(Array("fico_index", "dti")).
      setOutputCol("features"),
    new StringIndexer().
      setInputCol("approved").
      setOutputCol("label"),
    new GBTClassifier().
      setFeaturesCol("features").
      setLabelCol("label").
      setThresholds(Array(1.0, 1.0)).
      setProbabilityCol("myProbability").
      setPredictionCol("myPrediction").
      setRawPredictionCol("myRawPrediction")
  )).fit(dataset)
}
