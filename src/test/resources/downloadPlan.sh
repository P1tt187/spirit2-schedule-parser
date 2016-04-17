#!/bin/bash
#./timetable2db /opt/jetty/webapps/root/staticschedule/plan/s_mai2.html mai2 /home/denison/vorlage/tmp/

COURSES="bai bamc bawi bamm bais mai"; declare -a COURSES
directory=$(pwd)
planPATH=$directory"/plan/"
baseURL="http://my.fh-sm.de/~fbi-x/Stundenplan/"

echo $planPATH



for course in $COURSES
do
   for i in 1 2 3 4 5 6
   do
   curl -o $planPATH"s_"$course$i".html" $baseURL"s_"$course$i".html"
   done
done



