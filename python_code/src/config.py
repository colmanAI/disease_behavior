from src.databases.sources import SICK
from src.globals import DEVICE_TIME
import datetime

# night definition
NIGHT_START = datetime.time(2, 0)
NIGHT_END = datetime.time(4, 0)

# the determine time to take from traccar
TIME = DEVICE_TIME  # SERVER_TIME

# home definition - median of medians
HOME_RADIUS = 200
PERCENTAGE = 60

# sick criterion - who is considered sick
SICK_CR = SICK.sick_total  # sick above 37.5 or 38
