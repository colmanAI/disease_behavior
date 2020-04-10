package il.ac.colman.disease

import java.io.{FileWriter, PrintWriter}
import java.time.{Duration, LocalDate, LocalTime, ZonedDateTime}
import org.opensextant.geodesy.{Geodetic2DPoint, Latitude => GeodeticLatitude, Longitude => GeodeticLongitude}

import new_home_def.HomeCalc
import org.opensextant.geodesy.Geodetic2DPoint

object main extends App {

//
//  val participants_with_no_night_data: List[(Integer,Integer)] =
//    List((41,1),(13,2),(48,3),(51,3),(55,3),(56,3),(60,3),(61,3),(68,3))
//  val participants_with_no_home: List[(Integer,Integer)] =
//    List((50,1),(4,2),(65,3),(40,3))
//  // take off amsterdam points of 23 (2)


//  var remove = Map[Int, scala.collection.mutable.Set[Int]]()// = Map.empty//Map(1-> Set(41, 50),
////    2-> Set(13, 4),
////    3-> Set(48, 51, 55, 56, 60, 61, 68, 65, 40))
//
//  for (p <- participants_with_no_night_data ++ participants_with_no_home) {
//    val exp: Int = p._2
//    val participant: Int = p._1
//    if (!remove.contains(exp)) {
//      remove += (exp -> scala.collection.mutable.Set[Int]())
//    }
//    remove(exp).+(participant)
//  }



  val TRACCAR1 = "traccar"
  val TRACCAR2 = "traccarV2"
  val TRACCAR3 = "traccar3"
  val traccar = TRACCAR3

  var rawData: Seq[DataPoint] = DAO.loadDataPoints(traccar)

  // filter points abroad (Dana added)
  rawData = rawData.filter(dp => dp.rawLatitude < 40)

  /* Pre-process participant data
   * In this step we extract basic information needed for the following steps, such as HOME
   */
//  val basicData: Map[DeviceId, BasicParticipantData] = preProcessData(rawData)

  val basicData: Map[DeviceId, BasicParticipantData] = new HomeCalc(rawData, traccar).get_basic_data()


  // NOTE: participants without night data are filtered out
  val validDevices = basicData.keySet

  // extract features
  val fullData = extractFeatures(rawData.filter(d => validDevices.contains(d.deviceId)), basicData)

  // save features
  // processedData contains participent => participent's data
  val writer: PrintWriter = new PrintWriter(new FileWriter("processedData.txt"))
  // for each participant - write his data in a new line
  fullData.foreach(writer.println)
  writer.close()

  // prints "insertim" to the screen
  DAO.saveFeatures(fullData.values)

