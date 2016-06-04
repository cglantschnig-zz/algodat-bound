package ad2.ss16.pa;

import java.util.Arrays;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Map;

public class KMST extends AbstractKMST {
    private final Edge[] edges;
    private final Edge[] result;
    private boolean[] marked;

    public KMST(Integer numNodes, Integer numEdges, HashSet<Edge> edges, int k) {
        this.edges = edges.toArray(new Edge[0]);
        this.result = new Edge[k - 1];
        this.marked = new boolean[numNodes];
    }

    /**
     * run the k-MST algorithm
     * 1. Sort the edges by weight
     * 2. calculate the lowerBound, by taking the k edges which the lowest weight
     * 3. use the prim algorithm starting from each node once and add the results to a list for later calculations. (this way we get a good upper bound)
     * 4. compare all the results and pick the best one with branch and bound
     */
    @Override public void run() {
        Map<Integer, Integer> roots = new TreeMap<Integer, Integer>();

        Edge[] edges = new Edge[this.edges.length];
        int n, weight, relevantEdges, root, lowerBound = 0;

        // sort edges by weight with quicksort
        Arrays.sort(this.edges);

        // calculate the initial lower bound
        for (int i = 0; i < result.length; i++) {
            lowerBound += this.edges[i].weight;
        }

        // iterate over all nodes and pick them as a new starting point, from where we take
        // k - 1 edges as fixed (in each
        for (root = 0; root < marked.length; root++) {
            marked = new boolean[marked.length];
            System.arraycopy(this.edges, 0, edges, 0, this.edges.length);

            marked[root] = true;
            n = 0;
            weight = 0;
            relevantEdges = this.edges.length;

            while (n < result.length) {
                for (int i = 0; i < relevantEdges; i++) {
                    // check if the edges are connected and have no circle
                    if (marked[edges[i].node1] ^ marked[edges[i].node2]) {
                        // mark the node, which the new node is connected to our current tree
                        marked[marked[edges[i].node1] ? edges[i].node2 : edges[i].node1] = true;
                        result[n++] = edges[i];
                        weight += edges[i].weight;
                        // remove the choosen edge from the local copy, so that we dont need to check for it again
                        System.arraycopy(edges, i + 1, edges, i, --relevantEdges - i);
                        break;
                    }
                    // check if there is a circle
                    else if (marked[edges[i].node1]) {
                        // remove the edge, which would be creating a circle for the future
                        System.arraycopy(edges, i + 1, edges, i, --relevantEdges - i);
                        break;
                    }
                }
            }
            // take the found tree with k - 1 edges and set is as a possible result
            HashSet<Edge> set = new HashSet<Edge>(result.length);
            for (int i = 0; i < result.length; i++) {
                set.add(result[i]);
            }
            setSolution(weight, set);
            // save the spanning tree for later so we can evaluate if it is worth it. the weight is our key
            roots.put(weight, root);
        }
        // lets go through all the collected results and compute them
        for (int item : roots.values()) {
            marked = new boolean[marked.length];
            System.arraycopy(this.edges, 0, edges, 0, this.edges.length);
            marked[item] = true; // start to compute our solutions from the root of our best solutions so far
            computeSolution(edges, result.length, 0, lowerBound);
        }
    }


    /**
     * calculate some solutions, based on our so far best solution starting nodes.
     * @param edges list of edges, which are available to check
     * @param left number of edges we need for a valid result
     * @param weight current weight
     * @param lowerBound current lower bound
     */
    private void computeSolution(Edge[] edges, int left, int weight, int lowerBound) {
        Edge edge = null;
        int tmp = -1, relevantEdges = edges.length;
        // go through all edges
        for (int i = 0; i < relevantEdges; i++) {
            edge = edges[i];
            if (left == 0) { // finished, we have enough edges together for a valid solution
                HashSet<Edge> set = new HashSet<Edge>(result.length);
                for (int j = 0; j < result.length; j++) {
                    set.add(result[j]);
                }
                setSolution(weight, set);
                return;
            }
            else if (weight + edge.weight > getSolution().getUpperBound()) {
                // result is too expensive, kill this branch
                return;
            }
            // edge is connected to our spanning tree
            else if (marked[edge.node1] ^ marked[edge.node2]) {
                result[result.length - left] = edge;
                marked[(tmp = marked[edge.node1] ? edge.node2 : edge.node1)] = true;

                // remove edge for deeper recursions
                System.arraycopy(edges, i + 1, edges, i, relevantEdges-- - i-- - 1);
                Edge[] copy = new Edge[relevantEdges];
                System.arraycopy(edges, 0, copy, 0, relevantEdges);

                // recursive call ( we set the current path [edge] to true )
                computeSolution(copy, left - 1, weight + edge.weight, lowerBound);

                // set marked to false for a possible false solution
                marked[tmp] = false;

                if (i + left > relevantEdges) {
                    // not enough edges left
                    return;
                }
                else if ((lowerBound += (i < left - 1 ? edges[left - 1].weight : edges[i + 1].weight) - edge.weight) >= getSolution().getUpperBound()) {
                    // this branch will never reach a better result than we've already found
                    return;
                }
            }
            // we detected a circle
            else if (marked[edge.node1]) {
                System.arraycopy(edges, i + 1, edges, i, relevantEdges-- - i-- - 1);

                if (i + left > relevantEdges) {
                    // not enough edges left
                    return;
                }
                else if ((lowerBound += (i < left - 1 ? edges[left - 1].weight : edges[i + 1].weight) - edge.weight) >= getSolution().getUpperBound()) {
                    // this branch will never reach a better result than we've already found
                    return;
                }
            }
            else {
                if (i > left && i + 1 < relevantEdges) {
                    lowerBound -= edge.weight - edges[i + 1].weight;
                }
                if (lowerBound >= getSolution().getUpperBound()) {
                    return;
                }
            }
        }
    }
}