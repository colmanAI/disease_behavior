import os
import datetime
import re
import pandas as pd
from os.path import join
import numpy as np

from src.globals import TRACCAR1, TRACCAR2, TRACCAR3, EXPERIMENT, MAIL
# from src.participants_inspection import transmission_percentage
from src.participants_inspection.data_quality_check import transmission_percentage
from src.questionnaires_analysis.Group import group
from src.questionnaires_analysis.titles import body_temp, EMAIL_ADDRESS, FIX_DATE, TIMESTAMP, STATUS, DUPS, \
    body_temp_above_38, titles_for_sb_avg, SICK_W37, SICK_W38, used_drugs_today, used_drugs_recently, DATE, \
    SICK_WITH_MED38, SICK_WITH_MED37, SB_FILL_PERCENT, SB_AVG, titles_with_free_lang, HAVE_TRACCAR_TODAY, \
    TRANSMISSION_PERCENT
from src.valid_traccar_info import load_valid_participants

'''
globals
'''

PRINT = False

STAT_TRUE = 'TRUE'
STAT_QUES = 'QUESTION'
STAT_PROB = 'PROBLEM'


current_location = os.path.dirname(os.path.abspath(__file__))
path_to_qs = r"C:\Users\djoff\Documents\מסמכים חשובים\עבודות\מעבדה - גלית\עבודה\משימות\משימה 8- שאלונים"

Q2017 = 2017
Q2018 = 2018
Q2019 = 2019

quest_path = {Q2017: join(path_to_qs, "שאלונים 2017.xlsx"),
              Q2018: join(path_to_qs, "שאלון מחקר ניבוי מחלה 2018 (Responses).xlsx"),
              Q2019: join(path_to_qs, "שאלון מחקר ניבוי מחלה 2019 (Responses).xlsx")}

sheet = {Q2017: "Form responses 4",
         Q2018: "Form responses 1",
         Q2019: "Form Responses 1"}

experiment_num = {Q2017: 1,
                  Q2018: 2,
                  Q2019: 3}

traccar = {Q2017: TRACCAR1,
           Q2018: TRACCAR2,
           Q2019: TRACCAR3}


'''
HELPERS
'''


def print_df(df):
    with pd.option_context('display.max_rows', None, 'display.max_columns', None):  # more options can be specified also
        print(df)


def is_number(val):
    try:
        float(val)
        return True
    except Exception:
        return False


def is_nan(val):
    if is_number(val):
        return np.math.isnan(val)
    return False


def is_body_temp(temp):
    """
    return True if temp is in range of body temperature.

    by Wikipedia:
    - Hypothermia‎: ‎<35.0 °C (95.0 °F)
    - Fever‎: ‎>37.5 or 38.3 °C (99.5 or 100.9 °F)
    """
    return 35 <= temp < 44


def search_irregular_body_temp(ans_verity):
    irregulars = []
    vals = ans_verity[body_temp]
    for v in vals:
        if not is_number(v):
            irregulars.append(v)
        elif not is_body_temp(v):
            irregulars.append(v)
    ans_verity[body_temp] = irregulars

    if PRINT:
        file = join(current_location, "all_vals.txt")
        if os.path.isfile(file):
            return
        content = ""
        with open(file, 'w') as f:
            for t, vals in ans_verity.items():
                d = [t] + [str(v) for v in vals]
                content += ' | '.join(d) + "\n"
            f.write(content)


def fix_body_temp_value(val):
    if is_number(val):
        if val > 60:
            return val - 30
        return val
    # not a number
    m = re.match("([0-9]+).+([0-9]+)", val)
    if m:
        numbers = m.groups(0)
        return float('{}.{}'.format(numbers[0], numbers[1]))
    return val


def fix_used_drugs_recently(val):
    if val in {'כן', 'לא', 'כן, לא'} or is_nan(val):
        return val
    return 'כן' if 'אקמול' in val else 'לא'


def is_in_time_period(start_time, end_time, now_time):
    """

    :param start_time:
    :param end_time:
    :param now_time:
    :return: True if now_time is at least start_time but not yet end_time
    """
    if start_time < end_time:
        return end_time > now_time >= start_time
    else:  # Over midnight
        return now_time >= start_time or now_time < end_time


'''
OPERATIONS ON DATAFRAME
'''


