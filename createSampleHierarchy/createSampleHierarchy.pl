#!/usr/bin/perl
use strict;
use warnings;
use List::Util qw(first);
use File::Path qw (make_path);
 use File::Basename;
open (FILE, $ARGV[0]) or die "File not found : $ARGV[0]"; 

my $seqware_file="/.mounts/labs/seqprodbio/private/backups/sqwprod-db/20140512-01:02.csv.gz";

my @header = split("\t", <FILE>);
chomp (@header);

my $dt_index = first { $header[$_] eq "Data type"} 0 .. $#header;
my $to_index = first { $header[$_] eq "Tissue origin"} 0 .. $#header;
my $tt_index = first { $header[$_] eq "Tissue type"} 0 .. $#header;
my $is_index = first { $header[$_] eq "IUS SWID"} 0 .. $#header;
my $sn_index = first { $header[$_] eq "Sample name"} 0 .. $#header;
my $fq_index = first { $header[$_] eq "Fastqs"} 0 .. $#header;
my $bm_index = first { $header[$_] eq "BAM"} 0 .. $#header;

while (<FILE>) {
	my @line = split("\t");
	chomp(@line);
	my $dir = "Full_Samples/$line[$dt_index]/$line[$sn_index]";
#	if (-e $dir) {
#		print "$dir exists; skipping\n";
#		next;
#	}


#	my $small_dir = "Subset_Samples/$line[$dt_index]/$line[$sn_index]";
#	if (-e $small_dir) {
#                print "$small_dir exists; skipping\n";
#                next;
#       }



	my @fastqs=();
	my @bams=();

	if ($line[$is_index] eq "N/A") {
		@fastqs = split(",", $line[$fq_index]);
		push(@bams,$line[$bm_index]);
	} else {
		open (SWFILE, "gunzip -c $seqware_file |") or die "SeqWare file is missing: $seqware_file";
		my @results=grep /$line[$is_index]/, <SWFILE>;
		my @fastq_line = grep /chemical\/seq-na-fastq-gzip/, @results;
		foreach my $fastq (@fastq_line) {
			my @arrrrgh= split("\t", $fastq);
			chomp(@arrrrgh);
			my $filepath=$arrrrgh[33];
			push(@fastqs,$filepath);
		}
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

	make_path $dir;
	`echo "@line">>$dir/about`;
	foreach my $fastq(@fastqs) {
		my $base = basename($fastq);
		symlink $fastq, "$dir/$base";
	}
	foreach my $bam(@bams) {
		my $base = basename($bam);
		symlink $bam, "$dir/$base";
	}

}

close FILE;
exit;


