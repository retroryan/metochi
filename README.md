Metochi - Minimal Proof of Authority Blockchain with gRPC
=========================================================

This is designed to be a simple example of how to build a Proof of Authority (PoA) blockchain with gRPC.  It is not production worthy and has significant holes in the design.  It does a lot of anti-patterns like blocking calls to simplify the design.
Metochi uses a very naive Proof of Authority mechanism to establish which nodes are the authority nodes.  If a node in the config file has isAuthorityNode equal to true than it is considered an authority.

Future improvements would use a more sophisticated approach to establish authority and tie that to a nodes identity.

The consensus algorithm used is very simplisitic for this sample. A more complete example of a consensus algorithm would be to use the `Tendermint Byzantine Consensus Algorithm <http://tendermint.readthedocs.io/en/master/introduction.html#consensus-overview>`_

This example takes a very simplistic approach of running authority rounds approxiamtely every 10 seconds. Each authority node proposes to lead a round approxiamtely every 10 secs. with a small random offset to create the next block. The other nodes either accept or reject this proposal, depending on if they are already proposing a vote.


Running Metochi
===============

When metochi starts up it load a config file from the conf directory based on the name of that node.
The name is set in the environment variable NODE_NAME. Each node needs 2 or 3 peers to communicate with to gossip the blockchain info.
If a node is not a peer of another node, that is if is not listed in the peer list for another node in the cluster, then it will not recieve gossip messages.
This will cause that node to be out of sync with the other nodes, because it is only sending and not recieving gossip information.
Also there is a configurable pause at the startup of the node to allow it's peers to startup as well.  After the pause it sets up the channel information of the peers.

Export the node name environment variable.  There are 4 default node configurations available.
This makes it very simple to run a 4 node cluster by running 4 instances of metochi.
To do this open 4 terminal windows and in each window export a different node name.

It is important that all 4 nodes start within 10 seconds of each other so that they can synchronize.

The easiest way to do this is to first compile the application by running:

```
    mvn compile
```

First setup each node by setting the name of the node by running 4 terminal windows and in each one run:

```
    export NODE_NAME=one
```

Then run metochi in each of the terminal windows:

```
    mvn exec:java
```

Once metochi is running there is a lot of noise in the log files, but the command line prompt is still running.

You can export the current blockchain to a file by typing enter in the terminal window and then typing:

```
    /b
```
