package il.ac.colman.disease

import java.time.LocalDate

case class BasicParticipantData(deviceId: DeviceId, home: Location, homePercentage: Double, dailyData: Map[LocalDate, BasicDailyData])

case class BasicDailyData(nightLocation: Location)
