#/usr/bin/perl


#
# Create a sample hierarchy of directories and symlinks using a CSV file.
#
# Usage: perl createSampleHierarchy.pl representative_sample_set.csv
# 
# The CSV must have the following columns:
# * Data type : the two-letter code that describes the type of sequencing, e.g. WG, EX, WT
# * IUS SWID : the SeqWare SWID which can be used to grep through the seqware provenance 
#	file to find the files of interest. Can alternately be N/A but fastqs and bam columns
#	must be populated
# * Sample name : the full name of the sample/library 
# * Fastqs : the full comma-separated paths to the fastq files corresponding to the sample. 
#	Only used if IUS SWID is N/A.
# * BAM : the full path to the BAM file corresponding to the sample. Only used if IUS SWID is N/A.
#


use strict;
use warnings;
use List::Util qw(first);
use File::Path qw (make_path);
use File::Basename;

my $count_args = $#ARGV + 1;
die "Usage: perl createSampleHierarchy.pl representative_sample_set.csv\n" unless ($count_args == 1) ;


open (FILE, $ARGV[0]) or die "File not found : $ARGV[0]"; 

my $seqware_file="/.mounts/labs/seqprodbio/private/backups/sqwprod-db/20140512-01:02.csv.gz";

my @header = split("\t", <FILE>);
chomp (@header);

#Grab the indices of the header columns we're interested in
my $dt_index = first { $header[$_] eq "Data type"} 0 .. $#header;
my $is_index = first { $header[$_] eq "IUS SWID"} 0 .. $#header;
my $sn_index = first { $header[$_] eq "Sample name"} 0 .. $#header;
my $fq_index = first { $header[$_] eq "Fastqs"} 0 .. $#header;
my $bm_index = first { $header[$_] eq "BAM"} 0 .. $#header;

#iterate through the fastq
while (<FILE>) {
	my @line = split("\t");
	chomp(@line);
	print "Processing $line[$sn_index]\n";
	my $dir = "Full_Samples/$line[$dt_index]/$line[$sn_index]";
	my @fastqs=();
	my @bams=();

	#if this sample isn't in SeqWare, it will have N/A and must have Fastqs and BAM columns
	if ($line[$is_index] eq "N/A") {
		@fastqs = split(",", $line[$fq_index]);
		push(@bams,$line[$bm_index]);
	}else {
		#if this sample is in SeqWare, open the seqware_file above and grep through it for the IUS SWID
		open (SWFILE, "gunzip -c $seqware_file |") or die "SeqWare file is missing: $seqware_file";
		my @results=grep /$line[$is_index]/, <SWFILE>;

		#Find the FASTQ files
		my @fastq_line = grep /chemical\/seq-na-fastq-gzip/, @results;
		foreach my $fastq (@fastq_line) {
			my @arrrrgh= split("\t", $fastq);
			chomp(@arrrrgh);
			my $filepath=$arrrrgh[33];
			push(@fastqs,$filepath);
		}

		#Find the bam files
		my @bam_line = grep /application\/bam/, @results;
		@bam_line = grep /Picard/, @bam_line;
		foreach my $bam(@bam_line) {
			my @arrrrgh= split("\t", $bam);
			chomp(@arrrrgh);
			my $filepath=$arrrrgh[33];
			push(@bams,$filepath);
		}
		close SWFILE;
	}

	#create the hierarchy for this sample
	make_path $dir;
	#lazy way of creating an about file in each directory with the full contents of this line of the CSV file
	`echo "@line">>$dir/about`;
	#Symlink the fastqs
	foreach my $fastq(@fastqs) {
		my $base = basename($fastq);
		symlink $fastq, "$dir/$base";
	}
	#Symlink the BAMs
	foreach my $bam(@bams) {
		my $base = basename($bam);
		symlink $bam, "$dir/$base";
	}

}

close FILE;
exit;

