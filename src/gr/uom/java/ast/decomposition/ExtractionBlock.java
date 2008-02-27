package gr.uom.java.ast.decomposition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.LocalVariableInstructionObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.ParameterObject;
import gr.uom.java.ast.association.Association;

public class ExtractionBlock {
	private String extractedMethodName;
	private LocalVariableDeclarationObject returnVariableDeclaration;
	private VariableDeclarationStatement returnVariableDeclarationStatement;
	private List<AbstractStatement> statementsForExtraction;
	private List<String> assignmentOperators;
	private CompositeStatementObject parentStatementForCopy;
	private Map<LocalVariableDeclarationObject, VariableDeclarationStatement> additionalRequiredVariableDeclarationStatementMap;
	
	public ExtractionBlock(LocalVariableDeclarationObject returnVariableDeclaration,
			VariableDeclarationStatement returnVariableDeclarationStatement,
			List<AbstractStatement> statementsForExtraction, List<String> assignmentOperators) {
		this.extractedMethodName = returnVariableDeclaration.getName();
		this.returnVariableDeclaration = returnVariableDeclaration;
		this.returnVariableDeclarationStatement = returnVariableDeclarationStatement;
		this.statementsForExtraction = statementsForExtraction;
		this.assignmentOperators = assignmentOperators;
		this.parentStatementForCopy = null;
		this.additionalRequiredVariableDeclarationStatementMap = new LinkedHashMap<LocalVariableDeclarationObject, VariableDeclarationStatement>();
	}

	public String getExtractedMethodName() {
		return extractedMethodName;
	}

	public void setExtractedMethodName(String extractedMethodName) {
		this.extractedMethodName = extractedMethodName;
	}

	public LocalVariableDeclarationObject getReturnVariableDeclaration() {
		return returnVariableDeclaration;
	}

	public VariableDeclarationStatement getReturnVariableDeclarationStatement() {
		return returnVariableDeclarationStatement;
	}

	public List<AbstractStatement> getStatementsForExtraction() {
		return statementsForExtraction;
	}

	public List<String> getAssignmentOperators() {
		return assignmentOperators;
	}

	public AbstractStatement getParentStatementForCopy() {
		return parentStatementForCopy;
	}

	public void setParentStatementForCopy(CompositeStatementObject parentStatementForCopy) {
		this.parentStatementForCopy = parentStatementForCopy;
	}

	public void addRequiredVariableDeclarationStatement(LocalVariableDeclarationObject key, VariableDeclarationStatement value) {
		this.additionalRequiredVariableDeclarationStatementMap.put(key, value);
	}

	public Set<LocalVariableDeclarationObject> getAdditionalRequiredVariableDeclarations() {
		return this.additionalRequiredVariableDeclarationStatementMap.keySet();
	}

	public VariableDeclarationStatement getAdditionalRequiredVariableDeclarationStatement(LocalVariableDeclarationObject key) {
		return this.additionalRequiredVariableDeclarationStatementMap.get(key);
	}