def set_one_mail_per_person(df):
    # group mails
    groups = group(df, EMAIL_ADDRESS)
    # if PRINT:
    #     for g in groups:
    #         print(g)
    # dict: count for each mail how many appearances it has
    mail_count = df.groupby(EMAIL_ADDRESS)[EMAIL_ADDRESS].count().to_dict()
    # define one mail per person
    replace = {}
    for g in [g for g in groups if g.__len__() > 1]:
        ordered_mails = list(g)
        counts = np.array([mail_count[m] for m in ordered_mails])
        chosen_mail = ordered_mails[counts.argmax()]
        for m in g:
            replace[m] = chosen_mail.lower()  # NOTE: email addresses aren't case-sensitive
    df.replace(to_replace=replace, value=None, inplace=True)

    if PRINT:
        file = join(current_location, "mail_groups.csv")
        if os.path.isfile(file):
            return
        content = ','.join(["representative", "Mail(count)"]) + "\n"
        for g in groups:
            line = ['{}({})'.format(m, mail_count[m]) for m in g]
            if g.__len__() > 1:
                rep = [replace[g.pop()]]
            else:
                rep = [g.pop()]
            mails_and_counts = ','.join(rep + line) + "\n"
            content += mails_and_counts
        with open(file, 'w') as f:
            f.write(content)


def get_all_uniques(df, ignore_nan=False):
    """
    return dictionary of title => all it's unique values.

    :param df:
    :param ignore_nan:
    :return: dictionary of title => all it's unique values.
    """
    titles = list(df)[2:]
    ans_verity = {}
    for t in titles:
        ans_verity[t] = [val if not is_nan(val) else ''
                         for val in df[t].unique()
                         if (ignore_nan and not is_nan(val)) or not ignore_nan]
    search_irregular_body_temp(ans_verity)
    return ans_verity


def choose_row_from_dups(df):
    """
    get a list of rows and choose the right one.

    criterion:
    1. status TRUE exists:
      1.1 remove all non TRUE
      1.2 if only one left: choose it.
      1.3 multiple TRUE left: choose the one that filled more columns
      1.4 both filled the same: choose the last one.
    2. status QUESTION exists: the same
    3. same algo for status PROBLEM

    :param rows:
    :return:
    """
    # STATUS PRIORITY
    # leave only rows were status is TRUE
    priority = df.loc[df[STATUS] == STAT_TRUE]
    if priority.empty:
        # leave only rows were status is QUESTION
        priority = df.loc[df[STATUS] == STAT_QUES]
        if priority.empty:
            # the whole df is in status PROBLEM
            priority = df.loc[df[STATUS] == STAT_PROB]
    df = priority

    # only one row left
    if df.shape[0] == 1:
        return df

    # FILLING-PERCENTAGE PRIORITY
    fill_percent = []
    indexes = []
    for i, (index, row) in enumerate(df.iterrows()):
        # insert in reverse order so numpy.argmax will prefer the latest of the maxima
        fill_percent.insert(0, get_filling_percent(row))
        indexes.insert(0, i)
    # choose the row that it's filling percentage is the greatest. if all the same - choose last row
    indx = indexes[np.array(fill_percent).argmax()]
    return df.iloc[indx]


def fix_dates_and_duplicates(df):
    """
    dates are from 17 to 16:59

    :param df:
    :return:
    """

    # classify each timestamp to one fixed date
    new_col = df.apply(lambda row: get_fix_date(row), axis=1)
    # insert after Timestamp
    df.insert(df.columns.get_loc(TIMESTAMP) + 1, FIX_DATE, new_col)

    # set a reliability status on the fixed date, given the hour
    new_col = df.apply(lambda row: get_status(row), axis=1)
    # insert after fixed date
    df.insert(df.columns.get_loc(FIX_DATE)+1, STATUS, new_col)

    # for each mail: get duplicates on fixed date
    dup = df.duplicated(subset=[FIX_DATE, EMAIL_ADDRESS], keep=False)
    # insert after mail
    df.insert(df.columns.get_loc(EMAIL_ADDRESS) + 1, DUPS, dup)

    if PRINT:
        file = join(current_location, "fix_date,status,dups.xlsx")
        if not os.path.isfile(file):
            df.to_excel(file)

    # go over all that have 'TRUE' on dups
    # for each unique <mail, fix_date> value:
    #   get all rows (dups) with that value
    #   decide which row is the right one (from the duplicates)
    #   delete the other dups rows

    # get all duplicates (all mails, all dates)
    dups = df.loc[df[DUPS] == True]
    # delete all duplicates
    df.drop_duplicates(subset=[FIX_DATE, EMAIL_ADDRESS], keep=False, inplace=True)
    mails = dups[EMAIL_ADDRESS].unique()
    # for each mail that have dups
    for mail in mails:
        dups_by_mail = dups.loc[dups[EMAIL_ADDRESS] == mail]
        dup_dates = dups_by_mail[FIX_DATE].unique()
        # for each dup date
        for date in dup_dates:
            # get all rows (dups) with the same mail & fixes date
            dup_lines = dups_by_mail.loc[dups[FIX_DATE] == date]
            # decide which row is the right one (from the duplicates)
            row = choose_row_from_dups(dup_lines)
            df = df.append(row)
    df.sort_values(by=[EMAIL_ADDRESS, FIX_DATE], inplace=True)
    return df


