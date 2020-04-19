import geopy.distance


class GeodeticPoint(object):
    def __init__(self, latitude=None, longitude=None):
        self.longitude = longitude
        self.latitude = latitude

    @property
    def coordinate(self):
        tup = (self.latitude, self.longitude)
        return tup

    def distance_to(self, geo_point):
        """
        returns the distance from self to geo_point in meters.

        :param geo_point:
        :return: distance from self to geo_point in meters.
        """
        return geopy.distance.distance(self.coordinate, geo_point.coordinate).km * 1000


# def get_geodetic_point(latitude, longitude):
#     return GeodeticPoint(latitude, longitude)


# def distance_in_meters(geo_point1, geo_point2):
#     return geopy.distance.distance(geo_point1.coordinate, geo_point2.coordinate).km * 1000

# def get_median_gpoint():
#     pass
