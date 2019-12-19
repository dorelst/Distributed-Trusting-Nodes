AGGREGATOR = TrustingNodesAggregator
DIFFERENTIALTRUST = DifferentialTrustingNode
EMATRUST = EMATrustingNode
OPTIMISTICTRUST = OptimisticTrustingNode
PESSIMISTICTRUST = PessimisticTrustingNode
JFLAGS = -g
JC = javac
JVM= java 
FILE=
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
        TrustingNodesAggregator.java \
        DifferentialTrustingNode.java \
        EMATrustingNode.java \
        OptimisticTrustingNode.java \
        PessimisticTrustingNode.java \
        TrustingNodes.java \
        AllTrustingNodes.java

default: classes

classes: $(CLASSES:.java=.class)

run_aggregator:
	java  $(AGGREGATOR)

run_difftrust:
	java $(DIFFERENTIALTRUST)

run_ematrust:
	java $(EMATRUST)

run_optimtrust:
	java $(OPTIMISTICTRUST)

run_pessimtrust:
	java $(PESSIMISTICTRUST)

clean:
	$(RM) *.class
