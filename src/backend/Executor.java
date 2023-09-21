/**
 * Executor class for a simple interpreter.
 *
 * (c) 2020 by Ronald Mak
 * Department of Computer Science
 * San Jose State University
 */
package backend;

import java.util.ArrayList;
import java.util.HashSet;

import intermediate.*;
import static intermediate.Node.NodeType.*;

public class Executor
{
    private int lineNumber;

    private static HashSet<Node.NodeType> singletons;
    private static HashSet<Node.NodeType> relationals;

    static
    {
        singletons  = new HashSet<Node.NodeType>();
        relationals = new HashSet<Node.NodeType>();

        singletons.add(VARIABLE);
        singletons.add(INTEGER_CONSTANT);
        singletons.add(REAL_CONSTANT);
        singletons.add(STRING_CONSTANT);
        singletons.add(NEGATE);
        singletons.add(NOT);

        relationals.add(EQ);
        relationals.add(NE);
        relationals.add(LT);
        relationals.add(LTE);
        relationals.add(GT);
        relationals.add(GTE);
        relationals.add(AND);
        relationals.add(OR);
    }

    public Executor() {}

    public Object visit(Node node)
    {
        switch (node.type)
        {
            case PROGRAM :  return visitProgram(node);

            case COMPOUND :
            case ASSIGN :
            case LOOP :
            case IF:
            case WRITE :
            case WRITELN :  return visitStatement(node);

            case TEST:      return visitTest(node);

            default :       return visitExpression(node);
        }
    }

    private Object visitProgram(Node programNode)
    {
        Node compoundNode = programNode.children.get(0);
        return visit(compoundNode);
    }

    private Object visitStatement(Node statementNode)
    {
        lineNumber = statementNode.lineNumber;

        switch (statementNode.type)
        {
            case COMPOUND :  return visitCompound(statementNode);
            case ASSIGN :    return visitAssign(statementNode);
            case LOOP :      return visitLoop(statementNode);
            case IF:         return visitIf(statementNode);
            case WRITE :     return visitWrite(statementNode);
            case WRITELN :   return visitWriteln(statementNode);

            default :        return null;
        }
    }
    private Object visitIf(Node ifNode){
        Node exprNode = ifNode.children.get(0); //if statement
        Node thenNode = ifNode.children.get(1); //then statement
        Node elseNode;
        if(ifNode.children.size() == 3) elseNode = ifNode.children.get(2); //assign else if it exists
        else elseNode = null;

        if((Boolean)visit(exprNode)) return visit(thenNode); //explore the nodes for then
        else if (elseNode != null) return visit(elseNode); //explore else if exists

        return null;
    }
    private Object visitCompound(Node compoundNode)
    {
        for (Node statementNode : compoundNode.children) visit(statementNode);

        return null;
    }

    private Object visitAssign(Node assignNode)
    {
        Node lhs = assignNode.children.get(0);
        Node rhs = assignNode.children.get(1);

        // Evaluate the right-hand-side expression;
        Double value = (Double) visit(rhs);

        // Store the value into the variable's symbol table entry.
        SymtabEntry variableId = lhs.entry;
        variableId.setValue(value);

        return null;
    }

    private Object visitLoop(Node loopNode)
    {
        boolean b = false;
        do
        {
            for (Node node : loopNode.children)
            {
                Object value = visit(node);  // statement or test
                // Evaluate the test condition. Stop looping if true.
                b = (node.type == TEST) && ((boolean) value);
                if (b) break;
            }
        } while (!b);

        return null;
    }

    private Object visitTest(Node testNode)
    {
        return (Boolean) visit(testNode.children.get(0));
    }

    private Object visitWrite(Node writeNode)
    {
        printValue(writeNode.children);
        return null;
    }

    private Object visitWriteln(Node writelnNode)
    {
        if (writelnNode.children.size() > 0) printValue(writelnNode.children);
        System.out.println();

        return null;
    }

