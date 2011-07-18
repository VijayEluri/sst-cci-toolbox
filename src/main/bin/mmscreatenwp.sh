#! /bin/sh

if [ ! -d "$MMS_HOME" ]
then
    PRGDIR=`dirname $0`
    export MMS_HOME=`cd "$PRGDIR/.." ; pwd`
fi

if [ -z "$MMS_HOME" ]; then
    echo
    echo Error:
    echo MMS_HOME does not exists in your environment. Please
    echo set the MMS_HOME variable in your environment to the
    echo location of your CCI SST installation.
    echo
    exit 2
fi

MMS_OPTIONS=""
if [ ! -z $MMS_DEBUG ]; then
    MMS_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y"
fi

export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:/home/tstorm/opt/local/lib
export PATH=${PATH}:/home/tstorm/opt/cdo-1.5.0/bin

java \
    -Dmms.home="$MMS_HOME" \
    -Xmx1024M $MMS_OPTIONS \
    -javaagent:"$MMS_HOME/lib/openjpa-all-2.1.0.jar" \
    -classpath "$MMS_HOME/lib/*" \
    org.esa.cci.sst.tools.nwp.Nwp "atsr.3" "16" "false" "mmd.nc" "/exports/nas/exports/cse/geos/scratch/gc/sst-cci/ecmwf-era-interim/v01"
