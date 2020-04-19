import os
import pandas as pd
from src.databases.sources import paths


class DBs(object):
    """
    DBs class ensures that each database is loaded maximum once, and keeps a reference to that instance.
    """
    _loaded_dbs = {}

    def load(self, path):
        # if database file doesn't exist
        if not os.path.exists(path):
            raise FileNotFoundError("{} doesn't exist in databases".format(path))
        # if database wasn't loaded or defined yet
        if self._loaded_dbs.get(path, None) is None:
            self._loaded_dbs[path] = pd.read_excel(path)
        return self._loaded_dbs[path]


__dbs = DBs()


def get_db(**kwargs):
    """
    returns a database.

    arguments allowed:

    path: path to database
    db: database key name. can be any of the keys in __db_keys
    """
    if kwargs.get('path', None):
        return __dbs.load(kwargs['path'])
    if kwargs.get('db', None) in paths:
        return __dbs.load(paths[kwargs['db']])
    return None

