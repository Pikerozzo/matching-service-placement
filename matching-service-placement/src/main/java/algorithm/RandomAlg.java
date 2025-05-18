package algorithm;

import algorithm.model.AlgorithmResults;
import algorithm.model.ResourceAvailability;
import algorithm.model.Ue2VmMapping;
import model.PM;

import java.util.*;

public class RandomAlg extends MatchingAlg {
    long seed;

    public RandomAlg(long seed){
        super();
        this.seed = seed;
    }

    public RandomAlg(){
        this(new Random().nextLong());
    }

    @Override
    public AlgorithmResults run(boolean verbose) {
        // set seed for random number generator
        Random rand = new Random(this.seed);

        List<Ue2VmMapping> unmatchedVMs = new ArrayList<>(mecService.getUe2VmMappings());

        // randomize VM order
        Collections.shuffle(unmatchedVMs);

        // track available PM resources
        Map<Integer, ResourceAvailability> pmResources = new HashMap<>();
        for (PM pm : mecService.getPMs()) {
            pmResources.put(pm.getId(), new ResourceAvailability(pm.getId(), pm.getTotCores(), pm.getTotMemoryGB(), pm.getMaxVmsHosted()));
        }

        // assign VMs randomly
        for (Ue2VmMapping mapping : unmatchedVMs) {

            // filter PMs that can host the VM
            List<PM> feasiblePMs = mecService.getPMs().stream()
                    .filter(pm -> pmResources.get(pm.getId()).canPerformMatch(mapping.getCores(), mapping.getMemory())).toList();

            // if there are feasible PMs, randomly select one and allocate the VM
            if (!feasiblePMs.isEmpty()) {
                PM selectedPM = feasiblePMs.get(rand.nextInt(feasiblePMs.size()));
                mecService.addVMResourcesOnPm(mapping.getVmId(), selectedPM.getId(), mapping.getCores(), mapping.getMemory());
                pmResources.get(selectedPM.getId()).allocateResources(mapping.getCores(), mapping.getMemory());
                this.totalAllocatedUEs++;
            }
        }

        return prepareResults();
    }

    @Override
    public String getName() {
        return "Random";
    }
}
