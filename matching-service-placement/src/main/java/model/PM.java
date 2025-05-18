package model;

public class PM extends MecMachine {
    private static int incrementalId = 0;
    private double coreComputeOpsPerSec;
    private final int maxVmsHosted;


    public PM(int availableCores, int availableMemoryGB, double coreComputeOpsPerSec) {
        super(incrementalId++, availableCores, availableMemoryGB, "PM");
        this.coreComputeOpsPerSec = coreComputeOpsPerSec;
        this.maxVmsHosted = 7;
    }

    public PM(int availableCores, int availableMemoryGB, double coreComputeOpsPerSec, int maxVmsHosted) {
        super(incrementalId++, availableCores, availableMemoryGB, "PM");
        this.coreComputeOpsPerSec = coreComputeOpsPerSec;
        this.maxVmsHosted = maxVmsHosted;
    }

    public double getCoreComputeOpsPerSec() {
        return coreComputeOpsPerSec;
    }

    public int getMaxVmsHosted() {
        return maxVmsHosted;
    }

    @Override
    public String getMachineDetails() {
        return "(cores=" + getTotCores() + ", GB=" + getTotMemoryGB() + ", coreOps/sec=" + getCoreComputeOpsPerSec() + ", max PM placements="+ getMaxVmsHosted() + ")";
    }

    public void setCoreComputeOpsPerSec(double coreComputeOpsPerSec) {
        this.coreComputeOpsPerSec = coreComputeOpsPerSec;
    }
}