package com.stratocloud.utils;

import java.util.*;
import java.util.function.Function;

public class GraphUtil {
    public static <NODE> List<NODE> bfs(NODE root, Function<NODE, List<NODE>> neighborsGetter){
        List<NODE> result = new ArrayList<>();

        if(root == null)
            return result;

        Queue<NODE> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()){
            NODE current = queue.poll();

            if(result.contains(current)){
                continue;
            }

            result.add(current);

            List<NODE> children = neighborsGetter.apply(current);
            if(Utils.isNotEmpty(children)){
                children.forEach(queue::offer);
            }
        }

        return result;
    }
}
