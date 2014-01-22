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
	
	public int getSize() {
		if (this instanceof ArmInstComment || this instanceof ArmInstPseudoLabel) {
			return 0;
		}
		return 1;
	}
	
	public int getDistanceToStart() {
		int size = 0;
		ArmInst inst = prev;
		while (inst != null) {
			size += inst.getSize();
			inst = inst.prev;
		}
		return size;
	}
	
	public int getDistanceToEnd() {
		int size = getSize();
		ArmInst inst = next;
		while (inst != null) {
			size += inst.getSize();
			inst = inst.next;
		}
		return size;
	}
	
	/*
	 * Before: (prev2 -> ) prev1 -> this -> next
	 * After:  (prev2 -> )          this -> next
	 */
	public void removePrevious() {
		if (prev != null) {	
			if (prev.prev != null) {
				prev.prev.next = this;
			}
			prev = prev.prev;
		}
	}
	
	/*
	 * Before: prev -> this -> next1 (-> next2)
	 * After:  prev -> this          (-> next2)
	 */
	public void removeNext() {
		if (next != null) {
			if (next.next != null) {
				next.next.prev = this;
			}
			next = next.next;
		}
	}
	
	/*
	 * Before: (prev ->) this (-> next)
	 * After:  (prev ->)      (-> next)
	 */
	public void removeSelf() {
		if (prev != null) {
			prev.next = next;
		}
		if (next != null) {
			next.prev = prev;
		}
	}
	
	/*
	 * Before: this
	 * After:  *prev* -> this
	 */
	public void linkToPrevious(ArmInst prev) {
		prev.next = this;
		this.prev = prev;
	}

	/*
	 * Before: this
	 * After:  this -> *next*
	 */
	public void linkToNext(ArmInst next) {
		this.next = next;
		next.prev = this;
	}
	
	/*
	 * Before: this (-> something)
	 * After:  this -> *next* (-> something)
	 */
	public void insertAfter(ArmInst inst) {
		if (this.next != null) {
			inst.next = this.next;
			inst.next.prev = inst;
		}
		
		this.next = inst;
		inst.prev = this;
	}
	
	/*
	 * Before: (something ->) this
	 * After:  (something ->) *prev* -> this
	 */
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