def fix_titles(df, q):
    if q == Q2019:
        df.rename(columns={'Email Address': EMAIL_ADDRESS}, inplace=True)


def find_empty_answers(df):
    """

    :param df: all questionnaires
    :return:
    """
    titles = list(df)
    nans = {}
    for title in titles:
        nan_indxes = np.where(pd.isnull(df[title]))[0]
        if nan_indxes.size > 0:
            nans[title] = nan_indxes

    if PRINT:
        file = join(current_location, "empty_cells_location.txt")
        if os.path.isfile(file):
            return
        lines = []
        for title, rows in nans.items():
            rows = [r + 2 for r in rows]
            lines.append("column: {} rows: {}".format(title, ', '.join(map(str, rows))))
        with open(file, 'w') as f:
            f.write('\n'.join(lines))


def find_irregular_answers(df):
    """
    search irregular answers in free-language columns.

    irregular values are values that appear less than 0.5% out of the maximal count for a value in a column.

    :param df: all questionnaires
    :return:
    """
    # find irregular values for each free-language column
    irregular_vals = {}
    for title in titles_with_free_lang:  #, ans in ans_verity.items():
        all_vals = df.groupby(title)[title].count().to_dict()
        vals = []
        counts = []
        for val, count in all_vals.items():
            vals.append(val)
            counts.append(count)
        threshold = 0.5 * max(counts) / 100
        all_irregular_vals = [vals[i] for i, c in enumerate(counts) if c < threshold]
        # find location of irregular values
        irregular_vals[title] = [(v, df.index[df[title] == v].tolist())
                                 for v in all_irregular_vals]
    if PRINT:
        file = join(current_location, "irregular_vals_location.txt")
        if os.path.isfile(file):
            return
        lines = []
        for title, vals in irregular_vals.items():
            lines.append("column: {}".format(title))
            for val, rows in vals:
                # set the difference between dataframe indexes and excel indexes
                rows = [r + 2 for r in rows]
                lines.append("\tanswer: '{}' in rows: {}".format(val, ', '.join(map(str, rows))))
        with open(file, 'w') as f:
            f.write('\n'.join(lines))


'''
OPERATIONS ON ONE ROW
'''


def get_fix_date(row):
    """
    for each date D & hour H: if    00 > H >= 17   than date = D.
                             if 17:00 > H >= 00   than date = D - 1

    :param row:
    :return:
    """

    # date time
    dt = row[TIMESTAMP].to_pydatetime()
    if is_in_time_period(datetime.time(17,0), datetime.time(00,0), dt.time()):
        ret = dt.date()
    else:
        ret = dt.date() - datetime.timedelta(days=1) # date - 1
    return ret


def get_status(row):
    """
    for each hour H: if   2 > H >= 17  set status TRUE
                     if  10 > H >= 2   set status QUESTION
                     if  17 > H >= 10  set status PROBLEM

    :param row:
    :return:
    """
    # date time
    dt = row[TIMESTAMP].to_pydatetime()
    if is_in_time_period(datetime.time(17, 0), datetime.time(2, 0), dt.time()):
        ret = STAT_TRUE
    elif is_in_time_period(datetime.time(2, 0), datetime.time(10, 0), dt.time()):
        ret = STAT_QUES
    elif is_in_time_period(datetime.time(10, 0), datetime.time(17, 0), dt.time()):
        ret = STAT_PROB
    else:
        raise Exception("cant classify date status")
    return ret


def get_filling_percent(row, titles=None):
    if not titles:
        return sum([0 if is_nan(value) else 1 for index, value in row.items()]) / row.size
    return sum([0 if is_nan(row[t]) else 1 for t in titles]) / titles.__len__()


