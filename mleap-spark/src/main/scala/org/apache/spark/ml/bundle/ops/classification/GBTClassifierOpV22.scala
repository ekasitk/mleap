package org.apache.spark.ml.bundle.ops.classification

import ml.combust.bundle.BundleContext
import ml.combust.bundle.dsl._
import ml.combust.bundle.op.{OpModel, OpNode}
import ml.combust.bundle.serializer.ModelSerializer
import org.apache.spark.ml.bundle.SparkBundleContext
import org.apache.spark.ml.classification.GBTClassificationModel
import org.apache.spark.ml.regression.DecisionTreeRegressionModel

/**
  * Created by hollinwilkins on 9/24/16.
  */
class GBTClassifierOpV22 extends OpNode[SparkBundleContext, GBTClassificationModel, GBTClassificationModel] {
  override val Model: OpModel[SparkBundleContext, GBTClassificationModel] = new OpModel[SparkBundleContext, GBTClassificationModel] {
    override val klazz: Class[GBTClassificationModel] = classOf[GBTClassificationModel]

    override def opName: String = Bundle.BuiltinOps.classification.gbt_classifier

    override def store(model: Model, obj: GBTClassificationModel)
                      (implicit context: BundleContext[SparkBundleContext]): Model = {
      var i = 0
      val trees = obj.trees.map {
        tree =>
          val name = s"tree$i"
          ModelSerializer(context.bundleContext(name)).write(tree).get
          i = i + 1
          name
      }
      model.withAttr("num_features", Value.long(obj.numFeatures)).
        withAttr("num_classes", Value.long(2)).
        withAttr("tree_weights", Value.doubleList(obj.treeWeights)).
        withAttr("trees", Value.stringList(trees)).
        withAttr("thresholds", obj.get(obj.thresholds).map(Value.doubleList(_)))
    }

    override def load(model: Model)
                     (implicit context: BundleContext[SparkBundleContext]): GBTClassificationModel = {
      if (model.value("num_classes").getLong != 2) {
        throw new IllegalArgumentException("MLeap only supports binary logistic regression")
      }

      val numFeatures = model.value("num_features").getLong.toInt
      val treeWeights = model.value("tree_weights").getDoubleList.toArray

      val models = model.value("trees").getStringList.map {
        tree => ModelSerializer(context.bundleContext(tree)).read().get.asInstanceOf[DecisionTreeRegressionModel]
      }.toArray

      val gbt = new GBTClassificationModel(uid = "",
        _trees = models,
        _treeWeights = treeWeights,
        numFeatures = numFeatures)

      model.getValue("thresholds")
        .map(t => gbt.setThresholds(t.getDoubleList.toArray))
        .getOrElse(gbt)
    }
  }

  override val klazz: Class[GBTClassificationModel] = classOf[GBTClassificationModel]

  override def name(node: GBTClassificationModel): String = node.uid

  override def model(node: GBTClassificationModel): GBTClassificationModel = node

  override def load(node: Node, model: GBTClassificationModel)
                   (implicit context: BundleContext[SparkBundleContext]): GBTClassificationModel = {
    val gbt = new GBTClassificationModel(uid = node.name,
      _trees = model.trees,
      _treeWeights = model.treeWeights,
      numFeatures = model.numFeatures).
      setFeaturesCol(node.shape.input("features").name).
      setPredictionCol(node.shape.output("prediction").name)

    node.shape.getOutput("rawPrediction").map(rp => gbt.setRawPredictionCol(rp.name))
    node.shape.getOutput("probability").map(p => gbt.setProbabilityCol(p.name))
    gbt
  }

  override def shape(node: GBTClassificationModel): Shape = {
    val rawPrediction = if(node.isDefined(node.rawPredictionCol)) Some(node.getRawPredictionCol) else None
    val probability = if(node.isDefined(node.probabilityCol)) Some(node.getProbabilityCol) else None

    Shape().withInput(node.getFeaturesCol, "features").
      withOutput(node.getPredictionCol, "prediction").
      withOutput(rawPrediction, "rawPrediction").
      withOutput(probability, "probability")
  }

}
