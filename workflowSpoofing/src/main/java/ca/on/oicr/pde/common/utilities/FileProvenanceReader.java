/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.on.oicr.pde.common.utilities;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

/**
 *
 * @author Raunaq Suri
 */
public class FileProvenanceReader {

    private CellProcessor[] getProcessors() {
        final CellProcessor[] processers = new CellProcessor[]{
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new NotNull(),
            null,
            null,
            null,
            null,
            new NotNull(),
            null,
            null,
            null,
            null,
            null,
            new NotNull(),
            null,
            null,
            null,
            new NotNull(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        };

        return processers;
    }
    
    private String[] getHeader(){
        String header[] = {
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "sampleName",
            null,
            null,
            null,
            null,
            "runName",
            null,
            null,
            null,
            null,
            null,
            "lane",
            null,
            null,
            null,
            "ius",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        };
        return header;
    }

    public List<Map<String, Object>> readWithCsvMapReader(StringReader reader) throws Exception {

        ICsvBeanReader beanReader = null;
        List<Map<String,Object>> fileProvenanceReport = new ArrayList<Map<String,Object>>();
        try {
            beanReader = new CsvBeanReader(reader, CsvPreference.TAB_PREFERENCE);

            
            beanReader.getHeader(true); //Skip the header
            final CellProcessor[] processors = getProcessors();
            final String[] header = getHeader();
            
            Map<String, Object> customerMap;
               SpoofLinker customer;
               
                while( (customer = beanReader.read(SpoofLinker.class, header, processors)) != null ) {
                        System.out.println(customer.toString());
                }

        } finally {
            if (beanReader != null) {
                beanReader.close();
            }
        }
        
        return fileProvenanceReport;
        
    }
    
    public static void main(String[] args) throws Exception{
        StringReader hello = new StringReader("Last Modified	Study Title	Study SWID	Study Attributes	Experiment Name	Experiment SWID	Experiment Attributes	Parent Sample Name	Parent Sample SWID	Parent Sample Organism IDs	Parent Sample Attributes	Sample Name	Sample SWID	Sample Organism ID	Sample Organism Code	Sample Attributes	Sequencer Run Name	Sequencer Run SWID	Sequencer Run Attributes	Sequencer Run Platform ID	Sequencer Run Platform Name	Lane Name	Lane Number	Lane SWID	Lane Attributes	IUS Tag	IUS SWID	IUS Attributes	Workflow Name	Workflow Version	Workflow SWID	Workflow Run Name	Workflow Run Status	Workflow Run SWID	Processing Algorithm	Processing SWID	Processing Attributes	File Meta-Type	File SWID	File Attributes	File Path	File Md5sum	File Size	File Description	Skip\n" +
"2013-07-28 23:21:13.255331	PDE_TEST	2		PDE_ILLUMINA	3		TEST_0001_Pa_P:TEST_0001	6:5	31:31		TEST_0001_Pa_P_PE_300_WG	74	31	Homo_sapiens	sample.geo_tissue_type=P;sample.geo_library_type=PE;sample.geo_library_size_code=300;sample.geo_library_source_template_type=WG;sample.geo_tissue_origin=Pa	TEST_SEQUENCER_RUN_002	31		20	ILLUMINA	2	2	73	lane.geo_lane=2	NoIndex	75		GATKRecalibrationAndVariantCallingHg19WholeGenome	1.3.16-6	236		completed	300	SummaryOfWorkflowOutputs	925		application/bam	915		/.mounts/labs/seqprodbio/private/sqwci/checkpoint_output/gatk/results/seqware-0.12.5-full_GATKREcalibrationAndVariantCalling-1.3.16-6/10239348/recalibration_and_variant_calling_gatk.gatk.merged.realigned.markdup.recalibrated.bam			A file output from the GenericCommandRunner which executed the command \"/.mounts/labs/seqprodbio/private/sqwci/provisioned-bundles/Workflow_Bundle_GATKRecalibrationAndVariantCalling_0.10.6_SeqWare_12.5/GATKRecalibrationAndVariantCalling/1.x.x/bin/jre1.6.0_29/bin/java\".	false\n" +
"2013-07-28 23:21:13.255331	PDE_TEST	2		PDE_ILLUMINA	3		TEST_0001_Pa_P:TEST_0001	6:5	31:31		TEST_0001_Pa_P_PE_300_WG	74	31	Homo_sapiens	sample.geo_tissue_type=P;sample.geo_library_type=PE;sample.geo_library_size_code=300;sample.geo_library_source_template_type=WG;sample.geo_tissue_origin=Pa	TEST_SEQUENCER_RUN_002	31		20	ILLUMINA	2	2	73	lane.geo_lane=2	NoIndex	75		GATKRecalibrationAndVariantCallingHg19WholeGenome	1.3.16-6	236		completed	300	SummaryOfWorkflowOutputs	925		application/vcf-4-gzip	926		/.mounts/labs/seqprodbio/private/sqwci/checkpoint_output/gatk/results/seqware-0.12.5-full_GATKREcalibrationAndVariantCalling-1.3.16-6/10239348/recalibration_and_variant_calling_gatk.gatk.merged.realigned.markdup.recalibrated.variants.filtered.merged.sorted.vcf.gz				false");
        FileProvenanceReader reader = new FileProvenanceReader();
        reader.readWithCsvMapReader(hello);
    }
}