  def preProcessData(rawData: Seq[DataPoint]): Map[DeviceId, BasicParticipantData] = {
    // NEED TO RE-WRITE THIS FUNCTION
    /**
      * this tail recursive function calculates ..... todo
      * arguments in first call are empty, except rawData.
      *
      * tail recursion is a recursive function that calls itself (or returns a value) as its last action.
      *
      * this function only deals with datapoints that were taken during the night.
      *
      * @param rawData - initially all the DataPoints, and in every all one is removed.
      * @param processedData - holds final answer
      * @param deviceId id of the current analysed participant.
      * @param nightLocations - a map of date(night)=>location. tells where the current participant have been in every night.
      * @param day - the current analysed day
      * @param nightVisits - all places the current participant visited during the night. night location will
      *                    be chosen from nightVisits. we are interested in the duration of the visit.
      * @param visitBuilder - builds the current visit
      * @param distance - the distance (in meters) we did this current visit.
      * @return
      */
    def process(rawData: Seq[DataPoint], processedData: Map[DeviceId, BasicParticipantData], deviceId: DeviceId,
                nightLocations: Map[LocalDate, Location], day: LocalDate, nightVisits: Seq[Visit],
                visitBuilder: Visit.Builder, distance: Meters): Map[DeviceId, BasicParticipantData] = {
      import Position.Implicits._
      // stop condition
      if (rawData.isEmpty) {
        if (visitBuilder.nonEmpty) {
          // :+ is concatenation to the start.
          // this call concatenates a new visitBuilder.build() to the start of nightVisits
          process(rawData, processedData, deviceId, nightLocations, day, visitBuilder.build() +: nightVisits, visitBuilder.clear(ZonedDateTime.now), distance = 0)
        } else if (nightVisits.nonEmpty) {
          val homeCandidate: Location = getHomeCandidate(nightVisits)
          process(rawData, processedData, deviceId, nightLocations + (day -> homeCandidate), day, nightVisits = Nil, visitBuilder, distance = 0)
        } else if (nightLocations.nonEmpty) {
          val (home, homePercentage) = getHomeOpt(nightLocations)
          val deviceData: (DeviceId, BasicParticipantData) = deviceId -> BasicParticipantData(deviceId, home, homePercentage, nightLocations.mapValues(BasicDailyData))
          process(rawData, processedData + deviceData, deviceId, nightLocations = Map.empty, day, nightVisits, visitBuilder, distance = 0)
        } else { // rawData visitBuilder, nightVisits, nightLocations - are all empty
          processedData
        }
      } else {
        // take a dataPoint from the beginning
        val dataPoint = rawData.head


//        // todo: DELETE
//        if (dataPoint.deviceId == 26 ) {
//          val dayy = dataPoint.dateTime.toLocalDate.getDayOfMonth
//          if (dayy == 2) {
//            val x = 0
//          }
//          if (dayy == 24) {
//            val x = 0
//          }
//          if (dayy == 28) {
//            val x = 0
//          }
//          if (dayy == 29) {
//            val x = 0
//          }
//          if (dayy == 26) {
//            val x = 0
//          }
//          if (dayy == 27) {
//            val x = 0
//          }
//          val x = 0
//        }



        // if this call handles the same deviceId as the last call
        if (dataPoint.deviceId == deviceId) {
          // we just finished handling a visit / a visit hasn't start yet.
          if (visitBuilder.isEmpty) {
            // after 12 = 12 to 23
            // before 12 = 00-11:59
            // if we are not at night (we are at day time) - than ignore the data point
            if ((NIGHT_START.isAfter(LocalTime.of(12, 0, 0)) && (dataPoint.dateTime.toLocalTime.isBefore(NIGHT_START) && dataPoint.dateTime.toLocalTime.isAfter(NIGHT_END)))
              || (NIGHT_START.isBefore(LocalTime.of(12, 0, 0)) && (dataPoint.dateTime.toLocalTime.isBefore(NIGHT_START) || dataPoint.dateTime.toLocalTime.isAfter(NIGHT_END)))) {
              process(rawData.tail, processedData, deviceId, nightLocations, day, nightVisits, visitBuilder, distance)
            }
            // if we are analysing the same night as the last call
              else if (dataPoint.dateTime.toLocalDate.isEqual(day)
              || (NIGHT_START.isAfter(LocalTime.of(12, 0, 0)) && dataPoint.dateTime.toLocalTime.isBefore(NIGHT_END) && dataPoint.dateTime.toLocalDate.isEqual(day.plusDays(1)))) {
              process(rawData.tail, processedData, deviceId, nightLocations, day, nightVisits, visitBuilder.add(dataPoint), distance = 0)
            } else if (nightVisits.nonEmpty) {
              val homeCandidate: Location = getHomeCandidate(nightVisits)
              process(rawData.tail, processedData, deviceId, nightLocations + (day -> homeCandidate), dataPoint.dateTime.toLocalDate, nightVisits = Nil, visitBuilder.add(dataPoint), distance = 0)
            } else {
              process(rawData.tail, processedData, deviceId, nightLocations, dataPoint.dateTime.toLocalDate, nightVisits = Nil, visitBuilder.add(dataPoint), distance = 0)
            }
          } else { // visitBuilder is not empty.
            // a visit can continue to the day time
            // TODO: check if still same day (on the video)
            //
            val newDistance: Meters = distance + dataPoint.position.distanceTo(visitBuilder.head)
            // 2 * HOME_RADIUS = visit's diameter. this way we are sure the positions are not leaving the Location of the visit.
            if (newDistance <= 2 * HOME_RADIUS) {
              process(rawData.tail, processedData, deviceId, nightLocations, day, nightVisits, visitBuilder.add(dataPoint), newDistance)
            } else {
              val radius = visitBuilder.calcRadius()
              if (radius <= HOME_RADIUS) {
                // changed radius parameter to be diameter = 2*radius
                process(rawData.tail, processedData, deviceId, nightLocations, day, nightVisits, visitBuilder.add(dataPoint), 2*radius)
              } else {
                // keep the current dataPoint in rawData, to be analysed in the next call
                process(rawData, processedData, deviceId, nightLocations, day, visitBuilder.build() +: nightVisits, visitBuilder.clear(dataPoint.dateTime), distance = 0)
              }
            }
          }
        }
        // dataPoint.deviceId != deviceId
        // this call handles a different deviceId than the last call.
        else if (visitBuilder.nonEmpty) {
          process(rawData, processedData, deviceId, nightLocations, day, visitBuilder.build() +: nightVisits, visitBuilder.clear(dataPoint.dateTime), distance = 0)
        } else if (nightVisits.nonEmpty) {
          val homeCandidate: Location = getHomeCandidate(nightVisits)
          process(rawData, processedData, deviceId, nightLocations + (day -> homeCandidate), day, nightVisits = Nil, visitBuilder, distance = 0)
        } else if (nightLocations.nonEmpty) {
          val (home, homePercentage) = getHomeOpt(nightLocations)
          val dailyData = nightLocations.map { case (day, nightLocation) => day -> BasicDailyData(nightLocation) }
          val deviceData: (DeviceId, BasicParticipantData) = deviceId -> BasicParticipantData(deviceId, home, homePercentage, dailyData)
          process(rawData, processedData + deviceData, dataPoint.deviceId, nightLocations = Map.empty, day, nightVisits, visitBuilder, distance = 0)
        } else {
          process(rawData, processedData, dataPoint.deviceId, nightLocations, day, nightVisits, visitBuilder, distance = 0)
        }
      }
    }

    process(rawData, Map.empty, rawData.head.deviceId, Map.empty, rawData.head.dateTime.toLocalDate, Nil, new Visit.Builder(rawData.head.dateTime), 0)
  }

