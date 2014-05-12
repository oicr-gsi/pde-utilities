package com.github.seqware;

import ca.on.oicr.pde.utilities.workflows.OicrModule;
import java.io.File;
import java.io.IOException;
import net.sourceforge.seqware.common.module.ReturnValue;
import net.sourceforge.seqware.common.util.Log;
import net.sourceforge.seqware.common.util.filetools.FileTools;
import net.sourceforge.seqware.common.util.runtools.RunTools;

/**
 * <p>Uses cutadapt to trim reads from paired end fastq files. When quality is
 * passed to this module, cutadapt will remove reads from each fastq file. In
 * order to keep the two fastqs in sync with one another, the first pass over
 * the files writes to temporary files, and the second pass writes the final
 * files.</p>
 *
 * <p>cutadapt -a AGCT -q 20 -M 20 --paired-output tmp.2.fq.gz -o tmp.1.fq.gz
 * fq.1.gz fq.2.gz</p>
 *
 * <p>cutadapt -a AGCT -q 20 -M 20 --paired-output trim.1.fq.gz -o trim.2.fq.gz
 * tmp.2.fq.gz tmp.1.fq.gz</p>
 *
 *
 * <p>For more details, see <a
 * href="https://github.com/marcelm/cutadapt">cutadapt docs</a> in the
 * "Paired-end adapter trimming" section.</p>
 *
 * <p>Arguments:</p>
 *
 * <ul>
 *
 * <li>fastq-read-1: The path to read 1. Required: true</li>
 *
 * <li>fastq-read-2: The path to read 2. Required: true</li>
 *
 * <li>output-read-1: . Required: true</li>
 *
 * <li>output-read-2: . Required: true</li>
 *
 * <li>adapters-1: Comma-separated adapter sequences to check for on the 3' end
 * of read1 . Required: false</li>
 *
 * <li>adapters-2: Comma-separated adapter sequences to check for on the 3' end
 * of read2 . Required: false</li>
 *
 * <li>cutadapt: The full path to cutadapt 1.3+. Required: true</li>
 *
 * <li>quality: The minimum quality score to trim. Required: false</li>
 *
 * <li>minimum-length: The minimum permissible length for a read. Required:
 * false</li>
 *
 * <li>other-parameters-1: Any other parameters to pass to cutadapt for read 1.
 * Required: false</li>
 *
 * <li>other-parameters-2: Any other parameters to pass to cutadapt for read 2..
 * Required: false</li>
 *
 * </ul>
 *
 * If you need to trim adapters from regions other than the 3' end, use the
 * other-parameters-# arguments to give -g (for 5') or -b (5' and 3') arguments.
 *
 * @author Morgan Taschuk
 *
 */
public class CutAdaptModule extends OicrModule {

    private String read1Adapters = "";
    private String read2Adapters = "";
    private String quality;
    private String length;
    private String cutadapt;
    private File tempDir = null;

    public CutAdaptModule() {
        super();
        defineArgument("fastq-read-1", "The path to read 1", true);
        defineArgument("fastq-read-2", "The path to read 2", true);
        defineArgument("output-read-1", "", true);
        defineArgument("output-read-2", "", true);
        defineArgument("adapters-1", "Comma-separated adapter sequences to check for on the 3' end of read1 ", false);
        defineArgument("adapters-2", "Comma-separated adapter sequences to check for on the 3' end of read2 ", false);
        defineArgument("cutadapt", "The full path to cutadapt 1.3+", true);
        defineArgument("quality", "The minimum quality score to trim", false);
        defineArgument("minimum-length", "The minimum permissible length for a read", false);
        defineArgument("other-parameters-1", "Any other parameters to pass to cutadapt for read 1", false);
        defineArgument("other-parameters-2", "Any other parameters to pass to cutadapt for read 2.", false);

    }

    @Override
    public ReturnValue do_verify_parameters() {
        ReturnValue ret = super.do_verify_parameters();

        String[] a1 = getArgument("adapters-1").split(",");
        for (String a : a1) {
            if (!a.trim().isEmpty()) {
                read1Adapters += " -a " + a;
            }
        }
        String[] a2 = getArgument("adapters-2").split(",");
        for (String a : a2) {
            if (!a.trim().isEmpty()) {
                read2Adapters += " -a " + a;
            }
        }

        quality = getArgument("quality");
        if (!quality.trim().isEmpty()) {
            quality = "-q " + quality;
        }

        length = getArgument("minimum-length");
        if (!length.trim().isEmpty()) {
            length = "-M " + length;
        }

        cutadapt = getArgument("cutadapt");

        return ret;
    }

    @Override
    public ReturnValue do_run() {
        ReturnValue ret = new ReturnValue();
        try {
            tempDir = FileTools.createTempDirectory(new File("."));
        } catch (IOException e) {
            e.printStackTrace();
            ret.setStderr(e.getMessage());
            ret.setExitStatus(ReturnValue.DIRECTORYNOTWRITABLE);
        }

        //cutadapt -a AGCT -q 20 -M 20 --paired-output tmp.2.fq.gz -o tmp.1.fq.gz fq.1.gz fq.2.gz
        String command = cutadapt + " " + read1Adapters + " " + quality + " " + length + " ";
        command += "--paired-output " + tempDir.getAbsolutePath() + "/tmp.2.fastq.gz -o " + tempDir.getAbsolutePath() + "/tmp.1.fastq.gz ";
        command += getArgument("other-parameters-1") + " ";
        command += getArgument("fastq-read-1") + " ";
        command += getArgument("fastq-read-2") + " ";
        command += ";";

        //cutadapt -a AGCT -q 20 -M 20 --paired-output trim.1.fq.gz -o trim.2.fq.gz tmp.2.fq.gz tmp.1.fq.gz
        command += cutadapt + " " + read2Adapters + " " + quality + " " + length + " ";
        command += "--paired-output " + getArgument("output-read-1") + " -o " + getArgument("output-read-2") + " ";
        command += getArgument("other-parameters-2") + " ";
        command += tempDir.getAbsolutePath() + "/tmp.2.fastq.gz ";
        command += tempDir.getAbsolutePath() + "/tmp.1.fastq.gz ";
        command += ";";

        Log.stdout("Command: " + command);
        try {
            ret = RunTools.runCommand(command);
        } catch (Exception e) {
            e.printStackTrace();
            
        } finally {
            cleanUp();
        }
        Log.stdout("Exit status: " + ret.getExitStatus());



        return ret;
    }

    public void cleanUp() {
        ReturnValue ret = new ReturnValue();
        if (!tempDir.delete()) {
            ret.setExitStatus(ReturnValue.DIRECTORYNOTWRITABLE);
            ret.setStderr("Can't delete folder: " + tempDir.getAbsolutePath());
        }
    }
}
