#!/bin/bash

module load novocraft
module load picard 

dir=Sample_CPCG_158P_968

for i in {1..45}
do
    novoalign   \
       -d /oicr/data/genomes/homo_sapiens_mc/UCSC/hg19_random/Genomic/novocraft/hg19_random.nix   \
       -F ILM1.8   \
       -f $dir/CPCG_158P_968_TAAGGCGA_L001_R1_001.fastq.gz \
          $dir/CPCG_158P_968_TAAGGCGA_L001_R2_001.fastq.gz \
       -r ALL 5   \
       -R 0   \
       -oSAM $'@RG\tID:A04BK_1\tPU:m146\tLB:PCSI_0085_Ly_R_PE_397_EX\tSM:PCSI_0085_Ly_R\tPL:Illumina'   \
       > $dir/bams/$i.sam

    java -Xmx4g -jar $PICARDROOT/SortSam.jar   \
    I=$dir/bams/$i.sam   \
    O=$dir/bams/$i.bam   \
    SO=coordinate   \
    CREATE_INDEX=true   \
    TMP_DIR=picardTmp

    rm $dir/bams/$i.sam
done

for i in {1..44}
do
    index=$(($i + 1))
    for (( j=$index; j<=45; j++ ))
    do
        ./bam diff --in1 $dir/bams/$i.bam --in2 $dir/bams/$j.bam \
                   --recPoolSize -1 \
                   --out $dir/diffs/$i\_$j.txt 
    done
done 
