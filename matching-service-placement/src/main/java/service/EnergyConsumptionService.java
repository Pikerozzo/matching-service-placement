package service;

import model.*;

import java.util.ArrayList;

public class EnergyConsumptionService {
    private static EnergyConsumptionService instance = null;
    private static MecSystem mecSystem = null;

    private static MecMapping mapping = null;

    private EnergyConsumptionService() {
        mecSystem = MecSystem.getInstance();
        mapping = MecMapping.getInstance();
    }

    public static EnergyConsumptionService getInstance() {
        if (instance == null) {
            instance = new EnergyConsumptionService();
        }
        return instance;
    }

    /**
     * Calculates the total energy consumption of the system.
     * @param ignoreOffloading if true, the offloading energy consumption is ignored (as in the original paper "Virtual Resource Allocation for Mobile Edge Computing: A Hypergraph Matching Approach")
     * @return the total energy consumption
     */
    public double getTotalEnergyConsumption(boolean ignoreOffloading) {
        double totalConsumption = 0.0;

        for (PM pm : mecSystem.getPhysicalMachines()) {
            totalConsumption += getEnergyConsumptionPerPm(pm.getId());
        }

        if (!ignoreOffloading) {
            totalConsumption += getTotalOffloadingEnergyConsumption();
        }

        return totalConsumption;
    }

    /**
     * Calculates the total energy consumption of the system (ignoring offloading).
     * @return the total energy consumption
     */
    public double getTotalEnergyConsumption() {
        return getTotalEnergyConsumption(true);
    }

    /**
     * Calculates the energy consumption of a specific task with the VM-to-PM match.
     * @param vm the VM
     * @param cores the number of cores
     * @param pm the PM
     * @return the energy consumption
     */
    public double getEnergyConsumptionWithVmCoresAndPm(VM vm, int cores, PM pm) {
        return  (mecSystem.getTotalDurationTime() - mecSystem.getOffloadingDurationTime()) * pm.getCoreComputeOpsPerSec() * cores * vm.getEnergyConsumptionPerCoreOps();
    }

    /**
     * Computes the total energy consumption of a VM on a specific PM (considering all their assigned tasks).
     * @param vm the VM
     * @param pm the PM
     * @return the energy consumption
     */
    public double getEnergyConsumptionWithVmAndPm(VM vm, PM pm) {
        return  getEnergyConsumptionWithVmCoresAndPm(vm, mapping.getVmCores2PmPlacement().get(vm.getId()).get(pm.getId()), pm);
    }

    /**
     * Computes the total energy consumption of a VM (considering all its assigned tasks).
     * @param vmId the VM ID
     * @return the energy consumption
     */
    public double getEnergyConsumptionPerVm(int vmId) {
        double consumption = 0.0;
        VM vm = mecSystem.getVM(vmId);
        for (int pmId : mapping.getPmsHostingVm(vmId)) {
            PM pm = mecSystem.getPM(pmId);
            consumption += getEnergyConsumptionWithVmAndPm(vm, pm);
        }
        return consumption;
    }

    /**
     * Computes the total energy consumption of a PM (considering all its assigned VMs).
     * @param pmId the PM ID
     * @return the energy consumption
     */
    public double getEnergyConsumptionPerPm(int pmId) {
        double consumption = 0.0;
        PM pm = mecSystem.getPM(pmId);
        ArrayList<Integer> vmIds = mapping.getVmsHostedByPm(pmId);
        for (int vmId : vmIds) {
            VM vm = mecSystem.getVM(vmId);
            consumption += getEnergyConsumptionWithVmAndPm(vm, pm);
        }
        return consumption;
    }

    /**
     * Computes the energy consumption of the offloading step for a specific UE.
     * @param ueId the UE ID
     * @return the energy consumption
     */
    public double getOffloadingEnergyConsumptionPerUe(int ueId) {
        UE ue = mecSystem.getUE(ueId);
        return mecSystem.getOffloadingDurationTime() / mecSystem.getNumberOfUEs() * ue.getTransmitPower();
    }

    /**
     * Computes the total energy consumption of the offloading step for all UEs.
     * @return the total energy consumption
     */
    public double getTotalOffloadingEnergyConsumption() {
        double totalConsumption = 0.0;
        for (int i = 0; i < mecSystem.getNumberOfUEs(); i++) {
            totalConsumption += getOffloadingEnergyConsumptionPerUe(i);
        }
        return totalConsumption;
    }
}