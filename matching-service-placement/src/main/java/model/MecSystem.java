package model;

import algorithm.model.Ue2VmMapping;

import java.util.*;

public class MecSystem {
    private static MecSystem instance;
    private double totalDurationTime;
    private double offloadingDurationTime;
    private final ArrayList<VM> virtualMachines;
    private final ArrayList<PM> physicalMachines;
    private final ArrayList<UE> userEquipments;
    private final ArrayList<Ue2VmMapping> ue2VmMappings;


    private MecSystem(double totalDurationTime){
        this.totalDurationTime = totalDurationTime;
        this.offloadingDurationTime = this.totalDurationTime / 2;
        this.virtualMachines = new ArrayList<>();
        this.physicalMachines = new ArrayList<>();
        this.userEquipments = new ArrayList<>();
        this.ue2VmMappings = new ArrayList<>();
    }

    public static MecSystem getInstance(){
        if (instance == null) {
            double randomDurationTime = Math.max(new Random().nextGaussian(1., 0.2), 0.);
            instance = new MecSystem(randomDurationTime);
        }
        return instance;
    }

    public static MecSystem getInstance(double totalDurationTime){
        if (instance == null)
            instance = new MecSystem(totalDurationTime > 0 ? totalDurationTime : 1.0);
        return instance;
    }

    public double getTotalDurationTime() {
        return totalDurationTime;
    }

    public double getOffloadingDurationTime() {
        return offloadingDurationTime;
    }

    public void setTotalDurationTime(double totalDurationTime) {
        this.totalDurationTime = totalDurationTime;
    }

    public void setOffloadingDurationTime(double offloadingDurationTime) {
        this.offloadingDurationTime = offloadingDurationTime;
    }

    public ArrayList<VM> getVirtualMachines() {
        return virtualMachines;
    }

    public ArrayList<PM> getPhysicalMachines() {
        return physicalMachines;
    }
    public ArrayList<UE> getUserEquipments() {
        return userEquipments;
    }
    public ArrayList<Ue2VmMapping> getUe2VmMappings() {
        return ue2VmMappings;
    }

    public VM getVM(int id){
        return virtualMachines.get(id);
    }

    public PM getPM(int id){
        return physicalMachines.get(id);
    }

    public UE getUE(int id){
        return userEquipments.get(id);
    }


    public void addVM(VM vm){
        virtualMachines.add(vm);
    }

    public void addPM(PM pm){
        physicalMachines.add(pm);
    }

    public void removeVM(VM vm){
        virtualMachines.remove(vm);
    }

    public void removePM(PM pm){
        physicalMachines.remove(pm);
    }

    public void addUE(UE ue){
        userEquipments.add(ue);
    }

    public void removeUE(UE ue){
        userEquipments.remove(ue);
    }

    public int getNumberOfPMs(){
        return physicalMachines.size();
    }

    public int getNumberOfVMs(){
        return virtualMachines.size();
    }

    public int getNumberOfUEs() {
        return userEquipments.size();
    }

    public void resetSystem() {
        this.totalDurationTime = 0;
        this.offloadingDurationTime = 0;
        this.virtualMachines.clear();
        this.physicalMachines.clear();
        this.userEquipments.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MecSystem mecSystem = (MecSystem) o;
        return totalDurationTime == mecSystem.totalDurationTime && virtualMachines.equals(mecSystem.virtualMachines) && physicalMachines.equals(mecSystem.physicalMachines) && userEquipments.equals(mecSystem.userEquipments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalDurationTime, virtualMachines, physicalMachines, userEquipments);
    }

}