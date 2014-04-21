/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.on.oicr.pde.common.utilities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 *
 * @author Raunaq Suri
 */
public class SpoofLinker {

    private String ius = null, lane = ".", sampleName = ".", runName = ".";
    private String file = null;
    private String mimeType = ".";
    private String fileStatus = ".";
    private String separator = ",";

    /**
     * Gets the IUS SW Accession
     *
     * @return the accession
     */
    public String getIus() {
        return ius;
    }

    /**
     * gets the sample name
     *
     * @return the sample name
     */
    public String getSampleName() {
        return sampleName;
    }

    /**
     * gets the run name
     *
     * @return the run name
     */
    public String getRunName() {
        return runName;
    }

    /**
     * gets the lane number
     *
     * @return the lane number
     */
    public String getLane() {
        return lane;
    }

    /**
     * gets the file being linked
     *
     * @return the file that's being linked
     */
    public String getFile() {
        return file;
    }

    /**
     * gets the mime type of the file
     *
     * @return the mime type
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * gets the file status of the file to be linked
     *
     * @return the linked file
     */
    public String getFileStatus() {
        return fileStatus;
    }

    /**
     * sets the file
     *
     * @param file The path of the file
     */
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Sets the mime type of the file
     *
     * @param mime the mime-type of the file
     */
    public void setMimeType(String mime) {
        this.mimeType = mime;
    }

    /**
     * Sets the file status of the file
     *
     * @param status the status of the file
     */
    public void setFileStatus(String status) {
        this.fileStatus = status;
    }

    /**
     * Sets the ius swid
     *
     * @param ius the swid you want to link to
     */
    public void setIus(String ius) {
        this.ius = ius;
    }

    /**
     * Sets the sample name you want to link to
     *
     * @param sample the sample name
     */
    public void setSampleName(String sample) {
        this.sampleName = sample;
    }

    /**
     * Sets the run name you want to link to
     *
     * @param run the run name
     */
    public void setRunName(String run) {
        this.runName = run;
    }

    /**
     * Sets the lane you wish to link to
     *
     * @param laneNumber number of the lane you want to link to
     */
    public void setLane(String laneNumber) {
        this.lane = laneNumber;

    }

    /**
     * Sets the way to separate the files
     *
     * @param separator the separator, usually "\t" or ","
     */
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    /**
     * Gets the separator used
     *
     * @return separator
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * To String
     *
     * @return the way it should appear in accordance to the file Linker format
     */
    @Override
    public String toString() {
        String line = runName + separator + sampleName + separator + lane + separator + ius + separator + fileStatus + separator + mimeType + separator + file + "\n";
        return line;
    }

    /**
     * Override
     *
     * @param obj the other SpoofLinker obj to compare to
     * @return whether they are equal or not
     */
    @Override
    public boolean equals(Object obj) {

        return EqualsBuilder.reflectionEquals(this, obj);

    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
