package il.ac.colman.disease

import java.time.{Duration, LocalDate}

case class FullParticipantData(deviceId: DeviceId, dailyData: Map[LocalDate, FullDailyData])

case class FullDailyData(trips: Int,
                         totalDistance: Meters,
                         totalWalkingDistance: Meters,
                         totalDrivingDistance: Meters,
                         averageTripDistance: Meters,
                         maxTripDiameter: Meters,
                         totalDuration: Duration,
                         totalWalkingDuration: Duration,
                         totalDrivingDuration: Duration,
                         averageTripDuration: Duration,
                         maxTripDuration: Duration,
                         averageWalkingSpeed: MetersPerSecond,
                         averageDrivingSpeed: MetersPerSecond,
                         hasSleptHome: Boolean)
