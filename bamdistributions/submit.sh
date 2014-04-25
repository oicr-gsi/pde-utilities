#!/bin/bash

qsub -cwd -b y -N pop6_makebams -l h_vmem=40g -q production -e Sample_POP162_R_688/makebams.log -o Sample_POP162_R_688/makebams.log \
./sample_pop6.sh
