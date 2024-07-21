package org.texttechnologylab.dependency.graph;

import com.google.common.graph.ImmutableGraph;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyGraphStringifier {

    final static Character[] characterLookup = new Character[]{'a', 'A', 'b', 'B', 'c', 'C', 'd', 'D', 'e', 'E', 'f', 'F', 'g', 'G', 'h', 'H', 'i', 'I', 'j', 'J', 'k', 'K', 'l', 'L', 'm', 'M', 'n', 'N', 'o', 'O', 'p', 'P', 'q', 'Q', 'r', 'R', 's', 'S', 'ß', 't', 'T', 'u', 'U', 'v', 'V', 'w', 'W', 'x', 'X', 'y', 'Y', 'z', 'Z', 'α', 'β', 'γ', 'δ', 'ε', 'ζ', 'η', 'θ', 'ι', 'κ', 'λ', 'μ', 'ν', 'ξ', 'ο', 'π', 'ρ', 'σ', 'ς', 'τ', 'υ', 'φ', 'χ', 'ψ', 'ω', 'а', 'А', 'б', 'Б', 'в', 'В', 'г', 'Г', 'д', 'Д', 'е', 'Е', 'ж', 'Ж', 'з', 'З', 'и', 'И', 'й', 'Й', 'к', 'К', 'л', 'Л', 'м', 'М', 'н', 'Н', 'о', 'О', 'п', 'П', 'р', 'Р', 'с', 'С', 'т', 'Т', 'у', 'У', 'ф', 'Ф', 'х', 'Х', 'ц', 'Ц', 'ч', 'Ч', 'ш', 'Ш', 'щ', 'Щ', 'ъ', 'Ъ', 'ы', 'Ы', 'ь', 'Ь', 'э', 'Э', 'ю', 'Ю', 'я', 'Я',};

    public static String graphToZsStringRepresentation(
        final ImmutableGraph<Integer> graph
    ) {
        return graphToZsStringRepresentation(graph, 0);
    }

    public static String graphToZsStringRepresentation(
        final ImmutableGraph<Integer> graph, int rootNode
    ) {
        return graph.successors(rootNode).stream().flatMap(node -> {
            if (node >= characterLookup.length) {
                throw new IllegalArgumentException(String.format("Cannot stringify a tree of length > %d (node=%d)!",
                                                                 characterLookup.length - 1,
                                                                 node
                ));
            }
            Character nodeChar = characterLookup[node];
            if (graph.successors(node).isEmpty()) {
                return Stream.of(nodeChar.toString());
            } else {
                String subTree = graphToZsStringRepresentation(graph, node);
                return Stream.of(String.format("%s(%s)", nodeChar, subTree));
            }
        }).collect(Collectors.joining(" "));
    }
}
