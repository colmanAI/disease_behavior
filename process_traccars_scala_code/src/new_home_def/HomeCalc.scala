package new_home_def

import java.time.LocalDate

import il.ac.colman.disease.{BasicDailyData, BasicParticipantData, DataPoint, DeviceId, HOME_RADIUS, Location, Position}
import org.opensextant.geodesy.{Geodetic2DPoint, Latitude => GeodeticLatitude, Longitude => GeodeticLongitude}

/**
  * Created by djoff on 08/09/2019.
  */
class HomeCalc(rawData: Seq[DataPoint], traccar: String) {
  val traccarNumber = Map("traccar" -> 1, "traccarV2"-> 2, "traccar3"-> 3)
//  val participants_with_no_night_data: List[(Integer,Integer)] =
//    List((41,1),(13,2),(48,3),(51,3),(55,3),(56,3),(60,3),(61,3),(68,3))
//  val participants_with_no_home: List[(Integer,Integer)] =
//    List((50,1),(4,2),(65,3),(40,3))
  // take off amsterdam points of 23 (2)
  val remove: Map[Int, Set[Long]] = Map(1-> Set(41, 50),
                                        2-> Set(13, 4),
                                        3-> Set(48, 51, 55, 56, 60, 61, 68, 65, 40))

  var participants :List[ParticipantInfo] = divideByDeviceIDs(rawData, checkNight, traccarNumber(traccar))

  remove_bad_participants()

  def remove_bad_participants(): Unit = {
    val p_to_remove: Set[Long] = remove(traccarNumber(traccar))
    participants = participants.filter(p => !p_to_remove.contains(p.id))
    //True if dp.latitude > 40
//    var p = participants.find(p => p.id == 23 && p.experiment == 2)
//    val x = 0
  }

  def get_basic_data(): Map[DeviceId, BasicParticipantData] = {
    // define basicData:
    // Map of DeviceId(Long) => BasicParticipantData
    // BasicParticipantData:
    //    deviceId (Long)
    //    home (Location: center=median of medians, radius=200, positions=None)
    //    homePercentage (what is the nights percentage that are within 200 meters from home)
    //    dailyData (Map[LocalDate: year month day, BasicDailyData])
    // BasicDailyData:
    //    nightLocation (Location: center=median, radius=200, positions=None)
    var map: Map[DeviceId, BasicParticipantData] = Map.empty
    // takes only points at night.
//    var participantsInfo :List[ParticipantInfo] = divideByDeviceIDs(rawData, checkNight, traccarNumber(traccar))
    for (p <- participants) {
      map += p.id -> __get_BasicParticipantData(p)
    }
    map
  }

  def __get_BasicParticipantData(pi:ParticipantInfo): BasicParticipantData = {
    /**
      *  BasicParticipantData:
      *   deviceId (Long)
      *   home (Location: center=median of medians, radius=200, positions=None)
      *   homePercentage (what is the nights percentage that are within 200 meters from home)
      *   dailyData (Map[LocalDate: year month day, BasicDailyData])

      *  BasicDailyData:
      *   nightLocation (Location: center=median, radius=200, positions=None)
      */
    // calc median for each night
    val new_pi = _newParOnePointPerNight(pi)
    val nightLocations :Map[LocalDate, BasicDailyData] = (new_pi.getDataPoints map (dt => dt.dateTime.toLocalDate  ->
      BasicDailyData(new Location(dt.position, HOME_RADIUS, Nil)) ) ).toMap
    // calc Home
    val median_of_medians = _getMedianDataPoint(new_pi.getDataPoints)

    val home_center = new Geodetic2DPoint(
      new GeodeticLongitude(Math.toRadians(median_of_medians.rawLongitude)),
      new GeodeticLatitude(Math.toRadians(median_of_medians.rawLatitude)))

    val deviceId : DeviceId = pi.id
    val home: Location = Location(home_center, HOME_RADIUS, Nil)
    val homePercentage = __calc_home_percantage(home_center, new_pi.getDataPoints.map(dt => dt.position))
    BasicParticipantData(deviceId, home, homePercentage, nightLocations)
  }

  def __get_BasicDailyData_map(pi:ParticipantInfo): Map[LocalDate, BasicDailyData] = {
    // BasicDailyData:
    //    nightLocation (Location: center=median, radius=200, positions=None)

    // calc median point for each night
    val new_pi = _newParOnePointPerNight(pi)
    val m = new_pi.getDataPoints map (dt => dt.dateTime.toLocalDate  ->
                BasicDailyData(new Location(dt.position, HOME_RADIUS, Nil)) )
    m.toMap
  }

  def __calc_home_percantage(home: Position, nightPositions: List[Position]): Double = {
    import Position.Implicits._
    // calc what is the percentage of positions within HOME_RADIUS from home
    var counter = 0
    for (p <- nightPositions) {
      if (home.distanceTo(p) <= HOME_RADIUS) {
        counter += 1
      }
    }
    counter * 100 / nightPositions.length
  }


}
