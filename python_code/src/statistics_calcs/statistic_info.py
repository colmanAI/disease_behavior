from statistics import median, mean
from src.geodetic_calc import GeodeticPoint
from src.organize_data.data_point import DataPoint
from src.globals import LATITUDE, LONGITUDE
from copy import deepcopy


class StatisticInfo(object):
    """
    saves statistic information per participant.
    """
    def __init__(self, night_points):
        self.night_points = night_points
        self.mean_night_point = GeodeticPoint()
        self.median_night_point = GeodeticPoint()

        if self.night_points:
            self.calc_mean_median()

    def calc_mean_median(self):
        longitudes = []
        latitudes = []
        for dp in self.night_points:
            longitudes.append(dp.longitude)
            latitudes.append(dp.latitude)

        self.mean_night_point = GeodeticPoint(mean(latitudes), mean(longitudes))
        self.median_night_point = GeodeticPoint(median(latitudes), median(longitudes))


def fitRadAndPercent(min_night_percent, max_radius, stat_info, soft_definition=False):
    """
    return true if at least min_night_percent % of participant's night dataPoints are within maxRadius from
    participant's median.

    :param min_night_percent:
    :param max_radius:
    :param stat_info:
    :param soft_definition: says how to define the nights percentage. if True - in case X percentage is a.b nights,
    then a nights is enough.
    :return:
    """
    if not stat_info.night_points:
        return "-"
    # data points within radius (from the median)
    dpwr = 0
    # if distance from night_point to median night point <= max_radius
    for night_point in stat_info.night_points:
        np = GeodeticPoint(night_point.latitude, night_point.longitude)
        if np.distance_to(stat_info.median_night_point) <= max_radius:
            dpwr += 1
    if soft_definition and (dpwr >= int(stat_info.night_points.__len__() * min_night_percent / 100)):
        return "YES"
    elif (dpwr/stat_info.night_points.__len__()) * 100 >= min_night_percent:
        return "YES"
    return "NO"


def calc_median_data_point(data_points):
    """

    :param data_points: list of data points.
    :return: single data points, constructed from the longitude median and the latitude median.
    """
    latitudes, longitudes = [], []
    for dp in data_points:
        latitudes.append(dp.latitude)
        longitudes.append(dp.longitude)
    median_dp = DataPoint()
    median_dp.__setattr__(LATITUDE, median(latitudes)).__setattr__(LONGITUDE, median(longitudes))
    return median_dp


def cut_up_to_x_days(participants, x):
    """
    for each participant, remove day x+1 and forward.

    :param participants: list of participants.
    :param x: int, day number.
    :return: shortened participants list.
    """
    new_participants = deepcopy(participants)
    for p in new_participants:
        if p.ordered_days.__len__() > x:
            for k in p.ordered_days[x:]:
                del p.days[k]
            del p.ordered_days[x:]
            # update whether there is still night data
            p.has_night_data = any([True for date, day in p.days.items() if day.night_points])
    return new_participants

