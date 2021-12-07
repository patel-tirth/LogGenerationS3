/*
 *
 *  Copyright (c) 2021. Mark Grechanik and Lone Star Consulting, Inc. All rights reserved.
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under
 *   the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *   either express or implied.  See the License for the specific language governing permissions and limitations under the License.
 *
 */

import Generation.{LogMsgSimulator, RandomStringGenerator}
import HelperUtils.{CreateLogger, Parameters}
import com.amazonaws.AmazonServiceException
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.typesafe.config.ConfigFactory

import java.io.File
import java.nio.file.Paths
import java.text.SimpleDateFormat
import collection.JavaConverters.*
import scala.concurrent.{Await, Future, duration}
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import java.util.UUID.randomUUID

object GenerateLogData:
  val setLogFileName = "i-0edc8328304543e05-" + randomUUID().toString+".log"
  System.setProperty("LOGFILENAME",setLogFileName)
  val logger = CreateLogger(classOf[GenerateLogData.type])

//this is the main starting point for the log generator
  def main(args: Array[String]): Unit =
    import Generation.RSGStateMachine.*
    import Generation.*
    import HelperUtils.Parameters.*
    import GenerateLogData.*

    logger.info("Log data generator started...")
    val INITSTRING = "Starting the string generation"
    val init = unit(INITSTRING)

    val logFuture = Future {
      LogMsgSimulator(init(RandomStringGenerator((Parameters.minStringLength, Parameters.maxStringLength), Parameters.randomSeed)), Parameters.maxCount)
    }
    Try(Await.result(logFuture, Parameters.runDurationInMinutes)) match {
      case Success(value) => logger.info(s"Log data generation has completed after generating ${Parameters.maxCount} records.")
      case Failure(exception) => logger.info(s"Log data generation has completed within the allocated time, ${Parameters.runDurationInMinutes}")
    }
    val config = ConfigFactory.load()
    val bucket_name: String = config.getString("s3.bucket")
//    val file_path: String = config.getString("s3.file_path")
//    val key_name: String = config.getString("s3.key")

    val s3: AmazonS3 = AmazonS3ClientBuilder.standard.withRegion(Regions.US_EAST_1).build

    try s3.putObject(bucket_name, setLogFileName, new File("./log/"+setLogFileName))

    catch {
      case e: AmazonServiceException =>
        System.err.println(e)
        System.exit(1)
    }