package algorithm;

import algorithm.model.AlgorithmResults;
import algorithm.model.Ue2VmMapping;
import model.PM;

public class GreedyAlg extends MatchingAlg {
    @Override
    public AlgorithmResults run(boolean verbose) {
        for (Ue2VmMapping mapping : mecService.getUe2VmMappings()) {
            int vmId = mapping.getVmId();
            int cores = mapping.getCores();
            int memory = mapping.getMemory();

            // find the first PM that can host the UE/VM
            for (PM pm : mecService.getPMs()) {
                if (mecService.checkEnoughPmResources(vmId, pm.getId(), cores, memory) && mecService.checkAssignmentAllowed(vmId, pm.getId())) {
                    try {
                        this.totalAllocatedUEs++;
                        mecService.addVMResourcesOnPm(vmId, pm.getId(), cores, memory);
                    } catch (IllegalArgumentException e) {
                        if (verbose)
                            System.out.println("\t  ERROR ::: UE" + mapping.getUeId() + " cannot be allocated to VM_" + vmId + " allocated to " + pm.getShortName() + " with " + cores + " cores and " + memory + " GBs");
                    }
                    break;
                }
            }
        }

        return prepareResults();
    }

    @Override
    public String getName() {
        return "Greedy (first-fit)";
    }
}