    private void printValue(ArrayList<Node> children)
    {
        long fieldWidth    = -1;
        long decimalPlaces = 0;

        // Use any specified field width and count of decimal places.
        if (children.size() > 1)
        {
            double fw = (Double) visit(children.get(1));
            fieldWidth = (long) fw;

            if (children.size() > 2)
            {
                double dp = (Double) visit(children.get(2));
                decimalPlaces = (long) dp;
            }
        }

        // Print the value with a format.
        Node valueNode = children.get(0);
        if (valueNode.type == VARIABLE)
        {
            String format = "%";
            if (fieldWidth >= 0)    format += fieldWidth;
            if (decimalPlaces >= 0) format += "." + decimalPlaces;
            format += "f";

            Double value = (Double) visit(valueNode);
            System.out.printf(format, value);
        }
        else  // node type STRING_CONSTANT
        {
            String format = "%";
            if (fieldWidth > 0) format += fieldWidth;
            format += "s";

            String value = (String) visit(valueNode);
            System.out.printf(format, value);
        }
    }

    private Object visitExpression(Node expressionNode)
    {
        // Single-operand expressions.
        if (singletons.contains(expressionNode.type))
        {
            switch (expressionNode.type)
            {
                case VARIABLE         : return visitVariable(expressionNode);
                case INTEGER_CONSTANT : return visitIntegerConstant(expressionNode);
                case REAL_CONSTANT    : return visitRealConstant(expressionNode);
                case STRING_CONSTANT  : return visitStringConstant(expressionNode);
                case NOT:
                    Boolean b = (Boolean) visit(expressionNode.children.get(0));
                    return !b;
                case NEGATE:
                    return - ((double) visit(expressionNode.children.get(0)));

                default: return null;
            }
        }
        // Binary expressions.

        Object value1 = visit(expressionNode.children.get(0));
        Object value2 = visit(expressionNode.children.get(1));
        // Relational expressions.
        //do the comparison of two factors if we have an expressionNode
        if(relationals.contains(expressionNode.type)){
            boolean value = false;

            switch (expressionNode.type){
                case LT: value = ((double) value1) < ((double) value2); break;
                case LTE: value = ((double) value1) <= ((double) value2); break;
                case GT: value = ((double) value1) > ((double) value2); break;
                case GTE: value = ((double) value1) >= ((double) value2); break;
                case AND: value = ((boolean) value1) && ((boolean) value2); break;
                case OR: value = ((boolean) value1) || ((boolean) value2); break;
                case EQ: value = ((double) value1) == ((double) value2); break;
                case NE: value = ((double) value1) != ((double) value2); break;
                default: break;
            }
            return value;
        }
        double value = 0.0;

        // Arithmetic expressions.
        switch (expressionNode.type)
        {
            case ADD :      value = (double) value1 + (double) value2; break;
            case SUBTRACT : value = (double) value1 - (double) value2; break;
            case MULTIPLY : value = (double) value1 * (double) value2; break;

            case DIVIDE :
            {
                if ((double) value2 != 0.0) value = (double) value1/ (double) value2;
                else
                {
                    runtimeError(expressionNode, "Division by zero");
                    return 0.0;
                }

                break;
            }

            default : break;
        }

        return Double.valueOf(value);
    }

    private Object visitVariable(Node variableNode)
    {
        // Obtain the variable's value from its symbol table entry.
        SymtabEntry variableId = variableNode.entry;
        if(variableNode.entry == null) variableNode.value = 1;
        Double value = variableId.getValue();
        if(value != null) return value;
        return null;
    }

    private Object visitIntegerConstant(Node integerConstantNode)
    {
        long value = (Long) integerConstantNode.value;
        return (double) value;
    }

    private Object visitRealConstant(Node realConstantNode)
    {
        return (Double) realConstantNode.value;
    }

    private Object visitStringConstant(Node stringConstantNode)
    {
        return (String) stringConstantNode.value;
    }

    private void runtimeError(Node node, String message)
    {
        System.out.printf("RUNTIME ERROR at line %d: %s: %s\n",
                          lineNumber, message, node.text);
        System.exit(-2);
    }
}
