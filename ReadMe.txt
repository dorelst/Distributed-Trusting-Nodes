The following functionality are supported by the Makefile:

1. Build
	- "make"
	- this compiles all .java files

2. Run the trust node aggregator
	- "make run_aggregator"
	- this runs the trust node aggregator responsible of combinning the different trust received from the trust nodes

3. Run differential trust node
	- "make run_difftrust"
	- this runs the differential trust node
	
4. Run ema trust node
	- "make run_ematrust"
	- this runs the ema trust node

5. Run optimistic trust node
	- "make run_optimtrust"
	- this runs the optimistic trust node. This a light implementation of the optimistic trust model from T-CDASH application used for testing purposes

6. Run pessimistic trust node
	- "make run_pessimtrust"
	- this runs the pessimistic trust node. This a light implementation of the pessimistic trust model from T-CDASH application used for testing purposes

7. Clean the class files
	- "make clean"
	- this deletes all the .class files

TrustingNodeAggregator currently is set to run three trust nodes and look for the trust node in the following locations (lines 243 - 252):
- for DifferentialTrustingNode on in-csci-rrpc02.cs.iupui.edu
- for EMATrustingNode on in-csci-rrpc03.cs.iupui.edu
- for PessimisticTrustingNode on in-csci-rrpc03.cs.iupui.edu

All three trust nodes needs to be up and running before aggregator is launched (or the ones that are needed can be comented).

The code for the OptimisticTrustingNode was included also and can be used for testings, by replacing any of the above trust nodes or by adding a fourth trust node to the system.

The server.policy file contains the security policies used for Java RMI Security Manager for all the nodes and the aggregator.

A SampleData.csv file was included for testing purposes. This file needs to be located on the machine where the TrustingNodeAggregator runs (in the same folder).