package il.ac.colman.disease

import java.io.{FileWriter, PrintWriter}
import java.time.{Duration, LocalDate, LocalTime, ZonedDateTime}

object main extends App {
  val rawData: Seq[DataPoint] = DAO.loadDataPoints()

  /* Pre-process participant data
   * In this step we extract basic information needed for the following steps
   */
  val basicData: Map[DeviceId, BasicParticipantData] = preProcessData(rawData)

  // extract features
  val validDevices = basicData.keySet
  val fullData = extractFeatures(rawData.filter(d => validDevices.contains(d.deviceId)), basicData)

  // save features
  val writer: PrintWriter = new PrintWriter(new FileWriter("processedData.txt"))
  fullData.foreach(writer.println)
  writer.close()

  DAO.saveFeatures(fullData.values)

  def preProcessData(rawData: Seq[DataPoint]): Map[DeviceId, BasicParticipantData] = {
    def process(rawData: Seq[DataPoint], processedData: Map[DeviceId, BasicParticipantData], deviceId: DeviceId,
                nightLocations: Map[LocalDate, Location], day: LocalDate, nightVisits: Seq[Visit],
                visitBuilder: Visit.Builder, distance: Meters): Map[DeviceId, BasicParticipantData] = {
      import Position.Implicits._

      if (rawData.isEmpty) {
        if (visitBuilder.nonEmpty) {
          process(rawData, processedData, deviceId, nightLocations, day, visitBuilder.build() +: nightVisits, visitBuilder.clear(ZonedDateTime.now), distance = 0)
        } else if (nightVisits.nonEmpty) {
          val homeCandidate: Location = getHomeCandidate(nightVisits)
          process(rawData, processedData, deviceId, nightLocations + (day -> homeCandidate), day, nightVisits = Nil, visitBuilder, distance = 0)
        } else if (nightLocations.nonEmpty) {
          val (home, homePercentage) = getHomeOpt(nightLocations)
          val deviceData: (DeviceId, BasicParticipantData) = deviceId -> BasicParticipantData(deviceId, home, homePercentage, nightLocations.mapValues(BasicDailyData))
          process(rawData, processedData + deviceData, deviceId, nightLocations = Map.empty, day, nightVisits, visitBuilder, distance = 0)
        } else {
          processedData
        }
      } else {
        val dataPoint = rawData.head
        if (dataPoint.deviceId == deviceId) {
          if (visitBuilder.isEmpty) {
            if ((NIGHT_START.isAfter(LocalTime.of(12, 0, 0)) && (dataPoint.dateTime.toLocalTime.isBefore(NIGHT_START) && dataPoint.dateTime.toLocalTime.isAfter(NIGHT_END)))
              | (NIGHT_START.isBefore(LocalTime.of(12, 0, 0)) && (dataPoint.dateTime.toLocalTime.isBefore(NIGHT_START) || dataPoint.dateTime.toLocalTime.isAfter(NIGHT_END)))) {
              process(rawData.tail, processedData, deviceId, nightLocations, day, nightVisits, visitBuilder, distance)
            } else if (dataPoint.dateTime.toLocalDate.isEqual(day)
              | (NIGHT_START.isAfter(LocalTime.of(12, 0, 0)) && dataPoint.dateTime.toLocalTime.isBefore(NIGHT_END) && dataPoint.dateTime.toLocalDate.isEqual(day.plusDays(1)))) {
              process(rawData.tail, processedData, deviceId, nightLocations, day, nightVisits, visitBuilder.add(dataPoint), distance = 0)
            } else if (nightVisits.nonEmpty) {
              val homeCandidate: Location = getHomeCandidate(nightVisits)
              process(rawData.tail, processedData, deviceId, nightLocations + (day -> homeCandidate), dataPoint.dateTime.toLocalDate, nightVisits = Nil, visitBuilder.add(dataPoint), distance = 0)
            } else {
              process(rawData.tail, processedData, deviceId, nightLocations, dataPoint.dateTime.toLocalDate, nightVisits = Nil, visitBuilder.add(dataPoint), distance = 0)
            }
          } else {
            val newDistance: Meters = distance + dataPoint.position.distanceTo(visitBuilder.head)
            if (newDistance <= 2 * HOME_RADIUS) {
              process(rawData.tail, processedData, deviceId, nightLocations, day, nightVisits, visitBuilder.add(dataPoint), newDistance)
            } else {
              val radius = visitBuilder.calcRadius()
              if (radius <= HOME_RADIUS) {
                process(rawData.tail, processedData, deviceId, nightLocations, day, nightVisits, visitBuilder.add(dataPoint), radius)
              } else {
                process(rawData, processedData, deviceId, nightLocations, day, visitBuilder.build() +: nightVisits, visitBuilder.clear(dataPoint.dateTime), distance = 0)
              }
            }
          }
        } else if (visitBuilder.nonEmpty) { // dataPoint.deviceId != deviceId
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

  def getHomeCandidate(nightVisits: Seq[Visit]): Location = nightVisits.maxBy(_.duration).location

  def getHomeOpt(homeCandidates: Map[LocalDate, Location]): (Location, Double) = {
    val candidatesMap = homeCandidates.values.foldLeft(Map.empty[Location, Int])((map, newLocation) => {
      val entryOpt = map.find { case (location, _) => location.distanceBetweenEdges(newLocation) <= HOME_RADIUS }
      entryOpt match {
        case Some((location, n)) => {
          map - location + ((location + newLocation) -> (n + 1))
        }
        case None => {
          map + (newLocation -> 1)
        }
      }
    })

    val (home, count) = candidatesMap.maxBy { case (_, n) => n }
    (home, count.toDouble / homeCandidates.size)
  }

  def extractFeatures(rawData: Seq[DataPoint], basicData: Map[DeviceId, BasicParticipantData]): Map[DeviceId, FullParticipantData] = {
    def extract(rawData: Seq[DataPoint], lastDataPoint: DataPoint, participantDataAccumulator: Map[DeviceId, FullParticipantData],
                dailyDataAccumulator: Map[LocalDate, FullDailyData], trips: Int, totalDistance: Meters, totalWalkingDistance: Meters,
                totalDrivingDistance: Meters, maxTripDistance: Meters, maxTripDiameter: Meters, totalDuration: Duration,
                totalWalkingDuration: Duration, totalDrivingDuration: Duration, maxTripDuration: Duration, tripDistance: Meters,
                tripWalkingDistance: Meters, tripDrivingDistance: Meters, tripDuration: Duration, tripWalkingDuration: Duration,
                tripDrivingDuration: Duration, tripDiameter: Meters): Map[DeviceId, FullParticipantData] = {
      import Position.Implicits._

      if (rawData.isEmpty) {
        if (trips > 0) {
          val home = basicData(lastDataPoint.deviceId).home
          val nightLocation = basicData(lastDataPoint.deviceId).dailyData(lastDataPoint.dateTime.toLocalDate).nightLocation
          val sleptHome = nightLocation.distanceBetweenEdges(home) <= HOME_RADIUS
          val averageWalkingSpeed = if (totalWalkingDistance != 0) totalWalkingDistance / (totalWalkingDuration.toNanos.toDouble / NANOS_IN_SECOND) else 0.0
          val averageDrivingSpeed = if(totalDrivingDistance != 0) totalDrivingDistance / (totalDrivingDuration.toNanos.toDouble / NANOS_IN_SECOND) else 0.0
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
          if (wasHome && isHome) {
            extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips, totalDistance,
              totalWalkingDistance, totalDrivingDistance, maxTripDistance, maxTripDiameter, totalDuration, totalWalkingDuration,
              totalDrivingDuration, maxTripDuration, tripDistance, tripWalkingDistance, tripDrivingDistance, tripDuration,
              tripWalkingDuration, totalDrivingDuration, tripDiameter)
          } else if (wasHome && !isHome) {
            val distance = lastDataPoint.position.distanceTo(rawData.head.position)
            val duration = Duration.between(lastDataPoint.dateTime, rawData.head.dateTime)
            val speed = distance / (duration.toNanos.toDouble / NANOS_IN_SECOND)
            if(duration.isZero || speed < MIN_WALKING_SPEED) {
              extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips, totalDistance,
                totalWalkingDistance, totalDrivingDistance, maxTripDistance, maxTripDiameter, totalDuration, totalWalkingDuration,
                totalDrivingDuration, maxTripDuration, distance, 0.0, 0.0, duration, Duration.ZERO, Duration.ZERO, distanceFromHome)
            } else if(speed < MIN_DRIVING_SPEED) {
              extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips, totalDistance,
                totalWalkingDistance, totalDrivingDistance, maxTripDistance, maxTripDiameter, totalDuration, totalWalkingDuration,
                totalDrivingDuration, maxTripDuration, distance, distance, 0.0, duration, duration, Duration.ZERO, distanceFromHome)
            } else {
              extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips, totalDistance,
                totalWalkingDistance, totalDrivingDistance, maxTripDistance, maxTripDiameter, totalDuration, totalWalkingDuration,
                totalDrivingDuration, maxTripDuration, distance, 0.0, distance, duration, Duration.ZERO, duration, distanceFromHome)
            }
          } else if (!wasHome && isHome) {
            if (tripDuration.compareTo(TRIP_MINIMUM_DURATION) >= 0 && tripDistance >= TRIP_MINIMUM_DISTANCE) {
              val distance = lastDataPoint.position.distanceTo(rawData.head.position)
              val duration = Duration.between(lastDataPoint.dateTime, rawData.head.dateTime)
              val updatedTripDuration = tripDuration.plus(duration)
              val updatedTripDistance = tripDistance + distance
              val updatedMaxTripDuration = List(updatedTripDuration, maxTripDuration).maxBy(_.toNanos)
              val speed = distance / (duration.toNanos.toDouble / NANOS_IN_SECOND)
              if(duration.isZero || speed < MIN_WALKING_SPEED) {
                extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips + 1, totalDistance + updatedTripDistance,
                  totalWalkingDistance + tripWalkingDistance, totalDrivingDistance + tripDrivingDistance, Math.max(maxTripDistance, updatedTripDistance),
                  Math.max(maxTripDiameter, Math.max(tripDiameter, distanceFromHome)), totalDuration.plus(updatedTripDuration),
                  totalWalkingDuration.plus(tripWalkingDuration), totalDrivingDuration.plus(tripDrivingDuration), updatedMaxTripDuration,
                  0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0)
              } else if(speed < MIN_DRIVING_SPEED) {
                extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips + 1, totalDistance + updatedTripDistance,
                  totalWalkingDistance + tripWalkingDistance + distance, totalDrivingDistance + tripDrivingDistance, Math.max(maxTripDistance, updatedTripDistance),
                  Math.max(maxTripDiameter, Math.max(tripDiameter, distanceFromHome)), totalDuration.plus(updatedTripDuration),
                  totalWalkingDuration.plus(tripWalkingDuration).plus(duration), totalDrivingDuration.plus(tripDrivingDuration),
                  updatedMaxTripDuration, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0)
              } else {
                extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips + 1, totalDistance + updatedTripDistance,
                  totalWalkingDistance + tripWalkingDistance, totalDrivingDistance + tripDrivingDistance + distance, Math.max(maxTripDistance, updatedTripDistance),
                  Math.max(maxTripDiameter, Math.max(tripDiameter, distanceFromHome)), totalDuration.plus(updatedTripDuration),
                  totalWalkingDuration.plus(tripWalkingDuration), totalDrivingDuration.plus(tripDrivingDuration).plus(duration),
                  updatedMaxTripDuration, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0)
              }
            } else { // tripDuration.compareTo(TRIP_MINIMUM_DURATION) < 0 || tripDistance < TRIP_MINIMUM_DISTANCE
              extract(rawData.tail, rawData.head, participantDataAccumulator, dailyDataAccumulator, trips, totalDistance,
                totalWalkingDistance, totalDrivingDistance, maxTripDistance, maxTripDiameter, totalDuration, totalWalkingDuration,
                totalDrivingDuration, maxTripDuration, 0.0, 0.0, 0.0, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0)
            }
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
          val averageWalkingSpeed = if (totalWalkingDistance != 0) totalWalkingDistance / (totalWalkingDuration.toNanos.toDouble / NANOS_IN_SECOND) else 0.0
          val averageDrivingSpeed = if(totalWalkingDistance != 0) totalWalkingDistance / (totalDrivingDuration.toNanos.toDouble / NANOS_IN_SECOND) else 0.0
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
        val averageWalkingSpeed = if (totalWalkingDistance != 0) totalWalkingDistance / (totalWalkingDuration.toNanos.toDouble / NANOS_IN_SECOND) else 0.0
        val averageDrivingSpeed = if(totalWalkingDistance != 0) totalWalkingDistance / (totalDrivingDuration.toNanos.toDouble / NANOS_IN_SECOND) else 0.0
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
