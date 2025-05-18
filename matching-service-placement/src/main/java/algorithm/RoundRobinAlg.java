package algorithm;

import algorithm.model.AlgorithmResults;
import algorithm.model.Ue2VmMapping;
import model.PM;

public class RoundRobinAlg extends MatchingAlg {

    @Override
    public AlgorithmResults run(boolean verbose) {
        int totalPms = mecService.getNumberOfPMs();
        int lastPmId = 0;

        for (Ue2VmMapping mapping : mecService.getUe2VmMappings()) {
            int cores = mapping.getCores();
            int memory = mapping.getMemory();

            // allocate the VM to the UE
            boolean pmFound = false;
            PM pm = null;
            // find the first PM that can host the UE/VM requirements, in a Round Robin fashion
            int firstPmId = lastPmId;
            while(true){
                int pmId = lastPmId;
                lastPmId = (lastPmId+1)%totalPms;

                if (lastPmId == firstPmId){
                    break;
                }

                if (mecService.checkEnoughPmResources(mapping.getVmId(), pmId,cores,memory) && mecService.checkAssignmentAllowed(mapping.getVmId(), pmId)) {
                    pm = mecService.getPM(pmId);
                    pmFound = true;
                    break;
                }
            }
            if (!pmFound){
                if (verbose)
                    System.out.println("NO AVAILABLE PM FOUND FOR UE_" + mapping.getUeId());
            }
            else {
                try {
                    this.totalAllocatedUEs++;
                    mecService.addVMResourcesOnPm(mapping.getVmId(), pm.getId(), cores, memory);
                    if (verbose)
                        System.out.println("\t UE_" + mapping.getUeId() + " assigned to VM_" + mapping.getVmId() + " allocated to " + pm.getShortName() + " with " + cores + " cores and " + memory + " GBs");

                } catch (IllegalArgumentException e) {
                    if (verbose)
                        System.out.println("\t  ERROR ::: UE" + mapping.getUeId() + " cannot be allocated to VM_" + mapping.getVmId() + " allocated to " + pm.getShortName() + " with " + cores + " cores and " + memory + " GBs");
                }
            }
        }

        return prepareResults();
    }

    @Override
    public String getName() {
        return "Round Robin";
    }
}