  /**
    * calculates the nightLocation of a specific night is the location in which the participant spent the longest time,
    * from all the locations he've been in during that night.
    *
    * @param nightVisits - all the visits a participant had in a single night.
    * @return the night location: the location with the maximum duration, taken from nightVisits.
    */
  def getHomeCandidate(nightVisits: Seq[Visit]): Location = nightVisits.maxBy(_.duration).location

  /**
    * calculate home location by the following method:
    *   go over nightLocations and add together overlapping locations.
    *     two locations are considered 'overlapping' if The distance between their nearest Circumference points is <= HOME_RADIUS
    *     when two locations are added together, a new location is created, and the original two are disregarded.
    *   from the new composed locations, Home is the location composed of most of the nights.
    *
    * @param homeCandidates - map of night=>Location in which the participant spent the night.
    * @return home Location and it's night percentage (what percentage of nights the participant spent in this location).
    */
  def getHomeOpt(homeCandidates: Map[LocalDate, Location]): (Location, Double) = {
    // create a map of Location=>the number of locations from which the location is composed = number of nights in the location
    val candidatesMap = homeCandidates.values.foldLeft(Map.empty[Location, Int])((map, newLocation) => {
      // find in the map a single location that overlaps with the new location
      // two locations are overlapping if the distance between their Circumferences is <= HOME_RADIUS
      val entryOpt = map.find { case (location, _) => location.distanceBetweenEdges(newLocation) <= HOME_RADIUS }
      entryOpt match {
          // an overlapping location exists
          // n= the number of locations from which the location is composed
        case Some((location, n)) => {
          map - location + ((location + newLocation) -> (n + 1))
        }
          // an overlapping doesn't exist.
        case None => {
          map + (newLocation -> 1)
        }
      }
    })

    // home is the composed location that the participant spent most of the nights.
    val (home, count) = candidatesMap.maxBy { case (_, n) => n }
    (home, count.toDouble / homeCandidates.size)
  }

