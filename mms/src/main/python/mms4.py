from workflow import Period
from workflow import Workflow

usecase = 'mms4'
mmdtype = 'mmd4'

w = Workflow(usecase, Period('1991-01-01', '1992-01-01'))
w.add_primary_sensor('avhrr.n10', '1986-11-17', '1991-09-17')
w.add_primary_sensor('avhrr.n11', '1988-11-08', '1995-01-01')
w.add_primary_sensor('avhrr.n12', '1991-09-16', '1998-12-15')
w.set_samples_per_month(0)

w.run(mmdtype, with_history=True)
