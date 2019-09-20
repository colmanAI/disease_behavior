package il.ac.colman.disease

import java.time.ZonedDateTime

/**
  * Created by nirle on 3/30/2017.
  */
case class DataPoint(deviceId: DeviceId,
                     email: Email,
                     dateTime: ZonedDateTime,
                     speed: MetersPerSecond,
                     distance: Meters,
                     totalDistance: Meters,
                     position: Position,
                     rawLatitude: Latitude,
                     rawLongitude: Longitude)
