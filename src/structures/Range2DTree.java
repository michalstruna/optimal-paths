package structures;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Range2DTree<TNode> implements IRange2DTree<TNode> {

    private Function<TNode, Point2D> positionAccessor;
    Node root;
    Stack<Node> lastLeafs; // Helper structure for building linked lists from leafs. There must be stack because primary tree and secondary trees are builded recursively, and leafs of different trees should not be linked.

    public Range2DTree(List<TNode> nodes, Function<TNode, Point2D> positionAccessor) {
        this.positionAccessor = positionAccessor;
        lastLeafs = new Stack<>();
        lastLeafs.push(null);
        root = build(nodes, true);
    }

    /**
     * Find node on position (not recursively, but using stack).
     * If root is null or root has searched position, return root, else add root to the stack.
     * Repeat this algorithm until stack is not empty or node is found:
     *     - Get node from stack,
     *     - Is left child has searched position, return left child and exit,
     *     - Same as previous, but for right child,
     *     - If searched position is located at the interval of left child node, add left child to stack,
     *     - Same as previous, but for right child.
     */
    @Override
    public TNode find(Point2D position) {
        if (root == null) {
            return null;
        }

        if (isLeaf(root) && position.equals(positionAccessor.apply(root.data))) {
            return root.data;
        }

        Stack<Node> toExplore = new Stack<>(); // Because find() is not recursive, there must be stack because of multiple nodes on same X position.
        toExplore.push(root);

        while (!toExplore.empty()) {
            Node current = toExplore.pop();

            if (isLeaf(current.left) && position.equals(positionAccessor.apply(current.left.data))) {
                return current.left.data;
            }

            if (isLeaf(current.right) && position.equals(positionAccessor.apply(current.right.data))) {
                return current.right.data;
            }

            if (isNotLeaf(current.left) && current.left.interval.getRelation(position.getX()) == RangeRelation.CONTAINS) {
                toExplore.push(current.left);
            }

            if (isNotLeaf(current.right) && current.right.interval.getRelation(position.getX()) == RangeRelation.CONTAINS) {
                toExplore.push(current.right);
            }
        }

        return null;
    }

    @Override
    public List<TNode> find(IRange<Point2D, Double> area) {
        return new ArrayList<>(find(area, root, true).values());
    }

    /**
     * Check if node is leaf.
     */
    private boolean isLeaf(Node node) {
        return node != null && node.data != null;
    }

    /**
     * Check if node is not leaf. This method is not !isLeaf(), because it test also if node != null.
     */
    private boolean isNotLeaf(Node node) {
        return node != null && node.data == null;
    }

    /**
     * Get most left leaf of subtree of "node".
     * @param node Root node of subtree.
     * @return Most left node.
     */
    private Node getMostLeftNode(Node node) {
        Node current = node;

        while (current.left != null) {
            current = current.left;
        }

        return current;
    }

    /**
     * Recursively find all nodes in area in subtree where "node" is root node.
     * IF node is leaf in area, add node to result:
     * IF node is not leaf:
     *     IF node´s interval overlaps area (in X or Y axis depends on "isX" argument), search left and right subtree,
     *     IF node´s interval is whole in area,
     *         IF current level is X, search secondary (Y) tree,
     *         IF current level is Y, get most left leaf of current subtree and loop all nodes from left to right using linked list.
     *         Add to result each node that is in area.
     * @param area Area where we want find all nodes.
     * @param node Current root node.
     * @param isX Is current level X (false for Y).
     * @return Map of points and nodes. There should not be list because of duplicates.
     */
    private Map<Point2D, TNode> find(IRange<Point2D, Double> area, Node node, boolean isX) {
        Map<Point2D, TNode> result = new HashMap<>();

        if (node != null) {
            if (isLeaf(node)) {
                if (area.getRelation(positionAccessor.apply(node.data)) == RangeRelation.CONTAINS) {
                    result.put(positionAccessor.apply(node.data), node.data);
                }
            } else {
                RangeRelation intervalRelation = area.getRelation(isX ? 0 : 1, node.interval.getFrom(), node.interval.getTo());

                if (intervalRelation == RangeRelation.OVERLAPS) {
                    result.putAll(find(area, node.left, isX));
                    result.putAll(find(area, node.right, isX));
                } else if (intervalRelation == RangeRelation.CONTAINS) {
                    if (isX) {
                        result.putAll(find(area, node.secondary, !isX));
                    } else {
                        if (intervalRelation == RangeRelation.CONTAINS) {
                            Node current = getMostLeftNode(node);

                            while (current != null) {
                                if (area.getRelation(positionAccessor.apply(current.data)) == RangeRelation.CONTAINS) {
                                    result.put(positionAccessor.apply(current.data), current.data);
                                }
                                current = current.next;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Recursively build tree from list of nodes.
     * IF list of nodes is empty:
     *     - There is no node, so return null. This should happen only if:
     *         - Root is null, so whole tree will be empty,
     *         - Right child of some node does not exist, so tree will be unbalanced by one level,
     * IF list of nodes contains 1 node (current node is leaf node):
     *     - There is no need to keep branching tree, so create leaf node,
     *     - Add this leaf to the list, so all leafs of tree will be linked.
     * IF list of nodes contains more nodes (current node is not leaf node, but range node):
     *     - Sort nodes and split them into two lists of the same size,
     *     - Create interval min-max where min and max are X or Y coordinates depends on "isX" argument,
     *     - Create new node with this interval where left child is range node contains first sublist and right child is range node contains second sublist,
     *     - Recursively build subtree of left and right child nodes. If current level is X, build also secondary tree with Y.
     * @param nodes List of nodes.
     * @param isX Current level is primary (X). False for secondary level (Y).
     * @return Root node of tree.
     */
    private Node build(List<TNode> nodes, boolean isX) {
        Function<TNode, Double> accessor = isX ? node -> positionAccessor.apply(node).getX() : node -> positionAccessor.apply(node).getY(); // Accessor of x or y coord depends on if level is primary or secondary.

        if (nodes.isEmpty()) {
            return null;
        } else if (nodes.size() == 1) { // If there is only 1 node, create leaf.
            Node node = new Node();
            node.data = nodes.get(0);
            Node lastLeaf = lastLeafs.pop();

            if (lastLeaf != null) {
                node.previous = lastLeaf;
                lastLeaf.next = node;
            }

            lastLeafs.push(node);

            return node;
        } else { // If there are more than 1 nodes, create interval node and build subtree.
            List<TNode> sorted = nodes.stream().sorted(Comparator.comparingDouble(accessor::apply)).collect(Collectors.toList());
            int medianIndex = (int) Math.floor(sorted.size() / 2);

            List<TNode> left = sorted.subList(0, medianIndex);
            List<TNode> right = sorted.subList(medianIndex, sorted.size());

            Node node = new Node();
            node.interval = new Interval(accessor.apply(sorted.get(0)), accessor.apply(sorted.get(sorted.size() - 1)));
            node.left = build(left, isX);
            node.right = build(right, isX);

            if (isX) { // If current level is primary (X) , build tree in secondary (Y) level.
                lastLeafs.push(null); // Leafs of primary and secondary tree will not be linked.
                node.secondary = build(nodes, false);
            }

            return node;
        }
    }

    private class Node {

        /** Left child of node. */
        Node left = null;

        /** Right child of node. */
        Node right = null;

        /** Interval of coordinates (min-max). */
        IRange<Double, Void> interval = null;

        /** Custom data of node. */
        TNode data = null;

        /** Next sibling node. */
        Node next = null;

        /** Previous sibling node. */
        Node previous = null;

        /** Root node of secondary tree (y coordinate). */
        Node secondary = null;
    }

}