	public boolean validTargetObject(ClassObject sourceClass, ClassObject targetClass, MethodObject sourceMethod) {
		ITypeBinding targetClassBinding = targetClass.getTypeDeclaration().resolveBinding();
		List<LocalVariableInstructionObject> localVariableInstructions = new ArrayList<LocalVariableInstructionObject>();
    	if(parentStatementForCopy != null) {
    		for(AbstractExpression expression : parentStatementForCopy.getExpressions()) {
    			localVariableInstructions.addAll(expression.getLocalVariableInstructions());
    		}
    	}
    	for(AbstractStatement statement : statementsForExtraction) {
    		localVariableInstructions.addAll(statement.getLocalVariableInstructions());
    	}
    	for(LocalVariableInstructionObject localVariableInstruction : localVariableInstructions) {
    		if(localVariableInstruction.getType().getClassType().equals(targetClass.getName())) {
    			VariableDeclarationStatement variableDeclarationStatement = null;
    			LocalVariableDeclarationObject localVariableDeclaration = localVariableInstruction.generateLocalVariableDeclaration();
    			for(AbstractStatement statement : statementsForExtraction) {
    				if(statement instanceof CompositeStatementObject) {
    					CompositeStatementObject compositeStatement = (CompositeStatementObject)statement;
    					variableDeclarationStatement = compositeStatement.getVariableDeclarationStatement(localVariableDeclaration);
    				}
    				else if(statement instanceof StatementObject) {
    					StatementObject statementObject = (StatementObject)statement;
    					if(statementObject.containsLocalVariableDeclaration(localVariableDeclaration))
    						variableDeclarationStatement = (VariableDeclarationStatement)statementObject.getStatement();
    				}
    			}
    			if(variableDeclarationStatement != null)
    				return false;
    		}
    	}
    	for(LocalVariableInstructionObject localVariableInstruction : localVariableInstructions) {
    		if(localVariableInstruction.getType().getClassType().equals(targetClass.getName())) {
    			LocalVariableDeclarationObject localVariableDeclaration = localVariableInstruction.generateLocalVariableDeclaration();
    			VariableDeclarationStatement variableDeclarationStatement = sourceMethod.getVariableDeclarationStatement(localVariableDeclaration);
				if(variableDeclarationStatement != null)
					return true;
				ListIterator<ParameterObject> parameterIterator = sourceMethod.getParameterListIterator();
				while(parameterIterator.hasNext()) {
					ParameterObject parameter = parameterIterator.next();
					if(localVariableInstruction.getName().equals(parameter.getName()) && parameter.getType().getArrayDimension() == 0)
						return true;
				}
    		}
    	}
    	List<FieldInstructionObject> fieldInstructions = new ArrayList<FieldInstructionObject>();
    	if(parentStatementForCopy != null) {
    		for(AbstractExpression expression : parentStatementForCopy.getExpressions()) {
    			fieldInstructions.addAll(expression.getFieldInstructions());
    		}
    	}
    	for(AbstractStatement statement : statementsForExtraction) {
    		fieldInstructions.addAll(statement.getFieldInstructions());
    	}
    	for(FieldInstructionObject fieldInstruction : fieldInstructions) {
    		ITypeBinding fieldTypeBinding = fieldInstruction.getSimpleName().resolveTypeBinding();
    		if(fieldInstruction.getType().getClassType().equals(targetClass.getName()) || fieldTypeBinding.isEqualTo(targetClassBinding) ||
    				targetClassBinding.isEqualTo(fieldTypeBinding.getSuperclass())) {
				ListIterator<FieldObject> fieldIterator = sourceClass.getFieldIterator();
				while(fieldIterator.hasNext()) {
					FieldObject field = fieldIterator.next();
					if(fieldInstruction.getName().equals(field.getName()) && field.getType().getArrayDimension() == 0)
						return true;
				}
    		}
    	}
    	List<MethodInvocationObject> methodInvocations = new ArrayList<MethodInvocationObject>();
    	if(parentStatementForCopy != null) {
    		for(AbstractExpression expression : parentStatementForCopy.getExpressions()) {
    			methodInvocations.addAll(expression.getMethodInvocations());
    		}
    	}
    	for(AbstractStatement statement : statementsForExtraction) {
    		methodInvocations.addAll(statement.getMethodInvocations());
    	}
    	for(MethodInvocationObject methodInvocation : methodInvocations) {
    		if(methodInvocation.getOriginClassName().equals(sourceClass.getName())) {
    			MethodObject invokedMethod = sourceClass.getMethod(methodInvocation);
    			FieldInstructionObject fieldInstruction = invokedMethod.isGetter();
    			if(fieldInstruction != null && fieldInstruction.getType().getClassType().equals(targetClass.getName()))
    				return true;
    			MethodInvocationObject delegation = invokedMethod.isDelegate();
    			if(delegation != null && delegation.getOriginClassName().equals(targetClass.getName()))
    				return true;
    		}
    	}
    	return false;
    }
	
	public boolean oneToManyRelationshipWithTargetClass(List<Association> associations, ClassObject targetClass) {
		List<FieldInstructionObject> fieldInstructions = new ArrayList<FieldInstructionObject>();
    	if(parentStatementForCopy != null) {
    		for(AbstractExpression expression : parentStatementForCopy.getExpressions()) {
    			fieldInstructions.addAll(expression.getFieldInstructions());
    		}
    	}
    	for(AbstractStatement statement : statementsForExtraction) {
    		fieldInstructions.addAll(statement.getFieldInstructions());
    	}
    	for(Association association : associations) {
    		FieldObject fieldObject = association.getFieldObject();
    		for(FieldInstructionObject fieldInstruction : fieldInstructions) {
    			if(fieldObject.equals(fieldInstruction) && targetClass.getName().equals(association.getTo()) &&
    					association.isContainer())
    				return true;
    		}
    	}
    	return false;
	}
}
