package algorithm;

import algorithm.model.AlgorithmResults;
import algorithm.model.ResourceAvailability;
import algorithm.model.Preference;
import algorithm.model.Ue2VmMapping;
import algorithm.utils.PreferenceComparator;
import model.PM;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GaleShapleyAlg extends MatchingAlg {
    private final HashMap<Integer, ArrayList<Preference>> vmsPreferences;
    private final HashMap<Integer, ArrayList<Preference>> pmsPreferences;
    // keep track of the available resources on each PM, considering the (temporarily) accepted UE/VM proposals
    private final HashMap<Integer, ResourceAvailability> pmResources;
    private final double loadBalancingCoeff;
    private final double consolidationCoeff;
    private final double energyCoeff;
    private final boolean onlyAcceptsBestMatch;
    private final boolean useDynamicPrefs;
    private final boolean fragmentationInVMPreference;

    public GaleShapleyAlg() {
        this(.5,1.,4., true, true, false);

    }
    public GaleShapleyAlg(boolean onlyAcceptsBestMatch, boolean useDynamicPrefs, boolean fragmentationInVMPreference) {
        this(.1,10.,1., onlyAcceptsBestMatch, useDynamicPrefs, fragmentationInVMPreference);
    }

    public GaleShapleyAlg(double loadBalancingCoeff, double consolidationCoeff, double energyCoeff) {
        this(loadBalancingCoeff, consolidationCoeff, energyCoeff, true, true, false);
    }

    public GaleShapleyAlg(double loadBalancingCoeff, double consolidationCoeff, double energyCoeff, boolean onlyAcceptsBestMatch, boolean useDynamicPrefs, boolean fragmentationInVMPreference) {
        this.loadBalancingCoeff = loadBalancingCoeff;
        this.consolidationCoeff = consolidationCoeff;
        this.energyCoeff = energyCoeff;
        this.onlyAcceptsBestMatch = onlyAcceptsBestMatch;
        this.fragmentationInVMPreference = fragmentationInVMPreference;
        this.useDynamicPrefs = useDynamicPrefs;
        this.vmsPreferences = new HashMap<>();
        this.pmsPreferences = new HashMap<>();
        this.pmResources = new HashMap<>();
    }

    /**
     * Compute the preferences of all possible VM-to-PM match (and vice-versa) based on the available resources and energy consumption.
     */
    protected void computePreferences() {
        HashMap<Ue2VmMapping, ArrayList<PM>> mappingMatches = new HashMap<>();
        for (Ue2VmMapping ue2VmMapping : mecService.getUe2VmMappings()) {
            for (PM pm : mecService.getPMs()) {
                mappingMatches.putIfAbsent(ue2VmMapping, new ArrayList<>());

                if (!mappingMatches.get(ue2VmMapping).contains(pm))
                    mappingMatches.get(ue2VmMapping).add(pm);
            }
        }

        computePreferences(mappingMatches);
    }

    /**
     * Compute the preferences of the VMs and PMs, considering only the specified VM-to-PM assignments.
     * @param uePrefs assignments to be considered (i.e. the VM preferences to be computed for the PMs)
     */
    protected void computePreferences(List<ArrayList<Preference>> uePrefs) {
        HashMap<Ue2VmMapping, ArrayList<PM>> mappingMatches = new HashMap<>();

        for (List<Preference> uePref : uePrefs) {
            for (Preference pref : uePref) {

                Ue2VmMapping ue2VmMapping = pref.getUe2VmMapping();
                PM pm = mecService.getPM(pref.getReceiver());
                mappingMatches.putIfAbsent(ue2VmMapping, new ArrayList<>());

                if (!mappingMatches.get(ue2VmMapping).contains(pm))
                    mappingMatches.get(ue2VmMapping).add(pm);
            }
        }

        computePreferences(mappingMatches);
    }

    /**
     * Compute the preferences of the VMs and PMs, considering only the specified VM-to-PM assignments.
     * @param mappingMatches assignments to be considered
     */
    private void computePreferences(HashMap<Ue2VmMapping, ArrayList<PM>> mappingMatches) {
        HashMap<Integer, ArrayList<Preference>> tempVmsPreferences = new HashMap<>();
        HashMap<Integer, ArrayList<Preference>> tempPmsPreferences = new HashMap<>();
        HashMap<Integer, ArrayList<Double>> pmsEnergyConsumptions = new HashMap<>();
        ArrayList<Double> energyConsumptions = new ArrayList<>();

        for (Ue2VmMapping ue2VmMapping : mappingMatches.keySet()) {
            int vmId = ue2VmMapping.getVmId();
            int cores = ue2VmMapping.getCores();
            int memory = ue2VmMapping.getMemory();

            energyConsumptions.clear();
            for (PM pm : mappingMatches.get(ue2VmMapping)) {
                if (mecService.checkEnoughPmResources(vmId, pm.getId(), cores, memory) && mecService.checkAssignmentAllowed(vmId, pm.getId())) {

                    double energyConsumption = energyService.getEnergyConsumptionWithVmCoresAndPm(mecService.getVM(vmId), cores, pm);
                    energyConsumptions.add(energyConsumption);

                    // vmPreference = -energyCoeff*energyConsumptionPerVm(vm, cores, pm) + loadBalancingCoeff*availableResrcs(pm)
                    double vmPartialPreference = this.loadBalancingCoeff * (((double)pmResources.get(pm.getId()).getAvailableCores())/pmResources.get(pm.getId()).getTotCores() +
                            ((double)pmResources.get(pm.getId()).getAvailableMemory()/pmResources.get(pm.getId()).getTotMemory()))/2.0;

                    // pmPreference = -energyCoeff*energyConsumptionPerPm(pm, cores, vm) + consolidationCoeff*resrcsUsage(pm)
                    double pmPartialPreference = this.consolidationCoeff * (((double)pmResources.get(pm.getId()).getUsedCores() + cores)/pmResources.get(pm.getId()).getTotCores() +
                            ((double)pmResources.get(pm.getId()).getUsedMemory() + memory)/pmResources.get(pm.getId()).getTotMemory())/2.0;

                    if (this.fragmentationInVMPreference){
                        vmPartialPreference += this.consolidationCoeff * (((double)pmResources.get(pm.getId()).getUsedCores() + cores)/pmResources.get(pm.getId()).getTotCores() +
                                ((double)pmResources.get(pm.getId()).getUsedMemory() + memory)/pmResources.get(pm.getId()).getTotMemory())/2.0;

                        pmPartialPreference = 0;
                    }

                    tempVmsPreferences.putIfAbsent(vmId, new ArrayList<>());
                    tempVmsPreferences.get(vmId).add(new Preference(vmId, pm.getId(), vmPartialPreference, ue2VmMapping));

                    pmsEnergyConsumptions.putIfAbsent(pm.getId(), new ArrayList<>());
                    pmsEnergyConsumptions.get(pm.getId()).add(energyConsumption);
                    tempPmsPreferences.putIfAbsent(pm.getId(), new ArrayList<>());
                    tempPmsPreferences.get(pm.getId()).add(new Preference(vmId, pm.getId(), pmPartialPreference, ue2VmMapping));
                }
            }
            // normEnergyCons = (energyConsumption - minEnergyCons) / (maxEnergyCons - minEnergyCons)
            double minEnergyCons = energyConsumptions.stream().min(Double::compare).orElse(0.);
            double maxEnergyCons = energyConsumptions.stream().max(Double::compare).orElse(1e-8);

            if (Math.abs(maxEnergyCons - minEnergyCons) < 1e-8)
                continue;
            for (int i = 0; i < energyConsumptions.size(); i++) {
                double normalizedEnergy = this.energyCoeff * (energyConsumptions.get(i) - minEnergyCons) / (maxEnergyCons - minEnergyCons + 1e-6);
                tempVmsPreferences.get(vmId).get(i).setPreference(tempVmsPreferences.get(vmId).get(i).getPreference() - normalizedEnergy);
            }
        }

        for (Map.Entry<Integer, ArrayList<Double>> entry : pmsEnergyConsumptions.entrySet()) {
            double minEnergyCons = entry.getValue().stream().min(Double::compare).orElse(0.);
            double maxEnergyCons = entry.getValue().stream().max(Double::compare).orElse(1e-8);

            if (Math.abs(maxEnergyCons - minEnergyCons) < 1e-8)
                continue;
            for (int i = 0; i < entry.getValue().size(); i++) {
                    double normalizedEnergy = this.energyCoeff * (entry.getValue().get(i) - minEnergyCons) / (maxEnergyCons - minEnergyCons + 1e-6);
                    tempPmsPreferences.get(entry.getKey()).get(i).setPreference(tempPmsPreferences.get(entry.getKey()).get(i).getPreference() - normalizedEnergy);
            }
        }

        tempPmsPreferences.replaceAll((k, v) -> IntStream.range(0, v.size())
                .boxed()
                .sorted((o1, o2) -> new PreferenceComparator().compare(v.get(o1), v.get(o2)))
                .map(v::get).collect(Collectors.toCollection(ArrayList::new)));

        tempVmsPreferences.replaceAll((k, v) -> IntStream.range(0, v.size())
                .boxed()
                .sorted((o1, o2) -> new PreferenceComparator().compare(v.get(o1), v.get(o2)))
                .map(v::get).collect(Collectors.toCollection(ArrayList::new)));

        this.vmsPreferences.clear();
        this.pmsPreferences.clear();
        this.vmsPreferences.putAll(tempVmsPreferences);
        this.pmsPreferences.putAll(tempPmsPreferences);
    }

    /**
     * Gale-Shapley main loop algorithm to match VMs to PMs.
     */
    private void vmToPmMatching() {
        ArrayList<Preference> matches = new ArrayList<>();

        // map of PMs and their partial matches (already accepted VMs)
        HashMap<Integer, ArrayList<Preference>> pmsPartialMatches = new HashMap<>();


        for (PM pm : mecService.getPMs()) {
            pmResources.put(pm.getId(), new ResourceAvailability(pm.getId(), pm.getTotCores(), pm.getTotMemoryGB(), pm.getMaxVmsHosted()));
        }

        this.computePreferences();

        // map of UEs/VMs and their preferences to PMs
        HashMap<Integer, ArrayList<Preference>> tempUesPrefs = new HashMap<>();
        for (Map.Entry<Integer, ArrayList<Preference>> vmPreferences : this.vmsPreferences.entrySet()) {
            for (Preference vmPref : vmPreferences.getValue()) {
                tempUesPrefs.putIfAbsent(vmPref.getUe2VmMapping().getUeId(), new ArrayList<>());
                tempUesPrefs.get(vmPref.getUe2VmMapping().getUeId()).add(vmPref);
            }
        }

        // list of not yet matched UEs
        ArrayList<Integer> unmatchedUes = new ArrayList<>(tempUesPrefs.keySet());

        // list of new matches at each iteration/round
        ArrayList<Preference> newMatches = new ArrayList<>();

        // loop until all UEs are matched (or have no preferences left)
        while (!unmatchedUes.isEmpty()){
            // clear the new matches list for the current iteration
            newMatches.clear();

            ArrayList<Integer> toRemove = new ArrayList<>();
            // loop over all unmatched UEs and propose to their top-choice PM
            for (Map.Entry<Integer, ArrayList<Preference>> uePreferences : tempUesPrefs.entrySet()) {
                if (unmatchedUes.contains(uePreferences.getKey())) {
                    if (!uePreferences.getValue().isEmpty()) {
                        newMatches.add(uePreferences.getValue().get(0));
                    } else {
                        // UE has no preferences left, remove it from the unmatched list
                        toRemove.add(uePreferences.getKey());
                    }
                }
            }

            // remove the unmatched UEs that have no preferences left
            unmatchedUes.removeAll(toRemove);
            toRemove.forEach(tempUesPrefs::remove);

            Set<Integer> pmIds = newMatches.stream().map(Preference::getReceiver).collect(Collectors.toSet());
            // loop over all PMs that received proposals
            for (int pmId : pmIds) {
                // get the current PM preferences for the UEs/VMs that proposed to it
                List<Preference> pmMatches = new ArrayList<>();
                for (Preference pmMatch : newMatches.stream().filter(p -> p.getReceiver() == pmId).toList()) {
                    // (temporarily) accept all UE/VM proposals: to avoid resource over-subscription, the PM will afterwards reject (based on its preferences) the worst ones
                    this.pmsPreferences.get(pmId).stream().filter(pref -> pref.getUe2VmMapping().getUeId() == pmMatch.getUe2VmMapping().getUeId()).findFirst().ifPresent(pmMatches::add);
                }
                // add the already matched UEs/VMs (in the previous iterations) to the current PM
                pmsPartialMatches.putIfAbsent(pmId, new ArrayList<>());

                // sort the matched UEs/VMs based on the PM preferences
                pmMatches.sort(new PreferenceComparator());

                // get the maximum capacity of the PM (in terms of VMs it can host)
                int maxPMCapacity = mecService.getPM(pmId).getMaxVmsHosted();

                List<Preference> matchesToRemove = new ArrayList<>();
                // accept first proposals such that the requested resources are available
                for (Preference pmMatch : pmMatches) {
                    if (pmResources.get(pmId).canPerformMatch(pmMatch.getUe2VmMapping().getCores(), pmMatch.getUe2VmMapping().getMemory())) {

                        // (temporarily) allocate the resources to the PM
                        pmResources.get(pmId).allocateResources(pmMatch.getUe2VmMapping().getCores(), pmMatch.getUe2VmMapping().getMemory());
                    }
                    else{
                        matchesToRemove.add(pmMatch);
                    }
                }
                // remove the matches that are not acceptable for resources
                pmMatches.removeAll(matchesToRemove);

                // accept the first proposals that fit the PM capacity (in terms of resources and # of VMs it can host)
                int totalAccepted = Math.min(pmMatches.size(), maxPMCapacity - pmsPartialMatches.get(pmId).size());

                // set to (max) 1 if accepting only one VM per iteration
                if (this.onlyAcceptsBestMatch)
                    totalAccepted = Math.min(totalAccepted, 1);

                List<Preference> accepted = new ArrayList<>(pmMatches.subList(0, totalAccepted));

                // remove the accepted UEs from the unmatched list
                accepted.forEach(p -> unmatchedUes.remove((Object)p.getUe2VmMapping().getUeId()));

                // add the accepted UEs to the alreadyMatched list (keep track of the already matched UEs to PMs)
                accepted.stream().filter(p -> !pmsPartialMatches.get(pmId).contains(p)).forEach(pmsPartialMatches.get(pmId)::add);

                // get the rejected UEs
                List<Preference> rejected = new ArrayList<>(pmMatches.subList(totalAccepted, pmMatches.size()));

                // release the PM resources (temporarily) allocated by the rejected UEs
                rejected.forEach(p -> pmResources.get(pmId).releaseResources(p.getUe2VmMapping().getCores(), p.getUe2VmMapping().getMemory()));

                // add UEs/VMs that were rejected by the PM because of resource unavailability
                rejected.addAll(matchesToRemove);

                rejected.forEach(p -> {
                    // add the rejected UEs to the unmatched list
                    if (!unmatchedUes.contains(p.getUe2VmMapping().getUeId())) {
                        unmatchedUes.add(p.getUe2VmMapping().getUeId());
                    }
                    // remove from the rejected UEs the PM preference of the one that rejected them
                    tempUesPrefs.get(p.getUe2VmMapping().getUeId()).removeIf(pref -> pref.getReceiver() == pmId);

                    pmsPartialMatches.get(pmId).removeIf(pref -> pref.getUe2VmMapping().getUeId() == p.getUe2VmMapping().getUeId());

                    // If UE (via its assigned VM) has no remaining PMs, remove it from unmatched list
                    if (tempUesPrefs.get(p.getUe2VmMapping().getUeId()).isEmpty())
                        unmatchedUes.remove((Object)p.getUe2VmMapping().getUeId());
                });

                // add the accepted UEs to (and remove the rejected ones from) the matches list
                accepted.stream().filter(p -> !matches.contains(p)).forEach(matches::add);
                matches.removeAll(rejected);
            }

            if (this.useDynamicPrefs) {
                // recompute the preferences of the UEs/VMs based on the current PMs' preferences
                this.computePreferences(tempUesPrefs.values().stream().toList());

                // map of UEs/VMs and their preferences to PMs
                HashMap<Integer, ArrayList<Preference>> helperMap = new HashMap<>();
                for (Map.Entry<Integer, ArrayList<Preference>> vmPreferences : this.vmsPreferences.entrySet()) {
                    for (Preference vmPref : vmPreferences.getValue()) {
                        if (tempUesPrefs.getOrDefault(vmPref.getUe2VmMapping().getUeId(), new ArrayList<>()).stream().anyMatch(pref -> pref.getReceiver() == vmPref.getReceiver())) {
                            // remove the PM preference of the one that rejected them
                            helperMap.putIfAbsent(vmPref.getUe2VmMapping().getUeId(), new ArrayList<>());
                            helperMap.get(vmPref.getUe2VmMapping().getUeId()).add(vmPref);
                        }
                    }
                }
                tempUesPrefs.clear();
                tempUesPrefs.putAll(helperMap);
            }
        }

        if (matches.isEmpty()){
            System.out.println("NO MATCHES FOUND");
            System.out.println(mecService.getUe2VmMappings());
        }

        // add the matches to the final matches list
        this.finalMatches.addAll(matches);
    }

    /**
     * Gale-Shapley algorithm: perform matching, allocate matches to PMs, and prepare results.
     * @param verbose whether to print verbose output
     * @return the results of the algorithm
     */
    @Override
    public AlgorithmResults run(boolean verbose) {
        this.vmToPmMatching();

        this.allocateMatchesToPMs();

        return prepareResults();
    }

    @Override
    public String getName() {
        return "Gale-Shapley (" + (useDynamicPrefs ? "dynamic" : "static") + " prefs)";
    }
}