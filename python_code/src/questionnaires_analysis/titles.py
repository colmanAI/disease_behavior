""" all titles in raw & edited questionnaires"""


"""
initial titles 
"""

EMAIL_ADDRESS = 'Email address'
TIMESTAMP = 'Timestamp'

body_temp = "מדוד את טמפ' גופך באמצעות מדחום. מה הטמפ'?"
used_drugs_today = "האם השתמשת היום בתרופות המפחיתות חום או כאבים?"
used_drugs_recently = "האם השתמשת בארבעת השעות האחרונות בתרופות המפחיתות חום או כאבים?"
title4 = "אני לא רוצה לזוז"
title5 = "הגוף שלי כואב"
title6 = "אני רוצה להיות לבד"
title7 = "אין לי חשק לעשות דבר"
title8 = "אני מרגיש מדוכא"
title9 = "אני מרגיש סחוט"
title10 = "אני חש בחילה"
title11 = "אני מרגיש רעוע פיזית"
title12 = "אני מרגיש עייף"
title13 = "יש לי כאב ראש"
title14 = "אני מרגיש חולה"
body_temp_above_38 = "חום גופי עלה היום מעל 38 מעלות"
title16 = "הייתי היום אצל רופא משפחה"
# 18 titles, incl. mail & timestamp
# 4th column and above - specific options

# types of questions grouped in lists
titles_with_free_lang = [used_drugs_today, used_drugs_recently, title16]
titles_for_sb_avg = [title4, title5, title6, title7, title8, title9, title10, title11, title12, title13]

# above 38
# titles_determine_fever = [body_temp, body_temp_above_38]
# titles_determine_taken_med = [used_drugs_today, used_drugs_recently]


""" 
new titles 
"""

FIX_DATE = 'fixed_date'
DATE = FIX_DATE
DUPS = 'duplicates'
STATUS = 'data_status'
SICK_W38 = 'חולה היום עם 38 מעלות לפחות = אם חום הגוף עלה מעל טמפ זו או אם מדדתי עם מדחום' \
           '\nsick_with_38'
SICK_W37 = 'חולה היום עם 37.5 מעלות לפחות =  אם חום הגוף עלה מעל טמפ זו או אם מדדתי עם מדחום' \
           '\nsick_with_37.5'
SICK_WITH_MED38 = 'חולה עם תרופה = גם חולה עם 38 מעלות לפחות, וגם לקח תרופה (השתמש היום / ב 4 שעות האחרונות בתרופות משככות כאבים)' \
                  '\nsick_with_med_38'
SICK_WITH_MED37 = 'חולה עם תרופה = גם חולה עם 37.5 מעלות לפחות, וגם לקח תרופה (השתמש היום / ב 4 שעות האחרונות בתרופות משככות כאבים)' \
                  '\nsick_with_med_37.5'
SB_FILL_PERCENT = 'אחוז מילוי שאלון התנהגות מחלה'
SB_AVG = 'ממוצע שאלון התנהגות מחלה' \
         '\nSBavg'
HAVE_TRACCAR_TODAY = 'האם קיים traccar לתאריך זה לנבדק זה' \
                     '\nhave traccar today?'
TRANSMISSION_PERCENT = 'אחוז השידורים (100% = 12 שידורים בשעה) ב traccar ביום זה בטווח שעות' \
                       '\n%transmission {}-{}'