  /**
    * tail recurursive function
    * @param rawData - filtered rawData. contains only participants that have defined "home".
    * @param basicData - preProcessData output. map from device id to the basic participant information, including home.
    * @return
    */
  def extractFeatures(rawData: Seq[DataPoint], basicData: Map[DeviceId, BasicParticipantData]): Map[DeviceId, FullParticipantData] = {
    /**
      * assumption: rawData is organised be deviceId, and by chronological time.
      * @param rawData - filtered rawData. contains only participants that have defined "home".
      * @param lastDataPoint - the last dataPoint that we looked at
      * @param participantDataAccumulator - accumulator. the final output: a map between deviceID to full participant data
      * @param dailyDataAccumulator - accumulator. holds the map of FullDailyData for the current deviceID.
      * @param trips - accumulator of dailyData field.
      * @param totalDistance - accumulator of dailyData field.
      * @param totalWalkingDistance - accumulator of dailyData field.
      * @param totalDrivingDistance - accumulator of dailyData field.
      * @param maxTripDistance - accumulator of dailyData field.
      * @param maxTripDiameter - accumulator of dailyData field.
      * @param totalDuration - accumulator of dailyData field.
      * @param totalWalkingDuration - accumulator of dailyData field.
      * @param totalDrivingDuration - accumulator of dailyData field.
      * @param maxTripDuration - accumulator of dailyData field.
      * @param tripDistance - accumulator of dailyData field.
      * @param tripWalkingDistance - accumulator of dailyData field.
      * @param tripDrivingDistance - accumulator of dailyData field.
      * @param tripDuration - accumulator of dailyData field.
      * @param tripWalkingDuration - accumulator of dailyData field.
      * @param tripDrivingDuration - accumulator of dailyData field.
      * @param tripDiameter - accumulator of dailyData field.
      * @return participantDataAccumulator
      */
    def extract(rawData: Seq[DataPoint], lastDataPoint: DataPoint, participantDataAccumulator: Map[DeviceId, FullParticipantData],
                dailyDataAccumulator: Map[LocalDate, FullDailyData], trips: Int, totalDistance: Meters, totalWalkingDistance: Meters,
                totalDrivingDistance: Meters, maxTripDistance: Meters, maxTripDiameter: Meters, totalDuration: Duration,
                totalWalkingDuration: Duration, totalDrivingDuration: Duration, maxTripDuration: Duration, tripDistance: Meters,
                tripWalkingDistance: Meters, tripDrivingDistance: Meters, tripDuration: Duration, tripWalkingDuration: Duration,
                tripDrivingDuration: Duration, tripDiameter: Meters): Map[DeviceId, FullParticipantData] = {
      import Position.Implicits._
      // if finished going through all data points
      if (rawData.isEmpty) {
        if (trips > 0) {
          val home = basicData(lastDataPoint.deviceId).home
          val nightLocation = basicData(lastDataPoint.deviceId).dailyData(lastDataPoint.dateTime.toLocalDate).nightLocation
          val sleptHome = nightLocation.distanceBetweenEdges(home) <= HOME_RADIUS

          // Yoav's addition
          val averageWalkingSpeed = if (totalWalkingDistance != 0) totalWalkingDistance / (totalWalkingDuration.toNanos.toDouble / NANOS_IN_SECOND) else 0.0
          val averageDrivingSpeed = if(totalDrivingDistance != 0) totalDrivingDistance / (totalDrivingDuration.toNanos.toDouble / NANOS_IN_SECOND) else 0.0
          // end

          val dailyData = FullDailyData(trips, totalDistance, totalWalkingDistance, totalDrivingDistance, totalDistance / trips,
            maxTripDiameter, totalDuration, totalWalkingDuration, totalDrivingDuration, totalDuration.dividedBy(trips), maxTripDuration,
            averageWalkingSpeed, averageDrivingSpeed, sleptHome)
          extract(rawData, lastDataPoint, participantDataAccumulator, dailyDataAccumulator + (lastDataPoint.dateTime.toLocalDate -> dailyData),
            0, 0.0, 0.0, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0, 0.0, 0.0, Duration.ZERO,
            Duration.ZERO, Duration.ZERO, 0.0)
        } else if (dailyDataAccumulator.nonEmpty) {
          extract(rawData, lastDataPoint, participantDataAccumulator + (lastDataPoint.deviceId -> FullParticipantData(lastDataPoint.deviceId, dailyDataAccumulator)),
            Map.empty, 0, 0.0, 0.0, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0, 0.0, 0.0,
            Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0)
        } else {
          participantDataAccumulator
        }
      } else if (rawData.head.deviceId == lastDataPoint.deviceId) {
        if (rawData.head.dateTime.toLocalDate == lastDataPoint.dateTime.toLocalDate) {
          val home = basicData(lastDataPoint.deviceId).home
          val distanceFromHome = home.distanceFromCenter(rawData.head.position)
          val wasHome = home.distanceFromCenter(lastDataPoint.position) <= HOME_RADIUS
          val isHome = distanceFromHome <= HOME_RADIUS
          // still at home - no trip
          if (wasHome && isHome) {
            extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips, totalDistance,
              totalWalkingDistance, totalDrivingDistance, maxTripDistance, maxTripDiameter, totalDuration, totalWalkingDuration,
              totalDrivingDuration, maxTripDuration, tripDistance, tripWalkingDistance, tripDrivingDistance, tripDuration,
              tripWalkingDuration, totalDrivingDuration, tripDiameter)
          // start a trip now
          } else if (wasHome && !isHome) {
            val distance = lastDataPoint.position.distanceTo(rawData.head.position)
            val duration = Duration.between(lastDataPoint.dateTime, rawData.head.dateTime)
            val speed = distance / (duration.toNanos.toDouble / NANOS_IN_SECOND)
            // not walking, not driving
            if(duration.isZero || speed < MIN_WALKING_SPEED) {
              extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips, totalDistance,
                totalWalkingDistance, totalDrivingDistance, maxTripDistance, maxTripDiameter, totalDuration, totalWalkingDuration,
                totalDrivingDuration, maxTripDuration, distance, 0.0, 0.0, duration, Duration.ZERO, Duration.ZERO, distanceFromHome)
              // walking
            } else if(speed < MIN_DRIVING_SPEED) {
              extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips, totalDistance,
                totalWalkingDistance, totalDrivingDistance, maxTripDistance, maxTripDiameter, totalDuration, totalWalkingDuration,
                totalDrivingDuration, maxTripDuration, distance, distance, 0.0, duration, duration, Duration.ZERO, distanceFromHome)
              // driving
            } else {
              extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips, totalDistance,
                totalWalkingDistance, totalDrivingDistance, maxTripDistance, maxTripDiameter, totalDuration, totalWalkingDuration,
                totalDrivingDuration, maxTripDuration, distance, 0.0, distance, duration, Duration.ZERO, duration, distanceFromHome)
            }
          // finish a trip now
          } else if (!wasHome && isHome) {
            // a valid trip
            if (tripDuration.compareTo(TRIP_MINIMUM_DURATION) >= 0 && tripDistance >= TRIP_MINIMUM_DISTANCE) {
              val distance = lastDataPoint.position.distanceTo(rawData.head.position)
              val duration = Duration.between(lastDataPoint.dateTime, rawData.head.dateTime)
              val updatedTripDuration = tripDuration.plus(duration)
              val updatedTripDistance = tripDistance + distance
              val updatedMaxTripDuration = List(updatedTripDuration, maxTripDuration).maxBy(_.toNanos)
              val speed = distance / (duration.toNanos.toDouble / NANOS_IN_SECOND)
              // not walking, not driving
              if(duration.isZero || speed < MIN_WALKING_SPEED) {
                extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips + 1, totalDistance + updatedTripDistance,
                  totalWalkingDistance + tripWalkingDistance, totalDrivingDistance + tripDrivingDistance, Math.max(maxTripDistance, updatedTripDistance),
                  Math.max(maxTripDiameter, Math.max(tripDiameter, distanceFromHome)), totalDuration.plus(updatedTripDuration),
                  totalWalkingDuration.plus(tripWalkingDuration), totalDrivingDuration.plus(tripDrivingDuration), updatedMaxTripDuration,
                  0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0)
              // walking
              } else if(speed < MIN_DRIVING_SPEED) {
                extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips + 1, totalDistance + updatedTripDistance,
                  totalWalkingDistance + tripWalkingDistance + distance, totalDrivingDistance + tripDrivingDistance, Math.max(maxTripDistance, updatedTripDistance),
                  Math.max(maxTripDiameter, Math.max(tripDiameter, distanceFromHome)), totalDuration.plus(updatedTripDuration),
                  totalWalkingDuration.plus(tripWalkingDuration).plus(duration), totalDrivingDuration.plus(tripDrivingDuration),
                  updatedMaxTripDuration, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0)
              // driving
              } else {
                extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips + 1, totalDistance + updatedTripDistance,
                  totalWalkingDistance + tripWalkingDistance, totalDrivingDistance + tripDrivingDistance + distance, Math.max(maxTripDistance, updatedTripDistance),
                  Math.max(maxTripDiameter, Math.max(tripDiameter, distanceFromHome)), totalDuration.plus(updatedTripDuration),
                  totalWalkingDuration.plus(tripWalkingDuration), totalDrivingDuration.plus(tripDrivingDuration).plus(duration),
                  updatedMaxTripDuration, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0)
              }
            // not a real trip; this is noise
            } else { // tripDuration.compareTo(TRIP_MINIMUM_DURATION) < 0 || tripDistance < TRIP_MINIMUM_DISTANCE
              extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips, totalDistance,
                totalWalkingDistance, totalDrivingDistance, maxTripDistance, maxTripDiameter, totalDuration, totalWalkingDuration,
                totalDrivingDuration, maxTripDuration, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0)
            }
          // still on trip
          } else { // !wasHome && !isHome
            val distance = lastDataPoint.position.distanceTo(rawData.head.position)
            val duration = Duration.between(lastDataPoint.dateTime, rawData.head.dateTime)
            val speed = distance / (duration.toNanos.toDouble / NANOS_IN_SECOND)
            if(duration.isZero || speed < MIN_WALKING_SPEED) {
              extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips, totalDistance,
                totalWalkingDistance, totalDrivingDistance, maxTripDistance, maxTripDiameter, totalDuration, totalWalkingDuration,
                totalDrivingDuration, maxTripDuration, tripDistance + distance, tripWalkingDistance, tripDrivingDistance,
                tripDuration.plus(duration), tripWalkingDuration, tripDrivingDuration, Math.max(tripDiameter, distanceFromHome))
            } else if(speed < MIN_DRIVING_SPEED) {
              extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips, totalDistance,
                totalWalkingDistance, totalDrivingDistance, maxTripDistance, maxTripDiameter, totalDuration, totalWalkingDuration,
                totalDrivingDuration, maxTripDuration, tripDistance + distance, tripWalkingDistance + distance, tripDrivingDistance,
                tripDuration.plus(duration), tripWalkingDuration.plus(duration), tripDrivingDuration, Math.max(tripDiameter, distanceFromHome))
            } else {
              extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips, totalDistance,
                totalWalkingDistance, totalDrivingDistance, maxTripDistance, maxTripDiameter, totalDuration, totalWalkingDuration,
                totalDrivingDuration, maxTripDuration, tripDistance + distance, tripWalkingDistance, tripDrivingDistance + distance,
                tripDuration.plus(duration), tripWalkingDuration, tripDrivingDuration.plus(duration), Math.max(tripDiameter, distanceFromHome))
            }
          }
        } else if (trips > 0) { // rawData.head.dateTime.toLocalDate != lastDataPoint.dateTime.toLocalDate
          val home = basicData(lastDataPoint.deviceId).home
          val nightLocationOpt = basicData(lastDataPoint.deviceId).dailyData.get(lastDataPoint.dateTime.toLocalDate).map(_.nightLocation)
          val sleptHome = nightLocationOpt.exists(_.distanceBetweenEdges(home) <= HOME_RADIUS)

          // Yoav's addition
          val averageWalkingSpeed = if (totalWalkingDistance != 0) totalWalkingDistance / (totalWalkingDuration.toNanos.toDouble / NANOS_IN_SECOND) else 0.0
          val averageDrivingSpeed = if(totalWalkingDistance != 0) totalWalkingDistance / (totalDrivingDuration.toNanos.toDouble / NANOS_IN_SECOND) else 0.0
          // end

          val dailyData = FullDailyData(trips, totalDistance, totalWalkingDistance, totalDrivingDistance, totalDistance / trips,
            maxTripDiameter, totalDuration, totalWalkingDuration, totalDrivingDuration, totalDuration.dividedBy(trips),
            maxTripDuration, averageWalkingSpeed, averageDrivingSpeed, sleptHome)
          extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator + (lastDataPoint.dateTime.toLocalDate -> dailyData),
            0, 0.0, 0.0, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0)
        } else {
          extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, 0, 0.0, 0.0, 0.0, 0.0, 0.0,
            Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0)
        }
      } else if (trips > 0) { // rawData.head.deviceId != lastDataPoint.deviceId
        val home = basicData(lastDataPoint.deviceId).home
        val nightLocationOpt = basicData(lastDataPoint.deviceId).dailyData.get(lastDataPoint.dateTime.toLocalDate).map(_.nightLocation)
        val sleptHome = nightLocationOpt.exists(_.distanceBetweenEdges(home) <= HOME_RADIUS)
        val totalDistance = totalWalkingDistance + totalDrivingDistance
        val totalDuration = totalWalkingDuration.plus(totalDrivingDuration)

//      Yoav's addition
        val averageWalkingSpeed = if (totalWalkingDistance != 0) totalWalkingDistance / (totalWalkingDuration.toNanos.toDouble / NANOS_IN_SECOND) else 0.0
        val averageDrivingSpeed = if(totalWalkingDistance != 0) totalWalkingDistance / (totalDrivingDuration.toNanos.toDouble / NANOS_IN_SECOND) else 0.0
//        end

        val dailyData = FullDailyData(trips, totalDistance, totalWalkingDistance, totalDrivingDistance, totalDistance / trips,
          maxTripDiameter, totalDuration, totalWalkingDuration, totalDrivingDuration, totalDuration.dividedBy(trips), maxTripDuration,
          averageWalkingSpeed, averageDrivingSpeed, sleptHome)
        extract(rawData, rawData.head, participantDataAccumulator + (lastDataPoint.deviceId -> FullParticipantData(lastDataPoint.deviceId, dailyDataAccumulator + (lastDataPoint.dateTime.toLocalDate -> dailyData))),
          Map.empty, 0, 0.0, 0.0, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0, 0.0, 0.0,
          Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0)
      } else {
        extract(rawData, rawData.head, participantDataAccumulator + (lastDataPoint.deviceId -> FullParticipantData(lastDataPoint.deviceId, dailyDataAccumulator)),
          Map.empty, 0, 0.0, 0.0, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0, 0.0, 0.0,
          Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0)
      }
    }

    extract(rawData, rawData.head, Map.empty, Map.empty, 0, 0.0, 0.0, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO,
      Duration.ZERO, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0)
  }
}
