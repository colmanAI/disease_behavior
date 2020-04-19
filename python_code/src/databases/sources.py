import inspect
from os.path import join, dirname, abspath

databases_path = dirname(abspath(__file__))
__db_keys = {'questionnaires', 'mails_ids', 'sick'}

# paths to databases
paths = {k: join(databases_path, '{}_db.xlsx'.format(k)) for k in __db_keys}


class BaseTableTitles:
    """databases titles"""
    @staticmethod
    def titles():
        my_name = inspect.getframeinfo(inspect.currentframe()).function
        return [v for k, v in SICK.__dict__.items() if not k.startswith('_') and k != my_name]


class SICK(BaseTableTitles):
    mail = 'mail'
    sick_37 = 'כמות הימים שלנבדק היה מעל 37.5 מעלות חום, אבל לא יותר מ 38 מעלות' \
              '\n#sick days(37.5)'
    sick_38 = 'כמות הימים שלנבדק היה מעל 38 מעלות חום' \
              '\n#sick days(38)'
    sick_total = 'סך הכל ימי מחלה' \
                 '\ntotal sick days'


class MailsIDs(BaseTableTitles):
    exp = 'exp'
    id = 'id'
    mail = 'mail'

