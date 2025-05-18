package model;

public class UE extends MecElement {
    private static int incrementalId = 0;
    private final int id;
    private final int requiredOffloadedCores;
    private final int requiredOffloadedMemoryGB;
    private final int offloadedTaskSize;
    private final double transmitPower;
    private final int localTaskSize;
    private final int localCoresAvailable;
    private final int coreComputeOpsPerSec;

    public UE(int requiredOffloadedCores, int requiredOffloadedMemoryGB, int offloadedTaskSize, double transmitPower, int localTaskSize, int localCoresAvailable, int coreComputeOpsPerSec){
        super("UE");
        id = incrementalId++;
        this.requiredOffloadedCores = requiredOffloadedCores;
        this.requiredOffloadedMemoryGB = requiredOffloadedMemoryGB;
        this.localTaskSize = localTaskSize;
        this.transmitPower = transmitPower;
        this.offloadedTaskSize = offloadedTaskSize;
        this.localCoresAvailable = localCoresAvailable;
        this.coreComputeOpsPerSec = coreComputeOpsPerSec;
    }

    public UE(int requiredOffloadedCores, int requiredOffloadedMemoryGB, int offloadedTaskSize, double transmitPower){
        super("UE");
        id = incrementalId++;
        this.requiredOffloadedCores = requiredOffloadedCores;
        this.requiredOffloadedMemoryGB = requiredOffloadedMemoryGB;
        this.offloadedTaskSize = offloadedTaskSize;
        this.transmitPower = transmitPower;
        this.localTaskSize = 0;
        this.localCoresAvailable = 0;
        this.coreComputeOpsPerSec = 0;
    }

    @Override
    public String toString() {
        return super.getElementName() + "_" + getId() + " (cores=" + getRequiredOffloadedCores() + ", GB=" + getRequiredOffloadedMemoryGB() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UE ue = (UE) o;
        return id == ue.id && requiredOffloadedCores == ue.requiredOffloadedCores && requiredOffloadedMemoryGB == ue.requiredOffloadedMemoryGB;
    }

    public int getId() {
        return id;
    }

    public int getRequiredOffloadedCores() {
        return requiredOffloadedCores;
    }

    public int getRequiredOffloadedMemoryGB() {
        return requiredOffloadedMemoryGB;
    }

    public int getOffloadedTaskSize() {
        return offloadedTaskSize;
    }

    public double getTransmitPower() {
        return transmitPower;
    }

    public int getLocalTaskSize() {
        return localTaskSize;
    }

    public int getLocalCoresAvailable() {
        return localCoresAvailable;
    }

    public int getCoreComputeOpsPerSec() {
        return coreComputeOpsPerSec;
    }
}