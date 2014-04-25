#!/bin/bash

module load novocraft #todo: use bwa instead 
module load picard

dir=Sample_POP162_R_688
fastq1=POP162_R_688_CGATGT_L001_R1_001.fastq.gz
fastq2=POP162_R_688_CGATGT_L001_R2_001.fastq.gz


for i in {1..45}
do
    novoalign   \
       -d /oicr/data/genomes/homo_sapiens_mc/UCSC/hg19_random/Genomic/novocraft/hg19_random.nix   \
       -F ILM1.8   \
       -f $dir/$fastq1 \
          $dir/$fastq2 \
       -r ALL 5   \
       -R 0   \
       -oSAM $'@RG\tID:A04BK_1\tPU:m146\tLB:PCSI_0085_Ly_R_PE_397_EX\tSM:PCSI_0085_Ly_R\tPL:Illumina'   \
       > $dir/$i.sam

#    java -Xmx4g -jar $PICARDROOT/SortSam.jar   \
#    I=$dir/$i.sam   \
#    O=$dir/$i.bam   \
#    SO=coordinate   \
#    CREATE_INDEX=true   \
#    TMP_DIR=picardTmp
#
#    rm $dir/$i.sam

    if ( [ $i -ne 1 ] )
    then

        #first diff with all the other bams (if i==2 then there are none)
        for file in $dir/diffs/1_*
        do
            patch -o - $dir/1.sam $file | \
                diff --speed-large-files $dir/$i.sam \
                > $dir/diffs/$i\_$(basename $file)
        done

        #then diff with original
       diff $dir/1.sam $dir/$i.sam \
            > $dir/diffs/1_$i.diff
        
        #then delete samfile
        rm $dir/$i.sam
    fi
done

ls -al $dir/diffs/ | cut -d ' ' -f 5 > sizes.txt


