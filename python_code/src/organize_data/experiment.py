

class Experiment(object):
    """
    information:
    - all dates the experiment took place.
    - how many participants
    - how many participants with night transmissions.
    """
    def __init__(self, exp_num):
        self.num = exp_num
        self.dates = []
        self.part_with_nights = []
        self.part_without_nights = []

    def add_participant(self, p):
        """

        :param p: ParticipantInfo object
        :return:
        """
        if p.has_night_data:
            self.part_with_nights.append(p)
        else:
            self.part_without_nights.append(p)
        for day in p.ordered_days:
            if day not in self.dates:
                self.dates.append(day)

    def __str__(self):
        return "exp {}".format(self.num)


def get_exps_from_parts(participants):
    experiments = {}
    for p in participants:
        if p.experiment not in experiments:
            experiments[p.experiment] = Experiment(p.experiment)
        experiments[p.experiment].add_participant(p)
    return list(experiments.values())