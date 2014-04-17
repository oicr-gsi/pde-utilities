/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.on.oicr.pde.common.utilities;

/**
 *
 * @author Raunaq Suri
 */
public class SpoofLinker {

    private String ius, lane, sampleName, runName;

    public String getIus() {
        return ius;
    }

    public String getSampleName() {
        return sampleName;
    }

    public String getRunName() {
        return runName;
    }

    public String getLane() {
        return lane;
    }

    public void setIus(String ius) {
        this.ius = ius;
    }

    public void setSampleName(String sample) {
        this.sampleName = sample;
    }

    public void setRunName(String run) {
        this.runName = run;
    }

    public void setLane(String laneName) {
        this.lane = laneName;

    }

    @Override
    public String toString() {
        return "Hello World"
    }
}
