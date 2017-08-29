package ml.combust.mleap.runtime.transformer.classification

import ml.combust.mleap.core.classification.GBTClassifierModel
import ml.combust.mleap.runtime.test.TestUtil
import ml.combust.mleap.runtime.{LeapFrame, LocalDataset, Row}
import ml.combust.mleap.runtime.types.{DoubleType, StructField, StructType, TensorType}
import ml.combust.mleap.tensor.Tensor
import org.scalatest.FunSpec

/**
  * Created by hollinwilkins on 9/28/16.
  */
class GBTClassifierSpec extends FunSpec {

  describe("#transform") {
    val schema = StructType(Seq(StructField("features", TensorType(DoubleType())))).get
    val dataset = LocalDataset(Seq(Row(Tensor.denseVector(Array(0.2, 0.7, 0.4)))))
    val frame = LeapFrame(schema, dataset)
    val tree1 = TestUtil.buildDecisionTreeRegression(0.5, 0, goLeft = true)
    val tree2 = TestUtil.buildDecisionTreeRegression(0.75, 1, goLeft = false)
    val tree3 = TestUtil.buildDecisionTreeRegression(0.1, 2, goLeft = true)
    val gbt = GBTClassifier(featuresCol = "features",
      predictionCol = "prediction",
      model = GBTClassifierModel(Seq(tree1, tree2, tree3), Seq(0.5, 2.0, 1.0), 5))

    it("uses the GBT to make predictions on the features column") {
      val frame2 = gbt.transform(frame).get
      val prediction = frame2.dataset(0).getDouble(1)

      assert(prediction == 1.0)
    }

    describe("with invalid features column") {
      val gbt2 = gbt.copy(featuresCol = "bad_features")

      it("returns a Failure") { assert(gbt2.transform(frame).isFailure) }
    }
  }

  describe("#getFields") {
    it("has the correct inputs and outputs") {
      val gbt = GBTClassifier(featuresCol = "features", predictionCol = "prediction", model = null)
      assert(gbt.getFields().get ==
        Seq(StructField("features", TensorType(DoubleType())),
            StructField("prediction", DoubleType())))
    }

    it("has the correct inputs and outputs with probability column") {
      val gbt = GBTClassifier(featuresCol = "features", predictionCol = "prediction", model = null, probabilityCol = Some("probability"))
      assert(gbt.getFields().get ==
        Seq(StructField("features", TensorType(DoubleType())),
          StructField("probability", TensorType(DoubleType())),
          StructField("prediction", DoubleType())))
    }

    it("has the correct inputs and outputs with rawPrediction column") {
      val gbt = GBTClassifier(featuresCol = "features", predictionCol = "prediction", model = null, rawPredictionCol = Some("rawPrediction"))
      assert(gbt.getFields().get ==
        Seq(StructField("features", TensorType(DoubleType())),
          StructField("rawPrediction", TensorType(DoubleType())),
          StructField("prediction", DoubleType())))
    }

    it("has the correct inputs and outputs with both probability and rawPrediction columns") {
      val gbt = GBTClassifier(featuresCol = "features", predictionCol = "prediction", model = null, probabilityCol = Some("probability"), rawPredictionCol = Some("rawPrediction"))
      assert(gbt.getFields().get ==
        Seq(StructField("features", TensorType(DoubleType())),
          StructField("rawPrediction", TensorType(DoubleType())),
          StructField("probability", TensorType(DoubleType())),
          StructField("prediction", DoubleType())))
    }
  }
}
