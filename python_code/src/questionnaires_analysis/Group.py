from difflib import SequenceMatcher


class Group(object):
    def __init__(self, element=None):
        self.g = set()
        if element:
            self.g.add(element)

    def add(self, element):
        self.g.add(element)
        return self

    def get_set(self):
        return self.g

    def __add__(self, other):
        for e in other.get_set():
            self.add(e)
        return self

    def __str__(self):
        return self.g.__str__()

    def __eq__(self, other):
        return self.g == other.g

    def __hash__(self):
        return hash(frozenset(self.g))


def similar(a, b):
    return SequenceMatcher(None, a.lower(), b.lower()).ratio()


def group(df, group_key, similarity_th=0.92):
    """
    find all groups in dataframe df in column group_key s.t. each group contains values that are similar* to
    one another.

    *similar definition: a, b are considered similar if similar(a, b) > similarity_th
    """
    mails = list(df[group_key].unique())
    groups = {mail: Group(mail) for mail in mails}
    for i in range(mails.__len__()):
        for j in range(i + 1, mails.__len__()):
            mi = mails[i]
            mj = mails[j]
            # if similar - unite groups
            if similar(mj, mi) > similarity_th:
                unite_group = groups[mi] + groups[mj]
                groups[mi] = unite_group
                groups[mj] = unite_group
                # groups[mi].add(mj)
                # groups[mj] = groups[mi]
    groups = set(groups.values())
    return [g.get_set() for g in groups]

