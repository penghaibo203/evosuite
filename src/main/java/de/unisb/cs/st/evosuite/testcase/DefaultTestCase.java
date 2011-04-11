/*
 * Copyright (C) 2010 Saarland University
 * 
 * This file is part of EvoSuite.
 * 
 * EvoSuite is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * EvoSuite is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License along with
 * EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */

package de.unisb.cs.st.evosuite.testcase;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.unisb.cs.st.evosuite.assertion.Assertion;
import de.unisb.cs.st.evosuite.ga.ConstructionFailedException;
import de.unisb.cs.st.evosuite.ga.Randomness;
import de.unisb.cs.st.evosuite.testsuite.TestCallStatement;

/**
 * A test case is a list of statements
 * 
 * @author Gordon Fraser
 * 
 */
public class DefaultTestCase implements TestCase{

	private static Logger logger = Logger.getLogger(DefaultTestCase.class);

	/** The statements */
	private List<Statement> statements;

	// a list of all goals this test covers
	private HashSet<TestFitnessFunction> coveredGoals = new HashSet<TestFitnessFunction>();

		
	/**
	 * Constructor
	 */
	public DefaultTestCase() {
		statements = new ArrayList<Statement>();
	}

	/**
	 * Convenience constructor
	 * 
	 * @param statements
	 */
	public DefaultTestCase(List<Statement> statements) {
		this.statements = statements;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#size()
	 */
	@Override
	public int size() {
		return statements.size();
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return statements.isEmpty();
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#chop(int)
	 */
	@Override
	public void chop(int length) {
		while (statements.size() > length) {
			statements.remove(length);
		}
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#toCode()
	 */
	@Override
	public String toCode() {
		String code = "";
		for (Statement s : statements) {
			code += s.getCode() + "\n";
			code += s.getAssertionCode();
		}
		return code;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#toCode(java.util.Map)
	 */
	@Override
	public String toCode(Map<Integer, Throwable> exceptions) {
		String code = "";
		for (int i = 0; i < size(); i++) {
			Statement s = statements.get(i);
			if (exceptions.containsKey(i)) {
				code += s.getCode(exceptions.get(i)) + "\n";
				code += s.getAssertionCode();
			} else {
				code += s.getCode() + "\n";
				code += s.getAssertionCode(); // TODO: Handle semicolons
				                              // properly
			}
		}
		return code;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getObjects(java.lang.reflect.Type, int)
	 */
	@Override
	public List<VariableReference> getObjects(Type type, int position) {
		List<VariableReference> variables = new ArrayList<VariableReference>();
		// logger.trace("Looking for objects of type "+type
		// +" up to position "+position+" in test with length "+statements.size());
		for (int i = 0; i < position && i < size(); i++) {
			if (statements.get(i).retval == null)
				continue;
			if (statements.get(i).retval.isArray()) {
				if (GenericClass.isAssignable(type,
				        statements.get(i).retval.getComponentType())) {
					// Add components
					// variables.add(new
					// VariableReference(statements.get(i).retval.clone(),
					// Randomness.getInstance().nextInt(MAX_ARRAY), i));
					// ArrayStatement as = (ArrayStatement)statements.get(i);
					for (int index = 0; index < statements.get(i).retval.array_length; index++) {
						variables.add(new VariableReference(
						        statements.get(i).retval.clone(), index,
						        statements.get(i).retval.array_length, i));
					}
				}
			} else if (statements.get(i).retval.isArrayIndex()) { // &&
				                                                  // GenericClass.isAssignable(type,
				                                                  // statements.get(i).retval.array.getComponentType()))
				                                                  // {
				// Don't need to add this
			} else if (statements.get(i).retval.isAssignableTo(type)) {
				// if(constraint == null ||
				// constraint.isValid(statements.get(i).getReturnValue()))
				variables.add(new VariableReference(statements.get(i)
				        .getReturnType(), i));
				// else
				// logger.trace(statements.get(i).retval.getSimpleClassName()+" IS assignable to "+type+" but constrained");
				// variables.add(new VariableReference(type, i));
				// } else if(logger.isTraceEnabled()){
				// logger.trace(statements.get(i).retval.getSimpleClassName()+" is NOT assignable to "+type);
			}
		}
		logger.trace("Found " + variables.size() + " variables of type " + type);
		return variables;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getObjects(int)
	 */
	@Override
	public List<VariableReference> getObjects(int position) {
		List<VariableReference> variables = new ArrayList<VariableReference>();
		// logger.trace("Looking for objects of type "+type
		// +" up to position "+position+" in test with length "+statements.size());
		for (int i = 0; i < position && i < statements.size(); i++) {
			if (statements.get(i).retval == null)
				continue;
			// TODO: Need to support arrays that were not self-created
			if (statements.get(i).retval.isArray()) { // &&
				                                      // statements.get(i).retval.array
				                                      // != null) {
				// Add a single component
				// variables.add(new VariableReference(statements.get(i).retval,
				// Randomness.getInstance().nextInt(MAX_ARRAY), i));
				// Add components
				// ArrayStatement as = (ArrayStatement)statements.get(i);
				// for(int index = 0; index < as.size(); index++) {
				// variables.add(new VariableReference(as.retval.clone(), index,
				// as.size(), i));
				// }
				for (int index = 0; index < statements.get(i).retval.array_length; index++) {
					// variables.add(new
					// VariableReference(statements.get(i).retval.clone(),
					// index, statements.get(i).retval.array_length, i));
					variables.add(new VariableReference(
					        statements.get(i).retval.clone(), index, statements
					                .get(i).retval.array_length, i));
				}
			} else if (!statements.get(i).retval.isArrayIndex()) {
				variables.add(new VariableReference(statements.get(i)
				        .getReturnType(), i));
			}
			// logger.trace(statements.get(i).retval.getSimpleClassName());
		}
		logger.trace("Found " + variables.size() + " variables");
		return variables;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getRandomObject()
	 */
	@Override
	public VariableReference getRandomObject() {
		return getRandomObject(statements.size());
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getRandomObject(int)
	 */
	@Override
	public VariableReference getRandomObject(int position) {
		List<VariableReference> variables = getObjects(position);
		if (variables.isEmpty())
			return null;

		Randomness randomness = Randomness.getInstance();
		int num = randomness.nextInt(variables.size());
		return variables.get(num).clone();
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getRandomObject(java.lang.reflect.Type)
	 */
	@Override
	public VariableReference getRandomObject(Type type)
	        throws ConstructionFailedException {
		return getRandomObject(type, statements.size());
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getRandomObject(java.lang.reflect.Type, int)
	 */
	@Override
	public VariableReference getRandomObject(Type type, int position)
	        throws ConstructionFailedException {
		assert (type != null);
		List<VariableReference> variables = getObjects(type, position);
		if (variables.isEmpty())
			throw new ConstructionFailedException();

		Randomness randomness = Randomness.getInstance();
		int num = randomness.nextInt(variables.size());
		return variables.get(num).clone();
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getObject(de.unisb.cs.st.evosuite.testcase.VariableReference, de.unisb.cs.st.evosuite.testcase.Scope)
	 */
	@Override
	public Object getObject(VariableReference reference, Scope scope) {
		return scope.get(reference);
	}

	/**
	 * Adjust position of variables by a given delta
	 * 
	 * @param position
	 *            Starting position
	 * @param delta
	 *            Value to add to positions
	 */
	private void fixVariableReferences(int position, int delta) {
		for (int i = position; i < statements.size(); i++) {
			statements.get(i).adjustVariableReferences(position, delta);
		}
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#renameVariable(int, int)
	 */
	@Override
	public void renameVariable(int old_position, int new_position) {
		for (int i = old_position; i < statements.size(); i++) {
			for (VariableReference var : statements.get(i)
			        .getVariableReferences()) {
				if (var.statement == old_position)
					var.statement = new_position;
			}
		}
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#setStatement(de.unisb.cs.st.evosuite.testcase.Statement, int)
	 */
	@Override
	public VariableReference setStatement(Statement statement, int position) {
		statements.set(position, statement);
		return new VariableReference(statement.getReturnType(), position); // TODO:
		                                                                   // -1?
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#addStatement(de.unisb.cs.st.evosuite.testcase.Statement, int)
	 */
	@Override
	public void addStatement(Statement statement, int position) {
		fixVariableReferences(position, 1);
		statements.add(position, statement);
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#addStatement(de.unisb.cs.st.evosuite.testcase.Statement)
	 */
	@Override
	public void addStatement(Statement statement) {
		statements.add(statement);
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getReturnValue(int)
	 */
	@Override
	public VariableReference getReturnValue(int position) {
		return statements.get(position).getReturnValue();
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#hasReferences(de.unisb.cs.st.evosuite.testcase.VariableReference)
	 */
	@Override
	public boolean hasReferences(VariableReference var) {
		if (var == null || var.statement == -1)
			return false;

		for (int i = var.statement; i < statements.size(); i++) {
			if (statements.get(i).references(var))
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getReferences(de.unisb.cs.st.evosuite.testcase.VariableReference)
	 */
	@Override
	public List<VariableReference> getReferences(VariableReference var) {
		List<VariableReference> references = new ArrayList<VariableReference>();

		if (var == null || var.statement == -1)
			return references;

		// references.add(var);

		for (int i = var.statement; i < statements.size(); i++) {
			List<VariableReference> temp = new ArrayList<VariableReference>();
			if (statements.get(i).references(var))
				temp.add(statements.get(i).getReturnValue());
			for (VariableReference v : references) {
				if (statements.get(i).references(v))
					temp.add(statements.get(i).getReturnValue());
			}
			references.addAll(temp);
		}

		return references;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#remove(int)
	 */
	@Override
	public void remove(int position) {
		logger.debug("Removing statement " + position);
		if (position >= size())
			return;
		fixVariableReferences(position, -1);
		statements.remove(position);
		// for(Statement s : statements) {
		// for(Asss.assertions)
		// }
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getStatement(int)
	 */
	@Override
	public Statement getStatement(int position) {
		return statements.get(position);
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getStatements()
	 */
	@Override
	public List<Statement> getStatements() {
		return statements;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#isPrefix(de.unisb.cs.st.evosuite.testcase.DefaultTestCase)
	 */
	@Override
	public boolean isPrefix(TestCase t) {
		if (statements.size() > t.size())
			return false;

		for (int i = 0; i < statements.size(); i++) {
			if (!statements.get(i).equals(t.getStatement(i))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Equality check
	 * 
	 * @param t
	 *            Other test case
	 * @return True if this test consists of the same statements as t
	 */
	// public boolean equals(TestCase t) {
	// return statements.size() == t.statements.size() && isPrefix(t);
	// }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
		        + ((statements == null) ? 0 : statements.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultTestCase other = (DefaultTestCase) obj;

		if (statements == null) {
			if (other.statements != null)
				return false;
		} else {
			if (statements.size() != other.statements.size())
				return false;
			// if (!statements.equals(other.statements))
			for (int i = 0; i < statements.size(); i++) {
				if (!statements.get(i).equals(other.statements.get(i))) {
					return false;
				}
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#hasObject(java.lang.reflect.Type, int)
	 */
	@Override
	public boolean hasObject(Type type, int position) {
		for (int i = 0; i < position; i++) {
			Statement st = statements.get(i);
			if (st.retval == null)
				continue; // Nop
			if (st.retval.isAssignableTo(type)) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#hasCastableObject(java.lang.reflect.Type)
	 */
	@Override
	public boolean hasCastableObject(Type type) {
		for (Statement st : statements) {
			if (st.retval.isAssignableFrom(type)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Create a copy of the test case
	 */
	@Override
	public DefaultTestCase clone() {
		DefaultTestCase t = new DefaultTestCase();
		for (Statement s : statements) {
			t.statements.add(s.clone());
		}
		t.coveredGoals.addAll(coveredGoals);
		//t.exception_statement = exception_statement;
		//t.exceptionThrown = exceptionThrown;
		return t;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getAccessedClasses()
	 */
	@Override
	public Set<Class<?>> getAccessedClasses() {
		Set<Class<?>> accessed_classes = new HashSet<Class<?>>();
		for (Statement s : statements) {
			for (VariableReference var : s.getVariableReferences()) {
				if (var != null && !var.isPrimitive()) {
					Class<?> clazz = var.getVariableClass();
					while (clazz.isMemberClass())
						clazz = clazz.getEnclosingClass();
					while (clazz.isArray())
						clazz = clazz.getComponentType();
					accessed_classes.add(clazz);
				}
			}
			if (s instanceof MethodStatement) {
				MethodStatement ms = (MethodStatement) s;
				accessed_classes.addAll(Arrays.asList(ms.getMethod()
				        .getExceptionTypes()));
				accessed_classes.add(ms.getMethod().getDeclaringClass());
				accessed_classes.add(ms.getMethod().getReturnType());
			} else if (s instanceof FieldStatement) {
				FieldStatement fs = (FieldStatement) s;
				accessed_classes.add(fs.getField().getDeclaringClass());
				accessed_classes.add(fs.getField().getType());
			} else if (s instanceof ConstructorStatement) {
				ConstructorStatement cs = (ConstructorStatement) s;
				accessed_classes.add(cs.getConstructor().getDeclaringClass());
			}
		}
		return accessed_classes;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#addAssertions(de.unisb.cs.st.evosuite.testcase.DefaultTestCase)
	 */
	@Override
	public void addAssertions(TestCase other) {
		for (int i = 0; i < statements.size() && i < other.size(); i++) {
			for (Assertion a : other.getStatement(i).assertions) {
				if (!statements.get(i).assertions.contains(a))
					if (a != null)
						statements.get(i).assertions.add(a.clone());
			}
		}
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#hasAssertions()
	 */
	@Override
	public boolean hasAssertions() {
		for (Statement s : statements) {
			if (s.hasAssertions())
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getAssertions()
	 */
	@Override
	public List<Assertion> getAssertions() {
		List<Assertion> assertions = new ArrayList<Assertion>();
		for (Statement s : statements) {
			assertions.addAll(s.assertions);
		}
		return assertions;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#removeAssertions()
	 */
	@Override
	public void removeAssertions() {
		for (Statement s : statements) {
			s.removeAssertions();
		}

	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#isValid()
	 */
	@Override
	public boolean isValid() {
		int num = 0;
		for (Statement s : statements) {
			if (s.retval.statement != num) {
				logger.error("Test case is invalid at statement " + num + " - "
				        + s.retval.statement);
				return false;
			}
			num++;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getDeclaredExceptions()
	 */
	@Override
	public Set<Class<?>> getDeclaredExceptions() {
		Set<Class<?>> exceptions = new HashSet<Class<?>>();
		for (Statement statement : statements) {
			exceptions.addAll(statement.getDeclaredExceptions());
		}
		return exceptions;
	}

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#hasCalls()
	 */
	@Override
	public boolean hasCalls() {
		for (Statement s : statements) {
			if (s instanceof TestCallStatement) {
				return true;
			}
		}
		return false;
        }

	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#addCoveredGoal(de.unisb.cs.st.evosuite.testcase.TestFitnessFunction)
	 */
	@Override
	public void addCoveredGoal(TestFitnessFunction goal) {
		coveredGoals.add(goal);
		// TODO: somehow adds the same goal more than once (fitnessfunction.equals()?)
	}
	
	/* (non-Javadoc)
	 * @see de.unisb.cs.st.evosuite.testcase.TestCase#getCoveredGoals()
	 */
	@Override
	public Set<TestFitnessFunction> getCoveredGoals() {
		return coveredGoals;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Statement> iterator() {
		return statements.iterator();
	}
}
