from src.config import TIME
from src.helper_funcs import is_night_dp
from src.organize_data.data_point import get_data_points_from_db
from src.organize_data.day import Day
from src.globals import ID, EXP_NUM, EXPERIMENT, MAIL
from copy import deepcopy


class ParticipantInfo(object):
    def __init__(self, id, exp, name):
        self._data_points = []
        self.days = {}
        self.ordered_days = []
        self.__setattr__(EXPERIMENT, exp)
        self.__setattr__(ID, id)
        self.__setattr__(MAIL, name)
        self.has_night_data = False

    def add_data_point(self, dp):
        self._data_points.append(dp)

        date = dp[TIME].date()
        if str(date) not in self.days:
            self.days[str(date)] = Day(date)
            self.ordered_days.append(str(date))
        self.days[str(date)].add_data_point(dp)

        if is_night_dp(dp):
            self.has_night_data = True

    def __str__(self):
        return "id: {}, exp: {}, has_night: {}".format(self[ID], self[EXPERIMENT], self.has_night_data)

    def __setattr__(self, key, value):
        self.__dict__[key] = value
        return self

    # def __cmp__(self, other):
    #     return self.id == other.id and self.experiment == other.experiment

    def get_data_points(self):
        return self._data_points

    def remove_data_points_by_rule(self, drop_rule):
        """
        remove all data-points that agree with the drop_rule.

        :param drop_rule: function that gets a data point and returns True if it should be dropped.
        :return:
        """
        self._data_points = [dp for dp in self._data_points if not drop_rule(dp)]
        # delete data points from days list
        days = deepcopy(self.days)
        for date in days:
            day = self.days[date]
            day.day_points = [dp for dp in day.day_points if not drop_rule(dp)]
            day.night_points = [dp for dp in day.night_points if not drop_rule(dp)]
            if not day.day_points and not day.night_points:
                # delete day from days
                del self.days[date]
                # delete day from ordered days
                self.ordered_days.remove(date)
        self.__update_has_night_data()

    def __update_has_night_data(self):
        self.has_night_data = False
        for dp in self._data_points:
            if is_night_dp(dp):
                self.has_night_data = True
                break

    def __getitem__(self, item):
        return self.__dict__[item]


def __get_participants_from_data_points(data_points, experiment, filter_func=None):
    participants = {}
    for dp in data_points:
        if dp[ID] not in participants:
            participants[dp[ID]] = ParticipantInfo(id=dp[ID], name=dp[MAIL], exp=experiment)
        if (filter_func and filter_func(dp)) or not filter_func:
            participants[dp[ID]].add_data_point(dp)
    return list(participants.values())


def get_participants(traccars, filter_dp=None, filter_raw_data=None):
    """

    :param traccars: list of strings
    :param filter_dp:
    :param filter_raw_data:
    :return:
    """
    all_participants = []
    for tr in traccars:
        data = get_data_points_from_db(tr)
        data = filter_raw_data(data)if filter_raw_data else data
        participants = __get_participants_from_data_points(data, EXP_NUM[tr], filter_dp)
        all_participants += participants
    return all_participants