def sb_avg(row):
    """
    Mean Disease Behavior Questionnaire

    :return:
    """
    answers = [row[t] for t in titles_for_sb_avg if not is_nan(row[t])]
    return sum(answers) / answers.__len__()


def get_sickness_status(row, temp):
    return (is_number(row[body_temp]) and row[body_temp] >= temp) or \
            (not is_nan(row[body_temp_above_38]) and row[body_temp_above_38] == 'כן')


def get_sick_with_med_status(row, temp):
    # per temperature
    # find sickness by criteria
    col = SICK_W38 if temp == 38 else SICK_W37
    was_sick = row[col]
    if not was_sick:
        return 'בריא'
    # was sick
    took_medicine = (not is_nan(row[used_drugs_today]) and row[used_drugs_today] == 'כן') or \
                    (not is_nan(row[used_drugs_recently]) and row[used_drugs_recently] == 'כן')
    if took_medicine:
        return 'חולה עם תרופה'
    return 'חולה בלי תרופה'


def has_traccar_toay(row, participant):
    date = str(row[DATE])
    all_days = participant.days
    return date in all_days


# def transmission_percentage(date, participant, tfrom, tto):
#     """
#     get transmissions percentage between times tfrom-tto.
#
#     assumption: in ideal situation there is a transmission every 5 minuts, thus
#
#     :param row:
#     :param participant:
#     :param tfrom: string time
#     :param tto: string time
#     :return:
#     """
#     if date not in participant.days:
#         return 0
#     day = participant.days[date]
#
#     FMT = '%H:%M'
#     tfrom = datetime.datetime.strptime(tfrom, FMT)
#     tto = datetime.datetime.strptime(tto, FMT)
#     tdelta = tto - tfrom
#     hours = abs(tdelta.total_seconds() / (60 * 60))
#     ideal_signals_trans = hours * AMOUNT_OF_SIGNALS_IN_ONE_HOUR
#
#     tfrom = tfrom.time()
#     tto = tto.time()
#     # todo: support times that cross midnight
#
#     signals_amount = sum([1 for dp in day.day_points if tfrom <= dp[TIME].time() <= tto]) + \
#                      sum([1 for dp in day.night_points if tfrom <= dp[TIME].time() <= tto])
#
#     ret = signals_amount / ideal_signals_trans
#     return ret


'''
FLOW
'''


def load_questionnaire(q):
    """
    load questionnaire from excel to datafrom and prepare it for concatenation.

    :param q:
    :return:
    """
    df = pd.read_excel(quest_path[q], sheet[q])
    fix_titles(df, q)
    # filter out lines without mail or timestamp
    df.dropna(subset=[EMAIL_ADDRESS, TIMESTAMP], inplace=True)
    # drop columns from column 19   # todo: find better solution
    df = df.iloc[:, [j for j in range(18)]]
    return df


def edit_raw_questionnaire(df, q):
    # merge similar e-mails
    set_one_mail_per_person(df)

    # for each column - get all answers options (find irregular values)
    ans_verity = get_all_uniques(df)

    # fix irregular values in body_temp column
    df[body_temp] = df[body_temp].map(lambda val: fix_body_temp_value(val))
    # fix irregular values in used_drugs_recently column
    df[used_drugs_recently] = df[used_drugs_recently].map(lambda val: fix_used_drugs_recently(val))

    # sort by mail, afterwards by date & time
    df.sort_values(by=[EMAIL_ADDRESS, TIMESTAMP], inplace=True)

    # manage duplicates dates
    df = fix_dates_and_duplicates(df)

    # add experiment column as first column
    df.insert(0, EXPERIMENT, df.apply(lambda row: experiment_num[q], axis=1))

    # FILTER PARTICIPANTS

    # filter Keren out
    df.drop(df[df[EMAIL_ADDRESS] == 'kerenshakhar@gmail.com'].index, inplace=True)

    # todo: filter out mails
    # with less than 6 submits (unique dates)

    # print(df[MAIL].unique())
    # print(list(df))
    # print_df(df)
    return df


