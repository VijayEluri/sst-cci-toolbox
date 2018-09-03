#!/bin/bash

# MMS function definitions
# usage ${MMS_HOME}/bin/mms-env-svr.sh  (in xxx-start.sh and xxx-run.sh)

export SVR_PYTHON_EXEC=/group_workspaces/cems2/esacci_sst/software/miniconda3/envs/sst_cci_qa/bin/python
export MMS_HOME=/group_workspaces/cems2/esacci_sst/mms/software/quality_assessment

export PATH=$MMS_HOME/bin:$PATH
export PYTHONPATH=$MMS_HOME:$PYTHONPATH

set -e

if [ -z "${MMS_INST}" ]; then
    MMS_INST=`pwd -P`
fi

MMS_TASKS=${MMS_INST}/tasks
MMS_LOG=${MMS_INST}/log

read_task_jobs() {
    jobname=$1
    jobs=
    if [ -e ${MMS_TASKS}/${jobname}.tasks ]
    then
        for logandid in `cat ${MMS_TASKS}/${jobname}.tasks`
        do
            job=`basename ${logandid}`
            log=`dirname ${logandid}`
            if grep -qF 'Successfully completed.' ${log}
            then
                if [ "${jobs}" != "" ]
                then
                    jobs="${jobs}|${job}"
                else
                    jobs="${job}"
                fi
            fi
        done
    fi
}

wait_for_task_jobs_completion() {
    jobname=$1
    while true
    do
        sleep 10
        # Output of bjobs command:
        # JOBID   USER    STAT  QUEUE      FROM_HOST   EXEC_HOST   JOB_NAME   SUBMIT_TIME
        # 619450  rquast  RUN   lotus      lotus.jc.rl host042.jc. *r.n10-sub Aug 14 10:15
        # 619464  rquast  RUN   lotus      lotus.jc.rl host087.jc. *r.n11-sub Aug 14 10:15
        # 619457  rquast  RUN   lotus      lotus.jc.rl host209.jc. *r.n12-sub Aug 14 10:15
        # 619458  rquast  RUN   lotus      lotus.jc.rl host209.jc. *r.n11-sub Aug 14 10:15
        # 619452  rquast  RUN   lotus      lotus.jc.rl host043.jc. *r.n10-sub Aug 14 10:15
        if bjobs -P esacci_sst_svr | egrep -q "^$jobs\\>"
        then
            continue
        fi

        if [ -s ${MMS_TASKS}/${jobname}.tasks ]
        then
            for logandid in `cat ${MMS_TASKS}/${jobname}.tasks`
            do
                job=`basename ${logandid}`
                log=`dirname ${logandid}`

                if [ -s ${log} ]
                then
                    if ! grep -qF 'Successfully completed.' ${log}
                    then
                        echo "tail -n10 ${log}"
                        tail -n10 ${log}
                        echo "`date -u +%Y%m%d-%H%M%S`: tasks for ${jobname} failed (reason: see ${log})"
                        exit 1
                    else
                        echo "`date -u +%Y%m%d-%H%M%S`: tasks for ${jobname} done"
                        exit 0
                    fi
                else
                        echo "`date -u +%Y%m%d-%H%M%S`: logfile ${log} for job ${job} not found"
                fi
            done
        fi
    done
}

submit_job() {
    hrs=$1
    jobname=$2
    command=$3
    bsubmit="bsub -R rusage[mem=8192] -M 8192 -q short-serial -n 1 -W ${hrs} -P esacci_sst_svr -cwd ${MMS_INST} -oo ${MMS_LOG}/${jobname}.out -eo ${MMS_LOG}/${jobname}.err -J ${jobname} ${MMS_HOME}/bin/${command} ${@:4}"

    rm -f ${MMS_LOG}/${jobname}.out
    rm -f ${MMS_LOG}/${jobname}.err

    if hostname | grep -qF 'lotus.jc.rl.ac.uk'
    then
        echo "${bsubmit}"
        line=`${bsubmit}`
    else
        echo "ssh -A lotus.jc.rl.ac.uk ${bsubmit}"
        line=`ssh -A lotus.jc.rl.ac.uk ${bsubmit}`
    fi

    echo ${line}
    if echo ${line} | grep -qF 'is submitted'
    then
        jobs=`echo ${line} | awk '{ print substr($2,2,length($2)-2) }'`
        echo "${MMS_LOG}/${jobname}.out/${jobs}" > ${MMS_TASKS}/${jobname}.tasks
    else
        echo "`date -u +%Y%m%d-%H%M%S`: tasks for ${jobname} failed (reason: was not submitted)"
        exit 1
    fi
}