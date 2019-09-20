///**
//  * Created by nirle on 7/3/2017.
//  */
//case class CartesianPosition(x: Double, y: Double, z: Double) {
//
//  def asGeoPosition: GeoPosition = {
//    val latitude = Math.atan2(z, Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)))
//    val longitude = Math.atan2(-y, x)
//
//    GeoPosition(latitude, longitude)
//  }
//
//  def +(cp: CartesianPosition) = CartesianPosition(x + cp.x, y + cp.y, z + cp.z)
//  def /(d: Double) = CartesianPosition(x / d, y / d, z / d)
//}
