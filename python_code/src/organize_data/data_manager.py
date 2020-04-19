import datetime
import json
import os.path
import pyodbc
from datetime import datetime
from os.path import join, dirname, abspath

import pytz

from src.globals import ID, DISTANCE, TOTAL_DISTANCE, LATITUDE, LONGITUDE, SERVER_TIME, SPEED, ATTRIBUTES, \
    DEVICE_TIME, FIX_TIME, MAIL

# dump directory is next to current directory
DUMP_DIR_PATH = join(dirname(dirname(abspath(__file__))), "dump")


UTC = False
local_tz = pytz.timezone("Asia/Jerusalem")  # use your local timezone name here


def utc_to_local(utc_dt):
    return utc_dt.replace(tzinfo=pytz.utc).astimezone(local_tz)


def connect_and_select(traccar):
    db = traccar
    name = "localhost" #"DESKTOP-498C7SK"
    statement = \
            "select d.id, d.name, p.servertime, p.devicetime, p.fixtime, p.speed, p.attributes, p.latitude, p.longitude" \
            " from devices d" \
            " inner join positions p" \
            " on d.id = p.deviceid" \
            " where d.name not in('Lior', 'Tal', 'Ron', 'maayan')"\
            " order by d.id, p.devicetime"
    cnxn = pyodbc.connect("Driver={SQL Server};"
                          "Server=" + name + ";"
                          "Database=" + db + ";"
                          "Trusted_Connection=yes;")

    cursor = cnxn.cursor()
    cursor.execute(statement)
    return cursor


def dump_data(traccar):
    """
    dump raw data from server to a txt file, when each line is in json format.

    :param traccar:
    :return:
    """
    path = join(DUMP_DIR_PATH, traccar + ".txt")
    if os.path.isfile(path):
        answer = input("if you continue, existing '{}' will be run over. Do you want to continue? [y/n]\n".format(traccar + ".txt"))
        if answer != 'y':
            print("dump stopped.")
            return
    all_data = get_data_from_server(traccar)

    with open(path, 'w') as file:
        for data in all_data:
            data_j = json.dumps(data, default=str)
            file.write(data_j.__str__() + "\n")


def __convert_str_to_time(str_time_obj):
    try:
        return datetime.strptime(str_time_obj, '%Y-%m-%d %H:%M:%S.%f')
    except:
        return datetime.strptime(str_time_obj, '%Y-%m-%d %H:%M:%S')


def load_data(traccar):
    path = join(DUMP_DIR_PATH, traccar + ".txt")
    if not os.path.isfile(path):
        raise FileNotFoundError("file path {} wasn't fount".format(path))
    all_data = []
    with open(path, 'r') as file:
        for cnt, line in enumerate(file):
            dp = json.loads(line)
            # build time objects
            dp[SERVER_TIME] = __convert_str_to_time(dp[SERVER_TIME])
            dp[DEVICE_TIME] = __convert_str_to_time(dp[DEVICE_TIME])
            dp[FIX_TIME] = __convert_str_to_time(dp[FIX_TIME])
            all_data.append(dp)
    return all_data


def get_data_from_server(traccar):
    """
    gets raw data.

    :param traccar:
    :return: list of dictionaries.
    """
    cursor = connect_and_select(traccar)
    all_data = []
    for row in cursor:
        attributes = json.loads(row.attributes)
        data = {
            ID: row.id,
            ATTRIBUTES: attributes,
            MAIL: row.name,
            SERVER_TIME:  row.servertime,  # utc_to_local(row.servertime) if UTC else row.servertime,
            DEVICE_TIME: row.devicetime,
            FIX_TIME: row.fixtime,
            SPEED: row.speed,
            DISTANCE: attributes[DISTANCE],
            TOTAL_DISTANCE: attributes[TOTAL_DISTANCE],
            LATITUDE:  row.latitude,
            LONGITUDE: row.longitude
        }
        all_data.append(data)
    return all_data
