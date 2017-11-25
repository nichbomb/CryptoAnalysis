package test;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.WeightedBoomerang;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import crypto.analysis.CrySLAnalysisResultsAggregator;
import crypto.rules.CryptSLRuleReader;
import crypto.rules.StateMachineGraph;
import crypto.typestate.CryptSLMethodToSootMethod;
import crypto.typestate.ExtendedIDEALAnaylsis;
import crypto.typestate.SootBasedStateMachineGraph;
import soot.Body;
import soot.Local;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import sync.pds.solver.nodes.Node;
import test.assertions.MustBeInState;
import test.core.selfrunning.AbstractTestingFramework;
import test.core.selfrunning.ImprecisionException;
import typestate.TransitionFunction;

public abstract class IDEALCrossingTestingFramework extends AbstractTestingFramework{
	protected BiDiInterproceduralCFG<Unit, SootMethod> icfg;
	protected long analysisTime;
//	private  IDebugger<TypestateDomainValue<StateNode>>  debugger;
//	protected TestingResultReporter<StateNode> testingResultReporter;
	public final static String RESOURCE_PATH = "src/test/resources/";
	
	protected abstract File getCryptSLFile();

	protected ExtendedIDEALAnaylsis createAnalysis() {
		return new ExtendedIDEALAnaylsis() {
			
			@Override
			protected BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
				return icfg;
			}
			
			@Override
			public SootBasedStateMachineGraph getStateMachine() {
				return new SootBasedStateMachineGraph(CryptSLRuleReader.readFromFile(new File(RESOURCE_PATH + getCryptSLFile())).getUsagePattern());
			}
			
			@Override
			public CrySLAnalysisResultsAggregator analysisListener() {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

//	protected IDebugger<TypestateDomainValue<StateNode>> getDebugger() {
//		if(debugger == null)
//			debugger =  new IDEVizDebugger<>(ideVizFile, icfg);
//		return debugger;
//	}

	@Override
	protected SceneTransformer createAnalysisTransformer() throws ImprecisionException {
		return new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				icfg = new JimpleBasedInterproceduralCFG(true);
				Set<Assertion> expectedResults = parseExpectedQueryResults(sootTestMethod);
				TestingResultReporter testingResultReporter = new TestingResultReporter(expectedResults);
				Map<Node<Statement, AllocVal>, WeightedBoomerang<TransitionFunction>> seedToSolvers = executeAnalysis();
				for(Node<Statement, AllocVal> seed : seedToSolvers.keySet()){
					for(Query q : seedToSolvers.get(seed).getSolvers().keySet()){
						if(q.asNode().equals(seed)){
							testingResultReporter.onSeedFinished(q.asNode(), seedToSolvers.get(seed).getSolvers().getOrCreate(q));
						}
					}
				}
				List<Assertion> unsound = Lists.newLinkedList();
				List<Assertion> imprecise = Lists.newLinkedList();
				for (Assertion r : expectedResults) {
					if (!r.isSatisfied()) {
						unsound.add(r);
					}
				}
				for (Assertion r : expectedResults) {
					if (r.isImprecise()) {
						imprecise.add(r);
					}
				}
				if (!unsound.isEmpty())
					throw new RuntimeException("Unsound results: " + unsound);
				if (!imprecise.isEmpty()) {
					throw new ImprecisionException("Imprecise results: " + imprecise);
				}
			}
		};
	}

	protected Map<Node<Statement, AllocVal>, WeightedBoomerang<TransitionFunction>> executeAnalysis() {
		CryptSLMethodToSootMethod.reset();
		ExtendedIDEALAnaylsis analysis = IDEALCrossingTestingFramework.this.createAnalysis();
		return analysis.run();
	}

	private Set<Assertion> parseExpectedQueryResults(SootMethod sootTestMethod) {
		Set<Assertion> results = new HashSet<>();
		parseExpectedQueryResults(sootTestMethod, results, new HashSet<SootMethod>());
		return results;
	}

	private void parseExpectedQueryResults(SootMethod m, Set<Assertion> queries, Set<SootMethod> visited) {
		if (!m.hasActiveBody() || visited.contains(m))
			return;
		visited.add(m);
		Body activeBody = m.getActiveBody();
		for (Unit callSite : icfg.getCallsFromWithin(m)) {
			for (SootMethod callee : icfg.getCalleesOfCallAt(callSite))
				parseExpectedQueryResults(callee, queries, visited);
		}
		for (Unit u : activeBody.getUnits()) {
			if (!(u instanceof Stmt))
				continue;

			Stmt stmt = (Stmt) u;
			if (!(stmt.containsInvokeExpr()))
				continue;
			InvokeExpr invokeExpr = stmt.getInvokeExpr();
			String invocationName = invokeExpr.getMethod().getName();
			if (!invocationName.startsWith("assertState"))
				continue;
			Value param = invokeExpr.getArg(0);
			if (!(param instanceof Local))
				continue;
			Local queryVar = (Local) param;
			Value param2 = invokeExpr.getArg(1);
			Val val = new Val(queryVar, m);
			queries.add(new MustBeInState(stmt, val, param2.toString()));
		}
	}

	/**
	 * This method can be used in test cases to create branching. It is not
	 * optimized away.
	 * 
	 * @return
	 */
	protected boolean staticallyUnknown() {
		return true;
	}

}
