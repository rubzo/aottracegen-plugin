package eu.whrl.aottracegen.exceptions;

import java.util.List;

import eu.whrl.aottracegen.RecoverableError;

public class CommandException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public List<RecoverableError> recoverableErrors = null;
	public boolean isRecoverable = false;
	
	public CommandException attachRecoverableErrors(List<RecoverableError> errors) {
		recoverableErrors = errors;
		isRecoverable = true;
		return this;
	}
}
