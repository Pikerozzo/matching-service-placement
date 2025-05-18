package service;

import algorithm.model.Ue2VmMapping;
import model.*;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class MecSystemService {
    private static MecSystemService instance = null;
    private static MecSystem mecSystem = null;
    private static MecMapping mapping;

    private MecSystemService() {
        mecSystem = MecSystem.getInstance();
        mapping = MecMapping.getInstance();
    }

    private MecSystemService(double totalDurationTime) {
        mecSystem = MecSystem.getInstance(totalDurationTime);
        mapping = MecMapping.getInstance();
    }

    public static MecSystemService getInstance() {
        if (instance == null) {
            instance = new MecSystemService();
        }
        return instance;
    }

    public static MecSystemService getInstance(double totalDurationTime){
        if (instance == null) {
            instance = new MecSystemService(totalDurationTime);
        }
        return instance;
    }

    /**
     * Get the binary mapping between VMs and PMs
     * @return the binary mapping between VMs and PMs
     */
    public ArrayList<ArrayList<Integer>> getVm2PmPlacement() {
        return mapping.getVm2PmPlacement();
    }

    /**
     * Get the number of GBs of each VM assigned to each PM
     * @return the number of GBs of each VM assigned to each PM
     */
    public ArrayList<ArrayList<Integer>> getVmGb2PmPlacement() {
        return mapping.getVmGb2PmPlacement();
    }

    /**
     * Get the number of cores of each VM assigned to each PM
     * @return the number of cores of each VM assigned to each PM
     */
    public ArrayList<ArrayList<Integer>> getVmCores2PmPlacement() {
        return mapping.getVmCores2PmPlacement();
    }

    /**
     * Get the PMs assigned to a specific VM
     * @param vmId the VM id
     * @return PMs assigned to a specific VM
     */
    public ArrayList<Integer> getPmsHostingVm(int vmId) throws IllegalArgumentException {
        if (vmId < 0 || vmId >= mecSystem.getNumberOfVMs())
            throw new IllegalArgumentException("Invalid VM id");

        ArrayList<Integer> pms = new ArrayList<>();
        for (int i = 0; i < mecSystem.getNumberOfPMs(); i++){
            if (mapping.getVm2PmPlacement().get(vmId).get(i) == 1)
                pms.add(i);
        }

        return pms;
    }


    /**
     * Get the number of PMs hosting a specific VM
     * @param vmId the VM id
     * @return the number of PMs hosting the VM
     */
    public int getTotPmsHostingVm(int vmId) throws IllegalArgumentException{
        if (vmId < 0 || vmId >= mecSystem.getNumberOfVMs())
            throw new IllegalArgumentException("Invalid VM id");

        return getPmsHostingVm(vmId).size();
    }

    /**
     * Get the VMs hosted by a specific PM
     * @param pmId the PM id
     * @return the VMs hosted by the PM
     */
    public ArrayList<Integer> getVmsHostedByPm(int pmId) throws IllegalArgumentException {
        if (pmId < 0 || pmId >= mecSystem.getNumberOfPMs())
            throw new IllegalArgumentException("Invalid PM id");

        ArrayList<Integer> vms = new ArrayList<>();
        for (int i = 0; i < mecSystem.getNumberOfVMs(); i++){
            if (mapping.getVm2PmPlacement().get(i).get(pmId) == 1)
                vms.add(i);
        }

        return vms;
    }

    /**
     * Get the number of VMs hosted by a specific PM
     * @param pmId the PM id
     * @return the number of VMs hosted by the PM
     */
    public int getTotVmsHostedByPm(int pmId) throws IllegalArgumentException{
        if (pmId < 0 || pmId >= mecSystem.getNumberOfPMs())
            throw new IllegalArgumentException("Invalid PM id");

        return getVmsHostedByPm(pmId).size();
    }

    /**
     * Check if the assignment of a VM to a PM is allowed in terms of resource availability and VM/PM assignment limits
     * @param vmId the VM id
     * @param pmId the PM id
     * @return true if the assignment is allowed, false otherwise
     */
    public boolean checkAssignmentAllowed(int vmId, int pmId) {
        if (vmId < 0 || vmId >= mecSystem.getNumberOfVMs() || pmId < 0 || pmId >= mecSystem.getNumberOfPMs())
            throw new IllegalArgumentException("Invalid VM or PM id");

        if (getVmsHostedByPm(pmId).contains(vmId))
            return true;

        if (getTotVmsHostedByPm(pmId) >= mecSystem.getPhysicalMachines().get(pmId).getMaxVmsHosted())
            return false;
        if (getTotPmsHostingVm(vmId) >= mecSystem.getVirtualMachines().get(vmId).getMaxPmPlacements())
            return false;

        return true;
    }

    /**
     * Check if the PM has enough resources to host a VM
     * @param vmId the VM id
     * @param pmId the PM id
     * @param vmCores the number of VM cores to be assigned to the PM
     * @param vmGbs the number of VM memory GBs to be assigned to the PM
     * @return true if the PM has enough resources, false otherwise
     */
    public boolean checkEnoughPmResources(int vmId, int pmId, int vmCores, int vmGbs){
        if (vmId < 0 || vmId >= mecSystem.getNumberOfVMs() || pmId < 0 || pmId >= mecSystem.getNumberOfPMs())
            throw new IllegalArgumentException("Invalid VM or PM id");

        if (!getVmsHostedByPm(pmId).contains(vmId) && getTotVmsHostedByPm(pmId) >= mecSystem.getPhysicalMachines().get(pmId).getMaxVmsHosted())
            return true;

        if (vmCores < 0 || vmGbs < 0)
            throw new IllegalArgumentException("Invalid number of cores or GBs");

        if (vmCores > mecSystem.getVirtualMachines().get(vmId).getTotCores() || vmGbs > mecSystem.getVirtualMachines().get(vmId).getTotMemoryGB())
            return false;

        if (getRemainingCoresInPm(pmId) < vmCores || getRemainingGbsInPm(pmId) < vmGbs)
            return false;

        return true;
    }

    /**
     * Check if the VM has enough resources to host a UE
     * @param vmId the VM id
     * @param ueCores the number of UE cores to be assigned to the VM
     * @param ueGbs the number of UE memory GBs to be assigned to the VM
     * @return true if the VM has enough resources, false otherwise
     */
    public boolean checkEnoughVmResources(int vmId, int ueCores, int ueGbs){
        if (vmId < 0 || vmId >= mecSystem.getNumberOfVMs())
            throw new IllegalArgumentException("Invalid VM id");

        return getVmCores2Pms(vmId).stream().mapToInt(Integer::intValue).sum() + ueCores <= mecSystem.getVirtualMachines().get(vmId).getTotCores() &&
                getVmGb2Pms(vmId).stream().mapToInt(Integer::intValue).sum() + ueGbs <= mecSystem.getVirtualMachines().get(vmId).getTotMemoryGB();
    }

    /**
     * Set the placement of a VM on a PM
     * @param vmId the VM id
     * @param pmId the PM id
     */
    public void setVmPlacementOnPm(int vmId, int pmId) throws IllegalArgumentException {
        if (checkAssignmentAllowed(vmId, pmId))
            mapping.setVmPlacement(vmId, pmId);
    }

    /**
     * Remove the placement of a VM on a PM
     * @param vmId the VM id
     * @param pmId the PM id
     */
    public void removeVmPlacementOnPm(int vmId, int pmId) throws IllegalArgumentException {
        if (vmId < 0 || vmId >= mecSystem.getNumberOfVMs() || pmId < 0 || pmId >= mecSystem.getNumberOfPMs())
            throw new IllegalArgumentException("Invalid VM or PM id");

        mapping.removeVmPlacement(vmId, pmId);
    }

    /**
     * Set the number of cores of a specific VM assigned to a specific PM
     * @param vmId the VM id
     * @param pmId the PM id
     * @param vmCores the number of cores to be assigned to the VM on the PM
     */
    public void setVmCores2Pm(int vmId, int pmId, int vmCores) throws IllegalArgumentException {
        if (!checkAssignmentAllowed(vmId, pmId))
            return;

        if (vmCores < 0)
            throw new IllegalArgumentException("Invalid number of cores");

        // check that the VM has enough cores
        // get already assigned cores of the VM to any other PM
        int currentlyAssignedVmCores2Pm = mapping.getVmCores2PmPlacement().get(vmId).get(pmId);
        int totalVmUsedCores = mapping.getVmCores2PmPlacement().get(vmId).stream().mapToInt(Integer::intValue).sum() - currentlyAssignedVmCores2Pm;
        if (vmCores > mecSystem.getVirtualMachines().get(vmId).getTotCores() || totalVmUsedCores + vmCores > mecSystem.getVirtualMachines().get(vmId).getTotCores())
            throw new IllegalArgumentException("Not enough cores in the VM");

        // check that the PM has enough cores
        int totalPMUsedCores = mapping.getVmCores2PmPlacement().stream().mapToInt(row -> row.get(pmId)).sum();
        if (totalPMUsedCores + vmCores - currentlyAssignedVmCores2Pm > mecSystem.getPhysicalMachines().get(pmId).getTotCores())
            throw new IllegalArgumentException("Not enough cores in the PM");

        mapping.setVmCores2Pm(vmId, pmId, vmCores);
        setVmPlacementOnPm(vmId, pmId);
    }

    /**
     * Set the number of memory GBs of a specific VM assigned to a specific PM
     * @param vmId the VM id
     * @param pmId the PM id
     * @param vmGbs the number of memory GBs to be assigned to the VM on the PM
     */
    public void setVmGb2Pm(int vmId, int pmId, int vmGbs){
        if (!checkAssignmentAllowed(vmId, pmId))
            return;

        if (vmGbs < 0)
            throw new IllegalArgumentException("Invalid number of GBs");

        // check that the VM has enough GBs
        int currentlyAssignedVmGbs2Pm = mapping.getVmGb2PmPlacement().get(vmId).get(pmId);
        int totalVmUsedGbs = mapping.getVmGb2PmPlacement().get(vmId).stream().mapToInt(Integer::intValue).sum() - currentlyAssignedVmGbs2Pm;
        if (vmGbs > mecSystem.getVirtualMachines().get(vmId).getTotMemoryGB() || totalVmUsedGbs + vmGbs > mecSystem.getVirtualMachines().get(vmId).getTotMemoryGB())
            throw new IllegalArgumentException("Not enough GBs in the VM");

        // check that the PM has enough GBs
        int totalPmUsedGb = mapping.getVmGb2PmPlacement().stream().mapToInt(row -> row.get(pmId)).sum();
        if (totalPmUsedGb + vmGbs - currentlyAssignedVmGbs2Pm > mecSystem.getPhysicalMachines().get(pmId).getTotMemoryGB())
            throw new IllegalArgumentException("Not enough GBs in the PM");

        mapping.setVmGb2Pm(vmId, pmId, vmGbs);
        setVmPlacementOnPm(vmId, pmId);
    }

    /**
     * Set the number of cores and memory GBs of a specific VM assigned to a specific PM
     * @param vmId the VM id
     * @param pmId the PM id
     * @param vmCores the number of cores to be assigned to the VM on the PM
     * @param vmGbs the number of memory GBs to be assigned to the VM on the PM
     */
    public void setVmResourcesOnPm(int vmId, int pmId, int vmCores, int vmGbs) throws IllegalArgumentException {
        if (vmCores == 0 && vmGbs == 0)
            removeVmPlacementOnPm(vmId, pmId);

        if (!checkAssignmentAllowed(vmId, pmId) || !checkEnoughPmResources(vmId, pmId, vmCores, vmGbs) || !checkEnoughVmResources(vmId, vmCores, vmGbs))
            return;

        setVmCores2Pm(vmId, pmId, vmCores);
        setVmGb2Pm(vmId, pmId, vmGbs);
    }

    /**
     * Add the number of cores and memory GBs of a specific VM assigned to a specific PM
     * @param vmId the VM id
     * @param pmId the PM id
     * @param vmCores the number of cores to be added to the VM on the PM
     * @param vmGbs the number of memory GBs to be added to the VM on the PM
     */
    public void addVMResourcesOnPm(int vmId, int pmId, int vmCores, int vmGbs) throws IllegalArgumentException {
        int currCores = getVmCores2Pm(vmId, pmId);
        int currGbs = getVmGb2Pm(vmId, pmId);

        setVmResourcesOnPm(vmId, pmId, currCores + vmCores, currGbs + vmGbs);
    }

    /**
     * Remove the number of cores and memory GBs of a specific VM assigned to a specific PM
     * @param vmId the VM id
     * @param pmId the PM id
     * @param vmCores the number of cores to be removed from the VM on the PM
     * @param vmGbs the number of memory GBs to be removed from the VM on the PM
     */
    public void removeVMResourcesOnPm(int vmId, int pmId, int vmCores, int vmGbs) throws IllegalArgumentException {
        int currCores = getVmCores2Pm(vmId, pmId);
        int currGbs = getVmGb2Pm(vmId, pmId);

        if (currCores - vmCores < 0 || currGbs - vmGbs < 0)
            throw new IllegalArgumentException("Cannot remove more resources than assigned");

        setVmResourcesOnPm(vmId, pmId, currCores - vmCores, currGbs - vmGbs);
    }

    /**
     * Get the number of memory GBs of a specific VM assigned to a specific PM
     * @param vmId the VM id
     * @param pmId the PM id
     * @return the number of cores assigned to the VM on the PM
     */
    public int getVmGb2Pm(int vmId, int pmId) {
        return mapping.getVmGb2PmPlacement().get(vmId).get(pmId);
    }

    /**
     * Get the number of memory GBs of a specific VM assigned to all PMs
     * @param vmId the VM id
     * @return the number of cores assigned to the VM on all PMs
     */
    public ArrayList<Integer> getVmGb2Pms(int vmId) {
        return mapping.getVmGb2PmPlacement().get(vmId);
    }

    /**
     * Get the number of cores of a specific VM assigned to a specific PM
     * @param vmId the VM id
     * @param pmId the PM id
     * @return the number of cores assigned to the VM on the PM
     */
    public int getVmCores2Pm(int vmId, int pmId) {
        return mapping.getVmCores2PmPlacement().get(vmId).get(pmId);
    }

    /**
     * Get the number of cores of a specific VM assigned to all PMs
     * @param vmId the VM id
     * @return the number of cores assigned to the VM on all PMs
     */
    public ArrayList<Integer> getVmCores2Pms(int vmId) {
        return mapping.getVmCores2PmPlacement().get(vmId);
    }

    /**
     * Get the number of cores of all VMs assigned to a specific PM
     * @param pmId the PM id
     * @return the number of cores of all VMs assigned to the PM
     */
    public ArrayList<Integer> getHostedVmsCoresByPm(int pmId) {
        return (ArrayList<Integer>) mapping.getVmCores2PmPlacement().stream().map(row -> row.get(pmId)).collect(Collectors.toList());
    }

    /**
     * Get the number of memory GBs of all VMs assigned to a specific PM
     * @param pmId the PM id
     * @return the number of memory GBs of all VMs assigned to the PM
     */
    public ArrayList<Integer> getHostedVmsGbsByPm(int pmId) {
        return (ArrayList<Integer>) mapping.getVmGb2PmPlacement().stream().map(row -> row.get(pmId)).collect(Collectors.toList());
    }

    /**
     * Get the number of remaining available cores in a specific PM
     * @param pmId the PM id
     * @return the number of remaining available cores
     */
    public int getRemainingCoresInPm(int pmId) {
        return mecSystem.getPM(pmId).getTotCores() - getHostedVmsCoresByPm(pmId).stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Get the number of remaining available memory GBs in a specific PM
     * @param pmId the PM id
     * @return the number of remaining available memory GBs
     */
    public int getRemainingGbsInPm(int pmId) {
        return mecSystem.getPM(pmId).getTotMemoryGB() - getHostedVmsGbsByPm(pmId).stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Get the number of remaining available cores in a specific VM
     * @param vmId the VM id
     * @return the number of remaining available cores
     */
    public int getRemainingCoresInVm(int vmId) {
        return mecSystem.getVM(vmId).getTotCores() - getVmCores2Pms(vmId).stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Get the number of remaining available memory GBs in a specific VM
     * @param vmId the PM id
     * @return the number of remaining available memory GBs
     */
    public int getRemainingGbsInVm(int vmId) {
        return mecSystem.getVM(vmId).getTotMemoryGB() - getVmGb2Pms(vmId).stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Add a new VM to the system
     * @param vm the VM to be added
     */
    public void addVM(VM vm){
        mecSystem.addVM(vm);
        mapping.addVm();
    }

    /**
     * Add a new PM to the system
     * @param pm the PM to be added
     */
    public void addPM(PM pm){
        mecSystem.addPM(pm);
        mapping.addPm();
    }

    /**
     * Remove a VM from the system
     * @param vm the VM to be removed
     */
    public void removeVM(VM vm){
        mecSystem.removeVM(vm);
        mapping.removeVm(vm.getId());
    }

    /**
     * Remove a PM from the system
     * @param pm the PM to be removed
     */
    public void removePM(PM pm){
        mecSystem.removePM(pm);
        mapping.removePm(pm.getId());
    }

    /**
     * Add a new UE to the system
     * @param ue the UE to be added
     */
    public void addUE(UE ue){
        mecSystem.addUE(ue);
    }

    /**
     * Remove a UE from the system
     * @param ue the UE to be removed
     */
    public void removeUE(UE ue){
        mecSystem.removeUE(ue);
    }

    /**
     * Get the number of physical machines in the system
     * @return the number of physical machines
     */
    public int getNumberOfPMs(){
        return mecSystem.getNumberOfPMs();
    }

    /**
     * Get the number of virtual machines in the system
     * @return the number of virtual machines
     */
    public int getNumberOfVMs(){
        return mecSystem.getNumberOfVMs();
    }

    /**
     * Get the number of user equipments in the system
     * @return the number of user equipments
     */
    public int getNumberOfUEs(){
        return mecSystem.getNumberOfUEs();
    }

    /**
     * Get the physical machine with the specified id
     * @param pmId the physical machine id
     * @return the specified physical machine
     */
    public PM getPM(int pmId){
        return mecSystem.getPhysicalMachines().get(pmId);
    }

    /**
     * Get the virtual machine with the specified id
     * @param vmId the virtual machine id
     * @return the specified virtual machine
     */
    public VM getVM(int vmId){
        return mecSystem.getVirtualMachines().get(vmId);
    }

    /**
     * Get the user equipment with the specified id
     * @param ueId the user equipment id
     * @return the specified user equipment
     */
    public UE getUE(int ueId){
        return mecSystem.getUserEquipments().get(ueId);
    }

    /**
     * Get the list of physical machines in the system
     * @return list of physical machines
     */
    public ArrayList<PM> getPMs(){
        return mecSystem.getPhysicalMachines();
    }

    /**
     * Get the list of virtual machines in the system
     * @return list of virtual machines
     */
    public ArrayList<VM> getVMs(){
        return mecSystem.getVirtualMachines();
    }

    /**
     * Get the list of user equipments in the system
     * @return list of user equipments
     */
    public ArrayList<UE> getUEs(){
        return mecSystem.getUserEquipments();
    }

    /**
     * Get the list of user equipment to virtual machine mappings
     * @return list of user equipment to virtual machine mappings
     */
    public ArrayList<Ue2VmMapping> getUe2VmMappings(){
        return mecSystem.getUe2VmMappings();
    }

    /**
     * Add a list of new UE-to-VM mappings to the system
     * @param ue2VmMappings the list of UE-to-VM mappings to be added
     */
    public void addUe2VmMappings(ArrayList<Ue2VmMapping> ue2VmMappings){
        mecSystem.getUe2VmMappings().addAll(ue2VmMappings);
    }

    /**
     * Add a new UE-to-VM mapping to the system
     * @param ue2VmMappings the UE-to-VM mapping to be added
     */
    public void addUe2VmMapping(Ue2VmMapping ue2VmMappings){
        mecSystem.getUe2VmMappings().add(ue2VmMappings);
    }

    /**
     * Reset the mapping of VMs to PMs (e.g. mapping reinitialization for consecutive runs of different algorithms)
     */
    public void resetMapping(){
        mapping.resetMapping();
    }

    /**
     * Reset the entire system (e.g. mapping and system elements reinitialization for complete restarts of the simulation)
     */
    public void resetSystem(){
        mapping.resetSystem();
        mecSystem.resetSystem();
    }

    /**
     * Set the total duration time of the system
     * @param totalDurationTime the total duration time of the system
     */
    public void setTotalDurationTime(double totalDurationTime) {
        mecSystem.setTotalDurationTime(totalDurationTime);
    }

    /**
     * Set the offloading duration time of the system
     * @param offloadingDurationTime the offloading duration time of the system
     */
    public void setOffloadingDurationTime(double offloadingDurationTime) {
        mecSystem.setOffloadingDurationTime(offloadingDurationTime);
    }

    /**
     * Get the number of total allocated PMs in the system
     * @return the number of total allocated PMs
     */
    public int getTotalAllocatedPms(){
        int totalPms = 0;
        for (int i = 0; i < mecSystem.getNumberOfPMs(); i++) {
            for (int j = 0; j < mecSystem.getNumberOfVMs(); j++) {
                if (mapping.getVm2PmPlacement().get(j).get(i) == 1) {
                    totalPms++;
                    break;
                }
            }
        }

        return totalPms;
    }

    /**
     * Get the number of total allocated VMs in the system
     * @return the number of total allocated VMs
     */
    public int getTotalAllocatedVms(){
        return (int) mapping.getVm2PmPlacement().stream().filter(row -> row.contains(1)).count();
    }

    /**
     * Get the printable representation of the VM-to-PM mapping
     * @return string of the VM-to-PM mapping
     */
    public String printResourceMapping(){
        return mapping.toString();
    }
}