def traccar_integration(df, q, tr_participants):
    # 1) leave only participants with  questionnaires & traccar

    # leave only current experiment
    tr_participants = [p for p in tr_participants if p[EXPERIMENT] == experiment_num[q]]

    # drop from df (questionnaire) participants without valid traccar
    mails_with_traccar = {p[MAIL] for p in tr_participants}
    if PRINT:
        mails_with_quess = set(df[EMAIL_ADDRESS].unique())
        mails_with_q_without_t = {m for m in mails_with_quess if m not in mails_with_traccar}
        print("No traccar: from {} drop {}".format(q, ', '.join(mails_with_q_without_t)))
    df = df[df[EMAIL_ADDRESS].isin(mails_with_traccar)]

    # drop from traccar participants without questionnaires
    mails_with_quest = set(df[EMAIL_ADDRESS].unique())
    tr_participants = [p for p in tr_participants if p[MAIL] in mails_with_quest]

    # 2) add_info_based_on_traccar

    # map mail to participant
    tr_participants = {p[MAIL]: p for p in tr_participants}

    # check - have traccar today? (add as last columns)
    df.insert(df.shape[1], HAVE_TRACCAR_TODAY, df.apply(
        lambda row: str(row[DATE]) in tr_participants[row[EMAIL_ADDRESS]].days, axis=1))
    # df.insert(df.shape[1], 'have traccar today?', df.apply(
    #     lambda row: has_traccar_toay(row, participants[row[MAIL]]), axis=1))

    # calc transmission percentage
    df.insert(df.shape[1], TRANSMISSION_PERCENT.format('00', '12'), df.apply(
        lambda row: transmission_percentage(str(row[DATE]), tr_participants[row[EMAIL_ADDRESS]], '00:00', '12:00'), axis=1))

    df.insert(df.shape[1], TRANSMISSION_PERCENT.format('8', '20'), df.apply(
        lambda row: transmission_percentage(str(row[DATE]), tr_participants[row[EMAIL_ADDRESS]], '8:00', '20:00'), axis=1))

    return df


def add_features(df):
    # find sickness by criteria (add as last columns)
    df.insert(df.shape[1], SICK_W38, df.apply(lambda row: get_sickness_status(row, 38), axis=1))
    df.insert(df.shape[1], SICK_W37, df.apply(lambda row: get_sickness_status(row, 37.5), axis=1))

    # add feature - sick with\out medicine (as last columns)
    df.insert(df.shape[1], SICK_WITH_MED38, df.apply(lambda row: get_sick_with_med_status(row, 38), axis=1))
    df.insert(df.shape[1], SICK_WITH_MED37, df.apply(lambda row: get_sick_with_med_status(row, 37.5), axis=1))

    # SB filling percentage
    df.insert(df.shape[1], SB_FILL_PERCENT, df.apply(lambda row:
                                                     get_filling_percent(row, titles=titles_for_sb_avg), axis=1))
    # SB values average
    df.insert(df.shape[1], SB_AVG, df.apply(lambda row: sb_avg(row), axis=1))
    return df


def questionnaires_analysis(traccars, name=None):
    reversed_traccar = {tr: q for q, tr in traccar.items()}
    # for example: experiments = (Q2017, Q2018, Q2019)
    experiments = tuple([reversed_traccar[tr] for tr in traccars])

    # load raw questionnaires
    questionnaires = [load_questionnaire(q) for q in experiments]

    # # print raw united questionnaires
    # all_quests = pd.concat(questionnaires)
    # all_quests.to_excel(join(current_location, "raw_quests.xlsx"))

    # apply basic processing on each questionnaire
    questionnaires = [edit_raw_questionnaire(df, q) for df, q in zip(questionnaires, experiments)]
    # add info based on traccar
    tr_participants = load_valid_participants()
    questionnaires = [traccar_integration(df, q, tr_participants) for df, q in zip(questionnaires, experiments)]
    # add features
    questionnaires = [add_features(df) for df in questionnaires]

    # connect all questionnaires together to one table
    all_quests = pd.concat(questionnaires)
    # sort by experiment, then by mail and lastly by datetime
    all_quests.sort_values(by=[EXPERIMENT, EMAIL_ADDRESS, TIMESTAMP], inplace=True)
    all_quests.reset_index(inplace=True, drop=True)

    if name:
        title = "{}.xlsx".format(name if name else 'output')
        all_quests.to_excel(join(current_location, title))

    # find_irregular_answers(all_quests)
    # find_empty_answers(all_quests)

    return all_quests


    # ans_verity = get_all_uniques(all_experiments)
    # for k,v in ans_verity.items():
    #     print(k)
    #     print(v)

