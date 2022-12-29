package pt.fct.nova.id.srv.application.query.plans;

import java.util.Set;

public record TreeNode<T>(T value, Set<TreeNode<T>> leaves, int depth) {

}
