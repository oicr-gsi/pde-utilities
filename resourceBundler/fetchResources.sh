#!/bin/bash

set -e
set -o pipefail

help() { 
echo "Downloads the requested resources to the output location.
Supports rsync of individual files and directories, and ftp of files.
The resources file should be in the following format:

	#protocol://[user@]host
	relative file	relative md5file or md5sum

Where the relative files are the relative location on the server from the host"
usage

}

usage(){
echo "Usage: $0 -f <resource file with paths> -o <output directory> " 1>&2;
echo "Use -h for more information."
exit 1; 
}

while getopts ":f:o:ht" o; do
    case "${o}" in
	f)
	    FILES=${OPTARG}
            if [ ! -e $FILES ]
	    then
		echo -e "Resource file must exist"
		help;
	    fi
	    ;;
	o)
	    RBUNDLE=${OPTARG}
	    RBUNDLE="$(cd $(dirname ${RBUNDLE}); pwd)/$(basename ${RBUNDLE})"
	    if [ ! -d $RBUNDLE ]
            then
                echo -e "Output directory must exist"
                usage;
            fi
	    ;;
	h)
	    help
	    ;;
	t)
	    TEST=0
	    ;;
        *)
	    echo -e "Please provide an accepted option. this is not one: ${o} > ${OPTARG}"
            usage
            ;;
    esac
done
shift $((OPTIND-1))

if [ -z "${FILES}" ] || [ -z "${RBUNDLE}" ]
then
    echo -e "Missing a thing. ${FILES} & ${RBUNDLE}"
    usage
fi
LOGFILE=${RBUNDLE}/resourcebundling.log
EXECFILE=$RBUNDLE/resourcebundling.exec

#remove blank lines
sed -i '/^$/d' ${FILES}

echo "date >> $LOGFILE; cd ${RBUNDLE}" > $EXECFILE

while read line; do
    if [[ ${line:0:1} == '#' ]]
    then
	#strip out beginning #'s
	HOST=${line##\#}
	continue
    fi
    if [ -z "${HOST}" ]
    then
	echo -e "Improperly formatted resources file. Must start with #protocol://[username@]hostname followed by lines with files. protocol=${HOST}"
	help
    fi

    cols=( $line );
    #split the line
    file="${cols[0]}"

    md5="${cols[1]}"
    #remove spaces
    MD5FILE=`basename ${md5} |  tr -d '[[:space:]]'`

    #check whether the path is a directory or a file - note it's not a local file so we do a simple check for *
    #if the md5sum is a file, remove absolute paths with sed
    #if the md5sum is the sum, just echo it so that we can check
    IS_FILE=0
    IS_MD5_FILE=0
    if [[ ${file} == *\* ]]
    then 
	IS_FILE=1
	MD5CHECK="sed -i -r \"s/ .*\/(.+)/  \1/g\" ${MD5FILE} && md5sum -c $MD5FILE"
    else #if it is a file, check if the md5sum column is a file or a md5sum
	NAME=`basename ${file}`
	if [[ $MD5FILE =~ [a-f0-9]{32} ]]
	then
	    IS_MD5_FILE=1
	    MD5CHECK="echo \"$MD5FILE $NAME\" | md5sum -c"
	else
	    MD5CHECK="sed -i -r \"s/ .*\/(.+)/  \1/g\" ${MD5FILE} && grep ${NAME} ${MD5FILE} | md5sum -c"
	fi
    fi

    START=${PWD}
    if [[ $HOST == rsync* ]]
    then
	#rsync will log to the logfile, skip files based on their checksum, write to the resource bundle directory
	CMD="rsync -vc --log-file $LOGFILE ${HOST}/${file} ${RBUNDLE}/"
	test "$IS_MD5_FILE" == 0 && CMD="${CMD} && rsync -vc --log-file ${LOGFILE} ${HOST}/${md5} ${RBUNDLE}/"
		
    else
	if [ ! $IS_FILE==0  ]
	then
	    echo -e "$0 does not support fetching directories with wget"
	    usage;
	fi
	#wget will get any file that does not already exist in the destination, be non-verbose, append to the logfile
	CMD="wget -nc -nv -a $LOGFILE ${HOST}/${file}"
	test "$IS_MD5_FILE" == 0 && CMD="${CMD} && wget -nc -nv -a $LOGFILE ${HOST}/${md5}"
	
    fi
    CMD="${CMD} && ${MD5CHECK}"
    echo $CMD >> $EXECFILE
done < ${FILES}

echo "cd ${START}" >> $EXECFILE
chmod +x $EXECFILE
test "$TEST" != 0 && bash $EXECFILE



