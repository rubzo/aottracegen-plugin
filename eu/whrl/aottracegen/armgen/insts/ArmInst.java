package eu.whrl.aottracegen.armgen.insts;

import java.util.Iterator;

import eu.whrl.aottracegen.armgen.ArmConditionCode;
import eu.whrl.aottracegen.armgen.ArmOpcode;

public class ArmInst implements Iterable<ArmInst> {
	public boolean valid;
	public ArmInst prev;
	public ArmInst next;

	public ArmInst() {
		valid = true;
	}

	public void removePrevious() {
		if (prev != null) {
			if (prev.prev != null) {
				prev.prev.next = this;
			}
			prev = prev.prev;
		}
	}
	
	public void removeNext() {
		if (next != null) {
			if (next.next != null) {
				next.next.prev = this;
			}
			next = next.next;
		}
	}
	
	public void linkToPrevious(ArmInst prev) {
		prev.next = this;
		this.prev = prev;
	}

	public void linkToNext(ArmInst next) {
		this.next = next;
		next.prev = this;
	}
	
	public void insertAfter(ArmInst inst) {
		if (this.next != null) {
			inst.next = this.next;
			inst.next.prev = inst;
		}
		
		this.next = inst;
		inst.prev = this;
	}
	
	public void insertBefore(ArmInst inst) {
		if (this.prev != null) {
			inst.prev = this.prev;
			inst.prev.next = inst;
		}
		
		this.prev = inst;
		inst.next = this;
	}
	
	public void replace(ArmInst replacement) {
		replaceChain(replacement, replacement);
	}
	
	public void replaceChain(ArmInst groupStart, ArmInst groupEnd) {
		if (this.prev != null) {
			groupStart.linkToPrevious(this.prev);
		}
		if (this.next != null) {
			groupEnd.linkToNext(this.next);
		}
	}
	
	public Iterator<ArmInst> iterator() {
		return new Iterator<ArmInst>() {
			ArmInst nextInst = ArmInst.this;
			ArmInst releasedInst = null;
			
			public boolean hasNext() {
				return (nextInst != null);
			}
			
			public ArmInst next() {
				releasedInst = nextInst;
				nextInst = nextInst.next;
				return releasedInst;
			}
			
			public void remove() {
				if (releasedInst != null) {
					releasedInst.prev.next = nextInst;
					nextInst.prev = releasedInst.prev;
				}
			}
			
		};
	}
	
	public ArmOpcode getOpcode() {
		if (this instanceof ArmInstOp) {
			ArmInstOp op = (ArmInstOp) this;
			return op.opcode;
		}
		return ArmOpcode.INVALID;
	}
	
	public ArmConditionCode getConditionCode() {
		if (this instanceof ArmInstOp) {
			ArmInstOp op = (ArmInstOp) this;
			return op.cc;
		}
		return ArmConditionCode.INVALID;
	}
}
