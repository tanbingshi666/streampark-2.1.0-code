import org.apache.streampark.common.util.DeflaterUtils

object Test {

  def main(args: Array[String]): Unit = {

    val config: String = "D:\\project\\streampark\\streampark-2.1.0-code\\quickstart-flink\\quickstart-datastream\\datastream_1.16\\assembly\\conf\\application.yml"
    val content = DeflaterUtils.unzipString(config.drop(7))

    println(content)
  }

}
