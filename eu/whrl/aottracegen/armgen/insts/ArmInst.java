package eu.whrl.aottracegen.armgen.insts;

public class ArmInst {
	public ArmInst prev;
	public ArmInst next;

	public ArmInst() {

	}

	public void linkToPrevious(ArmInst prev) {
		this.next = prev.next;
		prev.next = this;
		this.prev = prev;
	}

	public void linkToNext(ArmInst next) {
		this.next = next;
		next.prev = this;
		this.prev = next.prev;
	}
}
