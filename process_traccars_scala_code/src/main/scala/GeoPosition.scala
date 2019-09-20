///**
//  * Created by nirle on 6/25/2017.
//  */
//case class GeoPosition(latitude: Double, longitude: Double) {
//  import GeoPosition._
//
//  def distanceTo(other: GeoPosition): Double = {
//    val deltaLatitude = Math.toRadians(other.latitude - latitude)
//    val deltaLongitude = Math.toRadians(other.longitude - longitude)
//    val a = Math.pow(Math.sin(deltaLatitude / 2), 2) + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude)) * Math.pow(Math.sin(deltaLongitude / 2), 2)
//    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
//
//    EARTH_RADIUS * c
//  }
//
//  def asCartesianPosition: CartesianPosition = {
//    val x = Math.sin(Math.toRadians(latitude)) * Math.cos(Math.toRadians(longitude))
//    val y = Math.sin(Math.toRadians(latitude)) * Math.sin(Math.toRadians(longitude))
//    val z = Math.cos(Math.toRadians(latitude))
//
//    CartesianPosition(x, y, z)
//  }
//}
//
//object GeoPosition {
//  val EARTH_RADIUS = 6371000
//}
