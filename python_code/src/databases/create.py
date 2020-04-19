"""
create all databases
"""
import os
import sys
import pandas as pd
from src.databases.load import get_db
from src.databases.sources import paths, SICK, MailsIDs
from src.globals import TRACCARS, EXPERIMENT, ID, MAIL
from src.helper_funcs import dump_table
from src.questionnaires_analysis.organize_questionnaires import questionnaires_analysis, EMAIL_ADDRESS, SICK_W37, \
    SICK_W38
from src.valid_traccar_info import load_valid_participants


def __raise_exception_if_exists(path):
    if os.path.exists(path):
        raise FileExistsError("database {} already exists".format(path))


"""
create databases

convention: create functions will be named '__create_<db key>_db'
"""


def create_db(db=None):
    """
    db can be any key in __db_keys
    """
    if db in paths:
        # check path doesn't exit
        __raise_exception_if_exists(paths[db])
        # call create function
        getattr(sys.modules[__name__], '__create_{}_db'.format(db))()
    return None


def __create_questionnaires_db():
    edited_quests = questionnaires_analysis(TRACCARS)
    edited_quests.to_excel(paths['questionnaires'], index=False)


def __create_mails_ids_db():
    """
    creates an excel table with experiment number, id and mail of all participants that have a valid traccar &
    questionnaires data.

    assumption: edited_questionnaires_path database exists.
    """
    quest_path = paths['questionnaires']
    if not os.path.exists(quest_path):
        raise FileNotFoundError("can't create tr_qs_mails_ids database; database {} is missing".
                                format(quest_path))
    series = pd.read_excel(quest_path)\
        .groupby(EXPERIMENT).apply(lambda df: df[EMAIL_ADDRESS].unique())
    exp_to_mail_in_ques = {exp: {m for m in mails} for exp, mails in series.to_dict().items()}

    tr_participants = load_valid_participants()
    exps = {p[EXPERIMENT] for p in tr_participants}
    exp_to_mail_in_trac = {exp: {p[MAIL] for p in tr_participants if p[EXPERIMENT] == exp}
                           for exp in exps}

    intersect_mails = {exp: exp_to_mail_in_ques[exp].intersection(exp_to_mail_in_trac[exp]) for exp in exps}
    table_content = [{MailsIDs.exp: exp,
                      MailsIDs.id: p[ID],
                      MailsIDs.mail: p[MAIL]}
                     for exp, mails in intersect_mails.items()
                     for p in tr_participants if p[EXPERIMENT] == exp and p[MAIL] in mails]
    dump_table(table_content, titles=MailsIDs.titles(), format='excel', path=paths['mails_ids'], index=False)

    # # get questionnaires data
    # quest_df = pd.read_excel(edited_questionnaires_path)
    # group = quest_df.groupby(EXPERIMENT)
    # # get traccar data
    # tr_participants = load_valid_participants()
    # table_content = []
    # # work on each experiment individually
    # for exp, table in group:
    #     participants = [p for p in tr_participants if p[EXPERIMENT] == exp]
    #     mails_with_traccar = {p[ParticipantInfo.MAIL] for p in participants}
    #     mails_with_quest = set(table[EMAIL_ADDRESS].unique())
    #
    #     joint_mails = mails_with_traccar.intersection(mails_with_quest)
    #     table_content += [{'exp': exp,
    #                        'id': p[ID],
    #                        'mail': p[ParticipantInfo.MAIL]}
    #                       for p in participants if p[ParticipantInfo.MAIL] in joint_mails]
    #
    # titles = ['exp', 'id', 'mail']
    # dump_table(table_content, titles=titles, format='excel', path=tr_qs_mails_ids_path, index=False)


def __create_sick_db():
    """
    who was sick and how many days - based on database edited_questionnaires_path.

    columns:
    1) mail (of people who were sick someday)
    2) amount of sick days (37)
    3) amount of sick days (38)

    :param ques_path: path to (all) questionnaires excel.
    :return: mails of sick participants
    """
    # all_quests
    df = get_db(db='questionnaires')
    # get all rows that sick=True (37 or 38)
    df = df.loc[df[SICK_W37], :]

    # for each participant count amount of rows that sick=37 & amount of rows that sick=38
    count38 = df.groupby(EMAIL_ADDRESS)[SICK_W38].sum().to_dict()
    count37 = df.groupby(EMAIL_ADDRESS)[SICK_W37].sum().to_dict()
    count37 = {mail: count - count38[mail] for mail, count in count37.items()}

    # create table
    mails = list(count38.keys())
    # titles = [SICK.mail, SICK.sick_37, SICK.sick_38, SICK.sick_total]
    data = [{SICK.mail: mail, SICK.sick_38: count38[mail], SICK.sick_37: count37[mail],
             SICK.sick_total: count37[mail] + count38[mail]} for mail in mails]
    dump_table(data, titles=SICK.titles(), format='excel', path=paths['sick'], index=False)

    # return mails

    # todo: count sick days with medicine.


"""
TODOS:
    1) add columns to sick

    data quality check - fix is_sick
    helper func - fix has_home
"""



