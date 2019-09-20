package il.ac.colman.disease

import org.opensextant.geodesy.Geodetic2DArc

object Position {
  object Implicits {
    implicit class RichPosition(position: Position) {
      def distanceTo(other: Position): Meters = {
        new Geodetic2DArc(position, other).getDistanceInMeters
      }
    }
  }
}
