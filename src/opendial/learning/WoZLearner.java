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

package opendial.learning;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.empirical.SimpleEmpiricalDistribution;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.nodes.UtilityNode;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.domains.rules.DecisionRule;
import opendial.inference.queries.UtilQuery;
import opendial.inference.sampling.WoZQuerySampling;
import opendial.planning.ForwardPlanner;
import opendial.state.DialogueState;

public class WoZLearner extends ForwardPlanner  {

	// logger
	public static Logger log = new Logger("WoZLearner", Logger.Level.DEBUG);

	String actionVariable = "a_m";

	public WoZLearner(DialogueState state) {
		super(state);
	}

	@Override
	public void run() {
		
		if (currentState.getNetwork().hasActionNode(actionVariable+"'")) {
			Value gold = getGoldAction(currentState);

			List<String> paramVars = currentState.getParameterIds();
			for (String paramVar : new ArrayList<String>(paramVars)) {
				ChanceNode paramNode = currentState.getNetwork().getChanceNode(paramVar);
				if (paramNode.getOutputNodesIds().isEmpty()) { 
		//				!(paramNode.getOutputNodes().iterator().next() instanceof UtilityNode)) {
					paramVars.remove(paramVar);
				}
			}
			
			UtilQuery query = new UtilQuery(currentState, paramVars);
			WoZQuerySampling wozquery = new WoZQuerySampling(query, new Assignment(actionVariable+"'", gold),
					Settings.getInstance().nbSamples, Settings.getInstance().maximumSamplingTime);

			Thread t = new Thread(wozquery);

			// waits for the results to be compiled
			synchronized (wozquery) {
				t.start();
				while (wozquery.getResults() == null) {
					try { wozquery.wait();  }
					catch (InterruptedException e) {}
				}
			}

			Assignment goldAction = new Assignment(actionVariable, gold);
			recordAction(currentState, goldAction);
			
			SimpleEmpiricalDistribution paramDistrib = wozquery.getResults();
			log.debug("updating parameters: " + paramDistrib.getHeadVariables());
			try {
				currentState.addContent(paramDistrib, "wozlearner");
	
			} catch (DialException e) {
				log.warning("could not update state parameters: " + e);
			}
		}
		else {
			super.run();
		}

	}
	



	private Value getGoldAction(DialogueState state) {
		if (state.getNetwork().hasChanceNode(actionVariable+"-gold")) {
			try {
				SimpleTable table = (SimpleTable) state.getContent(actionVariable+"-gold", true).toDiscrete();
				return table.getNBest(1).getRows().iterator().next().getValue(actionVariable+"-gold");
			}
			catch (DialException e) {
				log.warning("could not extract gold value: "+ e);
				return ValueFactory.none();
			}
		}
		else {
		//	log.warning("no gold value specified");
			return ValueFactory.none();
		}
	}


}

