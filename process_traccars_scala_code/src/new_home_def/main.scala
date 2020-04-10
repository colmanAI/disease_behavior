import java.time.{LocalDate, LocalTime, ZonedDateTime}
import java.io.{File, PrintWriter}

import scala.collection.mutable.Map
import il.ac.colman.disease.{DataPoint, DeviceId, Latitude, Longitude, NIGHT_END, NIGHT_START}
import org.opensextant.geodesy.{Geodetic2DPoint, Latitude => GeodeticLatitude, Longitude => GeodeticLongitude}

import scala.collection.mutable.ListBuffer

/**
  * Created by djoff on 21/10/2019.
  */
package object new_home_def {
//object main extends App{

  def medianCalculator(seq: Seq[Double]): Double= {
    //In order if you are not sure that 'seq' is sorted
    val sortedSeq = seq.sortWith(_ < _)

    if (seq.size % 2 == 1) sortedSeq(sortedSeq.size / 2)
    else {
      val (up, down) = sortedSeq.splitAt(seq.size / 2)
      (up.last + down.head) / 2
    }
  }


  def _getMedianDataPoint(dataPoints: Iterable[il.ac.colman.disease.DataPoint]): il.ac.colman.disease.DataPoint = {
    // gets a list of data points and returns one point - the median
    val lons: ListBuffer[Longitude] = new ListBuffer[Longitude]
    val lats: ListBuffer[Latitude] = new ListBuffer[Latitude]
    for (dataPoint <- dataPoints) {
      lons += dataPoint.rawLongitude
      lats += dataPoint.rawLatitude
    }
    val medianLon = medianCalculator(lons)
    val medianLat = medianCalculator(lats)
    val dp = dataPoints.head
    il.ac.colman.disease.DataPoint(
      dp.deviceId,
      dp.email,
      dp.dateTime,
      0, // speed
      0, // distance
      0, // totalDistance
      new Geodetic2DPoint(
        new GeodeticLongitude(Math.toRadians(medianLon)),
        new GeodeticLatitude(Math.toRadians(medianLat))),
      medianLat,
      medianLon
    )
  }


  def _newParOnePointPerNight(old_participant: ParticipantInfo): ParticipantInfo = {
    // create new ParticipantInfo
    var new_participant : ParticipantInfo = new ParticipantInfo(old_participant.id, old_participant.experiment)
    // for each night, calculate one representative point
    var nightPoints = ListBuffer[il.ac.colman.disease.DataPoint]()
    for (dataPoint <- old_participant.getDataPoints) {
      val date = dataPoint.dateTime.toLocalDate
      if (nightPoints.isEmpty || nightPoints.head.dateTime.toLocalDate == date) {
        nightPoints += dataPoint
      } else if (nightPoints.nonEmpty) {
        new_participant.addDataPoint(_getMedianDataPoint(nightPoints))
        nightPoints = ListBuffer[il.ac.colman.disease.DataPoint]()
        nightPoints += dataPoint
        val c = 0
      }
    }
    if (nightPoints.nonEmpty) {
      new_participant.addDataPoint(_getMedianDataPoint(nightPoints))
    }
    new_participant
  }





  /**
    * returns true if dataPoint is at night, false if its in day time.
    * @param dataPoint
    */
  def checkNight(dataPoint: il.ac.colman.disease.DataPoint): Boolean = {
    // not at night - nir's definition (works)
    if ((NIGHT_START.isAfter(LocalTime.of(12, 0, 0)) && (dataPoint.dateTime.toLocalTime.isBefore(NIGHT_START) && dataPoint.dateTime.toLocalTime.isAfter(NIGHT_END)))
      || (NIGHT_START.isBefore(LocalTime.of(12, 0, 0)) && (dataPoint.dateTime.toLocalTime.isBefore(NIGHT_START) || dataPoint.dateTime.toLocalTime.isAfter(NIGHT_END)))) {
      return false
    }
    true
  }

  /**
    *
    * @param rawData - all the data.
    * @param filter - function that tells if a dataPoint should be included in further calculations.
    * @return all data filtered by include func, organized in ParticipantInfo objects.
    */
  def divideByDeviceIDs(rawData : Seq[il.ac.colman.disease.DataPoint], filter:il.ac.colman.disease.DataPoint => Boolean, exp: Int) : List[ParticipantInfo] = {
    var allIDs = Map[DeviceId, Int]()
    var deviceIDs = Map[DeviceId, ParticipantInfo]()
    var id : DeviceId = 0
    for (dataPoint<- rawData) {
      if (!allIDs.contains(dataPoint.deviceId)) { allIDs += (dataPoint.deviceId -> 1) }

      // if dataPoint is at night
      if (filter(dataPoint)){
        id = dataPoint.deviceId
        if (!deviceIDs.contains(id)) {
          deviceIDs += (id -> new ParticipantInfo(id, exp))
        }
        deviceIDs(id).addDataPoint(dataPoint)
      }
    }
    print("Ids in experiment " + exp + ":\n")
    print(allIDs.keys.toList.sortBy(id=>id))
    println("")
    deviceIDs.values.toList.sortBy(participantsInfo => participantsInfo.id)
  }


  def printToFile_DataPointsPerNight(participants: Iterable[(String, Map[LocalDate, Int])]) : Unit ={
    for ((name, nights) <- participants) {
      //       for each night: how many points?
      val writer: PrintWriter = new PrintWriter(new File(name + ".txt"))
      for ( (night, am) <- nights) {
        writer.println(night.toString + ": " + am.toString)
      }
      writer.close()
    }
  }
}
