import pandas as pd
from src.statistics_calcs.statistic_info import fitRadAndPercent


def table1(experiments, participants):
    dict_exp_dates = {exp.num: exp.dates.__len__() for exp in experiments}
    titles = ['exp', 'id', 'name', '# broadcast nights', '# days', '# exp days', '% broadcast nights']
    content = []
    for p in participants:
        bn = sum([day.has_night_data() for date, day in p.days.items()])
        days = p.days.__len__()
        data = {'exp': p.experiment,
                'id': p.id,
                'name': p.name,
                '# broadcast nights': bn,
                '# days': days,
                '# exp days': dict_exp_dates[p.experiment],
                '% broadcast nights': (bn / days) * 100
                }
        line = [data[i] for i in titles]
        content.append(line)
    df = pd.DataFrame(content, columns=titles)
    df.to_csv("table1.csv")


def table2(stat_info, participants, percents, radiuses, soft_definition=False):
    """
    creates mean & median table
    """
    titles = ["experiment", "id", "median longitude", "median latitude", "mean longitude", "mean latitude", "distance [Meters]"]
    title = "{}% in radius {}"
    titles += [title.format(percent, radius) for percent in percents for radius in radiuses]
    content = []
    for si, p in zip(stat_info, participants):
        data = {'experiment': p.experiment,
                'id': p.id,
                "median longitude": si.median_night_point.longitude,
                "median latitude": si.median_night_point.latitude,
                "mean longitude": si.mean_night_point.longitude,
                "mean latitude": si.mean_night_point.latitude,
                "distance [Meters]": si.mean_night_point.distance_to(si.median_night_point)
                }
        for radius in radiuses:
            for percent in percents:
                data[title.format(percent, radius)] = fitRadAndPercent(percent, radius, si, soft_definition=soft_definition)

        line = [data[i] for i in titles]
        content.append(line)

    df = pd.DataFrame(content, columns=titles)
    df.to_csv("table2.csv")

