#!/bin/sh

# NOTE!  change out the APP to the application you want to run
APP="Apps.Face"
SLEEPAPP="Apps.MySleep"
OUTPUT="demo.out"


# to run these tests you need: 
# - SystemDispatch.java Send.java SendDate.java SendNotify.java SendMany.java
# getStats.java Apps/Face.java Apps/MySleep.java (the last two can be subbed
# with your app)

# running simple tests
# run regular workletJunction
# sending a worklet junction and normal application

# id = default0
java psl.worklets.SystemDispatch localhost target 9101 $APP >> $OUTPUT


# iterated runs, no wait time
# id = test1
java psl.worklets.Send localhost target 9101 $APP 10 0 "test1" >> $OUTPUT

# iterated runs, with wait time
# id = test2
java psl.worklets.Send localhost target 9101 $APP 10 2000 "test2" >> $OUTPUT

# Dated run
# need to change the date accordingly to:
# - the date is set with the year starting at 1900, so year + 1900 is what
# you get.  Also note that the month starts with 0.

# id=dated junction
java psl.worklets.SendDate localhost target 9101 102 3 29 2 24  >> $OUTPUT

# running notified run

# - start off waiting, to do this, make the interval = -1
# - send a notifier.  see SendNotify.java for details on 
# getting the workletJunction reference and waking it up

# id = notify
# id = notifier
java psl.worklets.Send localhost target 9101 $APP 2 -1 "notify" >> $OUTPUT
java psl.worklets.SendNotify localhost target 9101 "notify" >> $OUTPUT
# need to send it twice to remove all iterations
java psl.worklets.SendNotify localhost target 9101 "notify" >> $OUTPUT

# try notifying something that isn't there
# id = notifier
java psl.worklets.SendNotify localhost target 9101 "foo" >> $OUTPUT

# trying to add a whole bunch of junctions in the waiting state
# id = notify$i
java psl.worklets.SendMany localhost target 9101 $APP 1 -1 "notify" >> $OUTPUT

java psl.worklets.SendNotify localhost target 9101 "notify19" >> $OUTPUT
java psl.worklets.SendNotify localhost target 9101 "notify7" >> $OUTPUT
java psl.worklets.SendNotify localhost target 9101 "notify13" >> $OUTPUT

# send an application that tests that the running state is used
java psl.worklets.Send localhost target 9101 $SLEEPAPP 5 2000 "RunningState" >> $OUTPUT

# let's get the stats for what we just did
java psl.worklets.getStats localhost target 9101 >> $OUTPUT
