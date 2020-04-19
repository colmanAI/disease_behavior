from src.config import TIME
from src.helper_funcs import is_night_dp


class Day(object):
    def __init__(self, date):
        self.date = date
        self.night_points = []
        self.day_points = []

    def __setattr__(self, key, value):
        self.__dict__[key] = value
        return self

    def add_data_point(self, dp):
        if dp[TIME].date() != self.date:
            raise Exception("wrong day")
        if is_night_dp(dp):
            self.night_points .append(dp)
        else:
            self.day_points.append(dp)

    def has_night_data(self):
        return not self.night_points.__len__() == 0
