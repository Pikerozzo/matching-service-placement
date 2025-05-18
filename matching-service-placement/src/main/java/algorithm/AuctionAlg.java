package algorithm;

import algorithm.model.AlgorithmResults;
import algorithm.model.ResourceAvailability;
import algorithm.model.Preference;
import algorithm.model.Ue2VmMapping;
import algorithm.utils.PreferenceComparator;
import model.PM;

import java.util.*;

public class AuctionAlg extends MatchingAlg {
    // key is bid receiver (PMs), value is list of proposer bids (UEs/VMs) to that receiver
    private final HashMap<Integer, ArrayList<Preference>> bids2Pms;
    private final HashMap<Integer, ArrayList<Preference>> pmEvaluations;
    private final HashMap<Integer,HashMap<Integer,Double>> energyCosts;
    private final HashMap<Integer,Double> pmPrices;

    // keep track of the available resources on each PM, considering the accepted UE/VM proposals
    private final HashMap<Integer, ResourceAvailability> pmResources;
    private final ArrayList<Ue2VmMapping> unmatchedUes = new ArrayList<>();
    private final double energyCoeff;
    private final double priceCoeff;
    private final double loadCoeff;
    private final double speedCoeff;
    private final double epsilon;


    public AuctionAlg(double energyCoeff, double priceCoeff, double loadCoeff, double speedCoeff) {
        super();
        this.bids2Pms = new HashMap<>();
        this.pmEvaluations = new HashMap<>();
        this.pmPrices = new HashMap<>();
        this.energyCosts = new HashMap<>();
        this.pmResources = new HashMap<>();
        this.energyCoeff = energyCoeff;
        this.priceCoeff = priceCoeff;
        this.loadCoeff = loadCoeff;
        this.speedCoeff = speedCoeff;
        this.epsilon = 1e-6;

        // keep track of the available resources on PMs
        for (PM pm : mecService.getPMs()) {
            pmResources.put(pm.getId(), new ResourceAvailability(pm.getId(), pm.getTotCores(), pm.getTotMemoryGB(), pm.getMaxVmsHosted()));
        }
    }

    public AuctionAlg() {
        this(.7,.125,.05, .125);
    }

    /**
     * Computes the energy costs for each UE/VM mapping to each PM.
     */
    private void computeEnergyCosts() {
        HashMap<Integer, Double> energyConsumptions = new HashMap<>();
        for (Ue2VmMapping mapping : mecService.getUe2VmMappings()) {
            energyConsumptions.clear();
            for (PM pm : mecService.getPMs()) {
                if (pmResources.get(pm.getId()).canPerformMatch(mapping.getCores(), mapping.getMemory())) {
                    double energyConsumption = energyService.getEnergyConsumptionWithVmCoresAndPm(mecService.getVM(mapping.getVmId()), mapping.getCores(), pm);
                    energyConsumptions.put(pm.getId(), energyConsumption);
                }
            }

            // Normalize the energy consumption values
            double maxConsumption = Math.max(energyConsumptions.values().stream().max(Comparator.naturalOrder()).orElse(0.), 100);

            energyCosts.putIfAbsent(mapping.getUeId(), new HashMap<>());
            energyConsumptions.forEach((pmId, cons) -> energyCosts.get(mapping.getUeId()).put(pmId, Math.log(1 + maxConsumption / (cons + epsilon))));
        }
    }

    /**
     * Evaluate PMs based on their load factor, compute speed, and price.
     */
    private void evaluatePMs(){
        pmEvaluations.clear();

        // min/max compute speed for normalization
        double minCompute = mecService.getPMs().stream().mapToDouble(PM::getCoreComputeOpsPerSec).min().orElse(0);
        double maxCompute = mecService.getPMs().stream().mapToDouble(PM::getCoreComputeOpsPerSec).max().orElse(1e-8);

        // list of unmatched UEs
        for (Ue2VmMapping mapping : unmatchedUes) {

            for (PM pm : mecService.getPMs()) {
                // ignore PMs that cannot host the VM
                if (!pmResources.get(pm.getId()).canPerformMatch(mapping.getCores(), mapping.getMemory()))
                    continue;

                // load factor (lower is better)
                double coresUsage = (double) pmResources.get(pm.getId()).getUsedCores() / pm.getTotCores();
                double memUsage = (double) pmResources.get(pm.getId()).getUsedMemory() / pm.getTotMemoryGB();
                double loadFactor = (coresUsage + memUsage) / 2.;

                // normalized compute speed (higher is better)
                double computeSpeed = (pm.getCoreComputeOpsPerSec() - minCompute) / (maxCompute - minCompute + 1e-8);

                // final PM evaluation (higher is better)
                double evaluation = - this.loadCoeff * loadFactor + this.speedCoeff * computeSpeed - this.priceCoeff * pmPrices.getOrDefault(pm.getId(), 0.0) + this.energyCoeff * energyCosts.get(mapping.getUeId()).get(pm.getId());

                pmEvaluations.putIfAbsent(mapping.getUeId(), new ArrayList<>());
                pmEvaluations.get(mapping.getUeId()).add(new Preference(mapping.getVmId(), pm.getId(), evaluation, mapping));
            }
        }
    }

