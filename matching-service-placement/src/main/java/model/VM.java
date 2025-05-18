package model;

public class VM extends MecMachine {
    private static int incrementalId = 0;
    private final double energyConsumptionPerCoreOps;
    private final int maxPmPlacements;



    public VM(int availableCores, int availableMemoryGB, double energyConsumptionPerCoreOps) {
        super(incrementalId++, availableCores, availableMemoryGB, "VM");
        this.energyConsumptionPerCoreOps = energyConsumptionPerCoreOps;
        this.maxPmPlacements = 25;
    }

    public VM(int availableCores, int availableMemoryGB, double energyConsumptionPerCoreOps, int maxPmPlacements) {
        super(incrementalId++, availableCores, availableMemoryGB, "VM");
        this.energyConsumptionPerCoreOps = energyConsumptionPerCoreOps;
        this.maxPmPlacements = maxPmPlacements;
    }

    public double getEnergyConsumptionPerCoreOps() {
        return energyConsumptionPerCoreOps;
    }

    @Override
    public String getMachineDetails() {
        return "(cores=" + getTotCores() + ", GB=" + getTotMemoryGB() + ", energy/coreOps=" + getEnergyConsumptionPerCoreOps() + ", max PM placements="+ getMaxPmPlacements() + ")";
    }

    public int getMaxPmPlacements() {
        return maxPmPlacements;
    }
}