package de.bluekiwi.labs.vis;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JFrame;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

public class SimpleGraphViewWithJung {

    private HashSet<Color> colorsAvaible = new HashSet<Color>();

    public SimpleGraphViewWithJung() {
        colorsAvaible.add(Color.BLUE);
        colorsAvaible.add(Color.CYAN);
        colorsAvaible.add(Color.GREEN);
        colorsAvaible.add(Color.MAGENTA);
        colorsAvaible.add(Color.ORANGE);
        colorsAvaible.add(Color.PINK);
        colorsAvaible.add(Color.RED);
        colorsAvaible.add(Color.YELLOW);
        colorsAvaible.add(Color.BLUE.darker());
        colorsAvaible.add(Color.CYAN.darker());
        colorsAvaible.add(Color.GREEN.darker());
        colorsAvaible.add(Color.MAGENTA.darker());
        colorsAvaible.add(Color.ORANGE.darker());
        colorsAvaible.add(Color.PINK.darker());
        colorsAvaible.add(Color.RED.darker());
        colorsAvaible.add(Color.YELLOW.darker());
        colorsAvaible.add(Color.BLUE.brighter());
        colorsAvaible.add(Color.CYAN.brighter());
        colorsAvaible.add(Color.GREEN.brighter());
        colorsAvaible.add(Color.MAGENTA.brighter());
        colorsAvaible.add(Color.ORANGE.brighter());
        colorsAvaible.add(Color.PINK.brighter());
        colorsAvaible.add(Color.RED.brighter());
        colorsAvaible.add(Color.YELLOW.brighter());
    }

    public KKLayout<MyNode, Integer> layout;
    private JFrame frame;
    private BasicVisualizationServer<MyNode, Integer> vv;

    private HashMap<Integer, Color> colorGroups = new HashMap<Integer, Color>();

    public BasicVisualizationServer<MyNode, Integer> showGraph(DirectedGraph<MyNode, Integer> g)
            throws InterruptedException {
        layout = new KKLayout<MyNode, Integer>(g);
        layout.setSize(new Dimension(800, 800));
        layout.initialize();
        layout.setMaxIterations(10000);
        vv = new BasicVisualizationServer<MyNode, Integer>(layout);
        vv.setPreferredSize(new Dimension(800, 800));
        // Setup up a new vertex to paint transformer...
        Transformer<MyNode, Paint> vertexPaint = new Transformer<MyNode, Paint>() {
            @Override
            public Paint transform(MyNode i) {
                if (i.getLevel() == 0)
                {
                    // TODO fix the coloring problem
                    return Color.RED;
                    // if (colorGroups.containsKey(i.getLabelID()))
                    // {
                    // return colorGroups.get(i.getLabelID());
                    // }
                    // else {
                    // Color c = colorsAvaible.iterator().next();
                    // colorsAvaible.remove(c);
                    // colorGroups.put(i.getLabelID(), c);
                    // return c;
                    // }
                }
                else {
                    return Color.WHITE;
                }
            }

        };
        // set size of nodes
        Transformer<MyNode, Shape> vertexSize = new Transformer<MyNode, Shape>() {
            @Override
            public Shape transform(MyNode i) {
                Ellipse2D circle = new Ellipse2D.Double(-5, -5, 10, 10);
                return circle;
            }
        };
        // Set up a new stroke Transformer for the edges
        // float dash[] = { 10.0f };
        // final Stroke edgeStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash,
        // 10.0f);
        // Transformer<Integer, Stroke> edgeStrokeTransformer =
        // new Transformer<Integer, Stroke>() {
        // public Stroke transform(Integer s) {
        // return edgeStroke;
        // }
        // };
        vv.getRenderContext().setVertexShapeTransformer(vertexSize);
        vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
        // vv.getRenderContext().setEdgeStrokeTransformer(edgeStrokeTransformer);
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<MyNode>());
        vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<Integer>());
        vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
        frame = new JFrame("Simple Graph View");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(vv);
        frame.pack();
        frame.setVisible(true);
        return vv;
    }

    public void close() {
        frame.dispose();
        vv.setVisible(false);
    }
}
