"""
Assignments
"""
import pandas as pd

from src.extract_features import prep_participants
from src.helper_funcs import is_night_dp
from src.organize_data.experiment import get_exps_from_parts
# from src.globals import is_night_dp
# from src.organize_data import get_participants
# from src.participants_inspection import drop_long_distance_trips
from src.organize_data.participant_info import get_participants
from src.participants_inspection.missing_median_home import drop_long_distance_trips
from src.statistics_calcs.statistic_info import StatisticInfo, calc_median_data_point, cut_up_to_x_days
# from src.statistics_calcs import table1, table2


# #####################  create statistical information  ######################


# broadcast data
from src.statistics_calcs.tables import table1, table2


def create_broadcast_table(traccars, participants=None):
    participants = get_participants(traccars) if not participants else participants
    experiments = get_exps_from_parts(participants)
    table1(experiments, participants)


def broadcast_data_up_to_x_days(traccars, x=22):
    """
    cut from x'rd day onwards.
    """
    participants = get_participants(traccars)
    # for p in participants:
    #     if p.ordered_days.__len__() > x:
    #         for k in p.ordered_days[x:]:
    #             del p.days[k]
    #         del p.ordered_days[x:]
    #         update whether there is still night data
    #         p.has_night_data = any([True for date, day in p.days.items() if day.night_points])
    # delete day 23 onwards
    participants = cut_up_to_x_days(participants, x)
    experiments = get_exps_from_parts(participants)
    table1(experiments, participants)


def mean_median_for_all_points(traccars, percentages=None, radiuses=None):
    """
    calculate the mean and median for each participant for all night points.
    check if X% of the night points are within R radius from the median point.
    """
    participants = get_participants(traccars)
    # for each participant - calculate median and mean of locations.
    stat_info = [StatisticInfo([dp for dp in p.get_data_points() if is_night_dp(dp)]) for p in participants]
    percentages = [50, 60, 70, 80, 90, 100] if not percentages else percentages
    # in meters
    radiuses = [20, 30, 40, 50, 60] if not radiuses else radiuses
    table2(stat_info, participants, percentages, radiuses)


def mean_median_for_repr_points(traccars, percentages=None, radiuses=None, participants=None, soft_definition=False):
    """
    for each participant for each night calculate a representative point - the night's median.
    check if X% of the night representative points are within R radius from the median point.
    """
    participants = get_participants(traccars) if not participants else participants
    # for each participant - create night points array with representative points only.
    stat_info = [StatisticInfo([calc_median_data_point(day.night_points)
                                for date, day in p.days.items() if day.night_points]) for p in participants]
    percentages = [50, 60, 70, 80, 90, 100] if not percentages else percentages
    # in meters
    radiuses = [20, 30, 40, 50, 60] if not radiuses else radiuses
    table2(stat_info, participants, percentages, radiuses, soft_definition=soft_definition)


def mean_median_rep_points_drop_long_trips(traccars, percentages=None, radiuses=None, soft_definition=False):
    """
    create mean & median table for representative points, after dropping long distance trips from participants:
    23 (exp. 2), 40 (exp. 3)
    """
    participants = get_participants(traccars)
    participants = drop_long_distance_trips(participants)
    mean_median_for_repr_points(traccars, percentages=percentages, radiuses=radiuses, participants=participants,
                                soft_definition=soft_definition)


def mean_median_rep_points_drop_long_trips_only_x_days(traccars, percentages=None, radiuses=None, soft_definition=False,
                                                       x=22):
    """
    create mean & median table for representative points, after leaving x days for each participant and dropping
    long distance trips.
    """
    participants = drop_long_distance_trips(cut_up_to_x_days(get_participants(traccars), x))
    mean_median_for_repr_points(traccars, percentages=percentages, radiuses=radiuses, participants=participants,
                                soft_definition=soft_definition)


def find_dates_without_night_location(traccars):
    participants = prep_participants(traccars)
    titles = ["id", "date", "has_night_loc"]
    content = []
    for p in participants:
        for date in p.ordered_days:
            # line = [p.id, date, p.days[date].night_points.__len__() != 0]
            content.append([p.id, date, p.days[date].night_points.__len__() != 0])
    df = pd.DataFrame(content, columns=titles)
    df.to_csv("table3.csv")
