// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
//                                                                                                                                     
// This library is free software; you can redistribute it and/or                                                                       
// modify it under the terms of the GNU Lesser General Public License                                                                  
// as published by the Free Software Foundation; either version 2.1 of                                                                 
// the License, or (at your option) any later version.                                                                                 
//                                                                                                                                     
// This library is distributed in the hope that it will be useful, but                                                                 
// WITHOUT ANY WARRANTY; without even the implied warranty of                                                                          
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU                                                                    
// Lesser General Public License for more details.                                                                                     
//                                                                                                                                     
// You should have received a copy of the GNU Lesser General Public                                                                    
// License along with this program; if not, write to the Free Software                                                                 
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA                                                                           
// 02111-1307, USA.                                                                                                                    
// =================================================================                                                                   

package opendial.domains.rules.conditions;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.domains.datastructs.TemplateString;
import opendial.domains.rules.quantification.UnboundPredicate;


/**
 * Complex condition made up of a collection of sub-conditions connected with a
 * logical operator (AND, OR).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ComplexCondition implements Condition {

	// logger
	static Logger log = new Logger("ComplexCondition", Logger.Level.DEBUG);

	// the collection of subconditions
	Collection<Condition> subconditions;

	// the enumeration of possible binary operators
	public static enum BinaryOperator {AND, OR}

	// the binary operator for the complex condition (default is AND)
	BinaryOperator operator = BinaryOperator.AND;


	// ===================================
	//  CONDITION CONSTRUCTION
	// ===================================


	/**
	 * Creates a new empty complex condition
	 */
	public ComplexCondition() {
		subconditions = new LinkedList<Condition>();
	}
	
	/**
	 * Creates a new complex condition with a list of subconditions
	 * 
	 * @param subconditions the subconditions
	 */
	public ComplexCondition(Condition...subconditions) {
		this.subconditions = Arrays.asList(subconditions);
	}

	/**
	 * Creates a new complex condition with a list of subconditions
	 * 
	 * @param subconditions the subconditions
	 */
	public ComplexCondition(List<Condition> subconditions) {
		this.subconditions = subconditions;
	}

	/**
	 * Sets the logical operator for the complex condition
	 * 
	 * @param operator
	 */
	public void setOperator (BinaryOperator operator) {
		this.operator = operator;
	}

	
	/**
	 * Adds a new subcondition to the complex condition
	 * 
	 * @param condition the new subcondition
	 */
	public void addCondition(Condition condition) {
		subconditions.add(condition);
	}


	/**
	 * Removes the subcondition to the complex condition
	 * (if it exists)
	 * 
	 * @param condition the subcondition to remove
	 */
	public void removeCondition(Condition condition) {
		subconditions.remove(condition);
	}

	
	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Returns the logical operator for the complex condition
	 * 
	 * @return
	 */
	public BinaryOperator getOperator() {
		return operator;
	}

	/**
	 * Returns the set of input variables for the complex condition
	 * 
	 * @return the set of input variables
	 */
	@Override
	public Set<TemplateString> getInputVariables() {
		Set<TemplateString> variables = new HashSet<TemplateString>();
		for (Condition cond: subconditions) {
			variables.addAll(cond.getInputVariables());
		}
		return variables;
	}


	/**
	 * Returns the set of unbound predicates for the basic condition, which could
	 * be either associated with the variable label or its content.
	 * 
	 * @return the set of unbound predicates
	 */
	@Override
	public Set<UnboundPredicate> getUnboundPredicates() {
		Set<UnboundPredicate> predicates = new HashSet<UnboundPredicate>();
		for (Condition cond: subconditions) {
			predicates.addAll(cond.getUnboundPredicates());
		}	
		return predicates;
	}

	
	/**
	 * Returns true if the complex condition is satisfied by the input assignment,
	 * and false otherwise. 
	 * 
	 * <p>If the logical operator is AND, all the subconditions must be satisfied.  If
	 * the operator is OR, at least one must be satisfied.
	 * 
	 * @param input the input assignment
	 * @return true if the conditions are satisfied, false otherwise
	 */
	@Override
	public boolean isSatisfiedBy(Assignment input) {
		Assignment input2 = new Assignment(input);
		for (Condition cond : subconditions) {
			if (operator == BinaryOperator.AND && !cond.isSatisfiedBy(input2)) {
				return false;
			}
			else if (operator == BinaryOperator.OR && cond.isSatisfiedBy(input2)) {
				return true;
			}
			input2.addAssignment(cond.getLocalOutput(input2));
		}
		return (operator == BinaryOperator.AND);
	}

	/**
	 * Returns the (optional) local output produced by the fulfillment of the conditions
	 * on the input assignment -- for instance, slots filled by string matching.
	 *
	 * @param input the input assignment
	 * @return the local output assignment
	 */
	@Override
	public Assignment getLocalOutput(Assignment input) {
		Assignment output = new Assignment();
		for (Condition cond : subconditions) {
			if (cond.isSatisfiedBy(input)) {
				output.addAssignment(cond.getLocalOutput(input));
			}
		}
		return output;
	}



	// ===================================
	//  UTILITY FUNCTIONS
	// ===================================


	/**
	 * Returns a string representation of the complex condition
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = "";
		for (Condition cond: subconditions) {
			str += cond.toString() ;
			switch (operator) {
			case AND : str += " ^ "; break;
			case OR: str += " v "; break;
			}
		}
		return str.substring(0, str.length()-3);
	}


	/**
	 * Returns the hashcode for the condition
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return subconditions.hashCode() - operator.hashCode();
	}

	
	/**
	 * Returns true if the complex conditions are equal, false otherwise
	 *
	 * @param o the object to compare with current instance
	 * @return true if the conditions are equal, false otherwise
	 */
	public boolean equals(Object o) {
		return this.hashCode() == o.hashCode();
	}


}