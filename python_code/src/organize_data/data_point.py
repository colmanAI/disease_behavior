from src.globals import MAIL
from src.organize_data.data_manager import load_data


class DataPoint(object):
    def __init__(self):
        pass

    def __setattr__(self, key, value):
        self.__dict__[key] = value
        return self

    def __str__(self):
        return ', '.join(["{}: {}".format(k, v) for k, v in self.__dict__.items()])

    def __getitem__(self, item):
        return self.__dict__[item]

    def __eq__(self, other):
        pass


def get_data_points_from_db(db):
    """
    load all data-points and organize the info in DataPoint object.

    uses the traccar's titles to members' names, except title 'name' which is replaced by 'mail'

    :param db: string, traccar name.
    :return:
    """
    all_data = load_data(db)
    data_points = []
    for data in all_data:
        dp = DataPoint()
        for k, v in data.items():
            dp.__setattr__(k if k != 'name' else MAIL, v)
        data_points.append(dp)
    return data_points

