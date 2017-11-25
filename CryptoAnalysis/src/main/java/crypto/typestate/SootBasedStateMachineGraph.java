package crypto.typestate;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import crypto.rules.CryptSLMethod;
import crypto.rules.StateMachineGraph;
import crypto.rules.StateNode;
import crypto.rules.TransitionEdge;
import soot.SootMethod;
import typestate.TransitionFunction;
import typestate.finiteautomata.ITransition;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;

public class SootBasedStateMachineGraph {

	private Set<MatcherTransition> transition = new HashSet<>();
	private final WrappedState initialState;
	private Collection<SootMethod> edgeLabelMethods = Sets.newHashSet();
	
	private final StateMachineGraph stateMachineGraph;
	private Multimap<State, SootMethod> outTransitions = HashMultimap.create();
	private boolean seedIsConstructor;
	private Collection<SootMethod> initialTransitonLabel;
	private List<CryptSLMethod> crySLinitialTransitionLabel;
	private LabeledMatcherTransition initialTransiton;

	public SootBasedStateMachineGraph(StateMachineGraph fsm) {
		this.stateMachineGraph = fsm;
		//TODO #15 we must start the analysis in state stateMachineGraph.getInitialTransition().from();
		initialState = new WrappedState(stateMachineGraph.getInitialTransition().to());
		for (final TransitionEdge t : stateMachineGraph.getAllTransitions()) {
			WrappedState from = new WrappedState(t.from());
			WrappedState to = new WrappedState(t.to());
			LabeledMatcherTransition trans = new LabeledMatcherTransition(from, t.getLabel(),
					Parameter.This, to, Type.OnCallToReturn);
			this.addTransition(trans);
			outTransitions.putAll(from, convert(t.getLabel()));
			if(stateMachineGraph.getInitialTransition().equals(t))
				this.initialTransiton = trans;
		}
		crySLinitialTransitionLabel = stateMachineGraph.getInitialTransition().getLabel();
		
		initialTransitonLabel = convert(stateMachineGraph.getInitialTransition().getLabel());
		List<SootMethod> label = Lists.newLinkedList();
		for(SootMethod m : initialTransitonLabel){
			if(m.isConstructor()){
				label.add(m);
			}
		}
		if(!label.isEmpty()){
			this.addTransition(new MatcherTransition(initialState, label, Parameter.This, initialState, Type.OnCallToReturn));
			this.outTransitions.putAll(initialState, label);
			seedIsConstructor = true;
		}
		//All transitions that are not in the state machine 
		for(StateNode t :  this.stateMachineGraph.getNodes()){
			State wrapped = new WrappedState(t);
			Collection<SootMethod> remaining = getInvolvedMethods();
			Collection<SootMethod> outs =  this.outTransitions.get(wrapped);
			if(outs == null)
				outs = Sets.newHashSet();
			remaining.removeAll(outs);
			this.addTransition(new MatcherTransition(wrapped, remaining, Parameter.This, ErrorStateNode.v(), Type.OnCallToReturn));
		}
	}


	public Collection<SootMethod> getEdgesOutOf(State n){
		return outTransitions.get(n);
	}
	public void addTransition(MatcherTransition trans) {
		transition.add(trans);
	}

	private Collection<SootMethod> convert(List<CryptSLMethod> label) {
		Collection<SootMethod> converted = CryptSLMethodToSootMethod.v().convert(label);
		edgeLabelMethods.addAll(converted);
		return converted;
	}

	public boolean seedIsConstructor(){
		return seedIsConstructor;
	}

	public Collection<SootMethod> getInvolvedMethods(){
		return Sets.newHashSet(edgeLabelMethods);
	}
	
	
	private class WrappedState implements State{
		private final StateNode delegate;
		private final boolean initialState;

		WrappedState(StateNode delegate){
			this.delegate = delegate;
			this.initialState = stateMachineGraph.getInitialTransition().from().equals(delegate);
		}
		@Override
		public boolean isErrorState() {
			return delegate.isErrorState();
		}

		@Override
		public boolean isAccepting() {
			return delegate.getAccepting();
		}
		@Override
		public boolean isInitialState() {
			return initialState;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
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
			WrappedState other = (WrappedState) obj;
			if (delegate == null) {
				if (other.delegate != null)
					return false;
			} else if (!delegate.equals(other.delegate))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return delegate.getName().toString();
		}
	}

	public TransitionFunction getInitialWeight() {
		return new TransitionFunction(initialTransiton);
	}

	public List<MatcherTransition> getAllTransitions() {
		return Lists.newArrayList(transition);
	}

	public Collection<SootMethod> initialTransitonLabel() {
		return Lists.newArrayList(initialTransitonLabel);
	}


	public List<CryptSLMethod> getInitialTransition() {
		return crySLinitialTransitionLabel;
	}
}