    /**
     * Auction-based main loop algorithm to match VMs to PMs.
     */
    private void vm2PmAuction() {
        // list of not yet matched UEs
        unmatchedUes.clear();
        unmatchedUes.addAll(mecService.getUe2VmMappings());

        // list of new matches at each iteration/round
        ArrayList<Preference> newMatches = new ArrayList<>();

        while (!unmatchedUes.isEmpty()) {
            this.evaluatePMs();

            // UE/VM matches to delete because no PM can host them
            ArrayList<Integer> toDelete = new ArrayList<>();

            for (Map.Entry<Integer, ArrayList<Preference>> entry : pmEvaluations.entrySet()) {
                // ignore UEs/VMs that are already matched
                if (finalMatches.stream().anyMatch(m -> m.getUe2VmMapping().getUeId()==entry.getKey())) {
                    continue;
                }

                // ignore (and delete from "unmatchedUes" list) UEs/VMs for which no PM can host them (no preferences for PMs left)
                if (entry.getValue().isEmpty()) {
                    toDelete.add(entry.getKey());
                    continue;
                }

                // get argmax of pmEvaluations
                entry.getValue().sort((b1, b2) -> new PreferenceComparator().compare(b1, b2));
                Preference bestPref = entry.getValue().get(0);

                // ignore PMs that cannot host the UE/VM
                if (!pmResources.get(bestPref.getReceiver()).canPerformMatch(bestPref.getUe2VmMapping().getCores(), bestPref.getUe2VmMapping().getMemory())) {
                    continue;
                }

                // get the second-best PM value
                double bestVal = bestPref.getPreference();
                double secondBestValue = entry.getValue().size() >= 2 ? entry.getValue().get(1).getPreference() : bestVal;

                // compute bid:     bid = valuation(j*) - second_highest_valuation + epsilon
                double bid = bestVal - secondBestValue + epsilon;
                bids2Pms.putIfAbsent(bestPref.getReceiver(), new ArrayList<>());
                bids2Pms.get(bestPref.getReceiver()).add(new Preference(bestPref.getProposer(), bestPref.getReceiver(), bid, bestPref.getUe2VmMapping()));
            }

            if (bids2Pms.isEmpty()){
                // no PM can host any VM
                break;
            }

            // loop over all the proposed bids
            for (Map.Entry<Integer, ArrayList<Preference>> bids2Pm : bids2Pms.entrySet()) {
                // sort the bids by their value
                ArrayList<Preference> bids = bids2Pm.getValue();
                bids.sort((b1, b2) -> new PreferenceComparator().compare(b1, b2));

                // get the winning bid and add it to the new matches
                Preference winningBid = bids.get(0);
                newMatches.add(winningBid);

                // update the price of the PM
                Ue2VmMapping mapping = winningBid.getUe2VmMapping();
                double currPrice = pmPrices.getOrDefault(bids2Pm.getKey(), 0.0);
                double updatedPrice = 0.6 * winningBid.getPreference() + (1 - 0.6) * currPrice;
                pmPrices.put(bids2Pm.getKey(), updatedPrice);

                // (temporarily) allocate the PM resources
                pmResources.get(bids2Pm.getKey()).allocateResources(mapping.getCores(), mapping.getMemory());
            }

            // remove the unmatched UEs for which there are no PMs available
            unmatchedUes.removeIf(m -> toDelete.contains(m.getUeId()));

            // remove the matched UEs/VMs from the unmatched list
            newMatches.forEach(match -> unmatchedUes.removeIf(mapping -> mapping.getUeId() == match.getUe2VmMapping().getUeId()));

            // add new matches to the final UE/VM/PM mapping list
            this.finalMatches.addAll(newMatches);
            newMatches.clear();

            // clear current iteration's bids
            bids2Pms.clear();
        }
    }

    /**
     * Auction-based algorithm: compupte energy costs, perform auction-based matching, allocate matches to PMs, and prepare results.
     * @param verbose whether to print verbose output
     * @return the results of the algorithm
     */
    @Override
    public AlgorithmResults run(boolean verbose) {

        // compute the energy costs for each (used) VM to each PMs
        this.computeEnergyCosts();

        // perform the UE/VM-PM auction
        this.vm2PmAuction();

        // allocate the matches to the PMs based on the found matches
        this.allocateMatchesToPMs();

        // return the algorithm results
        return prepareResults();
    }

    @Override
    public String getName() {
        return "Auction-based";
    }
}