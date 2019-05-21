package compiler.phases.ralloc;

import compiler.Main;
import compiler.data.asmcode.AsmInstr;
import compiler.data.asmcode.Code;
import compiler.data.layout.Temp;
import compiler.phases.livean.LiveAn;

import java.util.*;

class Node {
    Temp temp;
    ArrayList<Node> connections;
    int color;
    boolean inColorStack = false;
    boolean isPotentialSpill = false;

    Node(Temp t){
        temp = t;
        color = -1;
        connections = new ArrayList<>();
    }

    void addConnection(Node n){
        boolean isIn = false;
        for (Node c: connections){
            if (c == n){
                isIn = true;
            }
        }
        if (!isIn){
            connections.add(n);
        }
    }

    int numOfConnections(){
        int n = 0;
        for (Node node : connections){
            if (!node.inColorStack){
                n++;
            }
        }
        return n;
    }

    HashSet<Integer> getNeighbourColors(){
        HashSet<Integer> nc = new HashSet<>();
        for (Node node: connections){
            nc.add(node.color);
        }
        return nc;
    }
}

public class Graph {

    Code code;
    ArrayList<Node> nodes;
    ArrayList<Node> spilledNodes;
    HashMap<Temp, Node> nodeMap;
    Stack<Node> colorNodeStack;
    ArrayList<Integer> colors;
    long tempSize = 0;


    public Graph(Code code){
        this.code = code;
        nodeMap = new HashMap<>();
        nodes = new ArrayList<>();
        colorNodeStack = new Stack<>();
        colors = new ArrayList<>();
        for (int i=0; i<8; i++){
            colors.add(i);
        }
        create_graph(code.instrs);
    }

    void create_graph(Vector<AsmInstr> instrs){
        HashSet<Temp> temps = new HashSet<>();
        for (AsmInstr instr : instrs){
            temps.addAll(instr.defs());
            temps.addAll(instr.uses());
        }
        for (Temp t : temps){
            Node n = new Node(t);
            nodes.add(n);
            nodeMap.put(t, n);
        }
        for (AsmInstr instr : instrs){
            for (Temp t1: instr.in()){
                for (Temp t2: instr.in()){
                    if (t1 != t2){
                        Node n1 = nodeMap.get(t1);
                        Node n2 = nodeMap.get(t2);
                        n1.addConnection(n2);
                    }
                }
            }
            for (Temp t1: instr.out()){
                for (Temp t2: instr.out()){
                    if (t1 != t2){
                        Node n1 = nodeMap.get(t1);
                        Node n2 = nodeMap.get(t2);
                        n1.addConnection(n2);
                    }
                }
            }
        }
    }

    public Code getModifiedCode(){
        simplify();
        mapColors();
        return code;
    }

    void simplify(){
        boolean haveRemovedMore = true;
        while (haveRemovedMore) {
            haveRemovedMore = false;
            for (int i = 0; i < nodes.size(); i++) {
                Node n = nodes.get(i);
                if (!n.inColorStack && n.numOfConnections() < 8) {
                    n.inColorStack = true;
                    colorNodeStack.push(n);
                    haveRemovedMore = true;
                }
            }
        }
        if(nodes_left()==0){
            colorGraph();
        } else {
            selectForSpill();
        }
    }

    int nodes_left(){
        int n = 0;
        for (Node node: nodes){
            if (!node.inColorStack){
                n++;
            }
        }
        return n;
    }

    void colorGraph(){
        boolean canColorAll = true;
        for (Node node: colorNodeStack){
            HashSet<Integer> colorsLeft = new HashSet<Integer>(colors);
            HashSet<Integer> neighboursColors = node.getNeighbourColors();
            colorsLeft.removeAll(neighboursColors);
            if (colorsLeft.size() > 0){
                int min = 9;
                for (Integer i: colorsLeft){
                    if (i < min){
                        min = i;
                    }
                }
                node.color = min;
            } else {
                spilledNodes.add(node);
                canColorAll = false;
            }
        }
        if (!canColorAll){
            //spillNodes();
        }
    }

    void selectForSpill(){
        for (Node node: nodes){
            if (!node.inColorStack){
                node.isPotentialSpill = true;
                node.inColorStack = true;
                colorNodeStack.push(node);
                simplify();
            }
        }
    }

    void spillNodes(){
        Vector<AsmInstr> newInstructions = new Vector<>();
        for (AsmInstr instr: code.instrs){
            boolean tempIsIn = false;
            for (Node n : spilledNodes){
                // do something
            }
            if (!tempIsIn){
                newInstructions.add(instr);
            }
        }
        create_graph(newInstructions);
        simplify();
    }

    void mapColors(){
        HashMap<Temp, Integer> tempToRegMap = new HashMap<>();
        for (Node n: nodes){
            tempToRegMap.put(n.temp, n.color);
        }
        code = new Code(code.frame, code.entryLabel, code.exitLabel, code.instrs, tempToRegMap, tempSize);
    }

}