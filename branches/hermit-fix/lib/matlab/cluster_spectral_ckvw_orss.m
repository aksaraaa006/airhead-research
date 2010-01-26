
% Spectral clustering, using CKVW tree and ORSS seeding.
% Cheng, Kannan, Vempala, and Wang. On a Recursive Spectral
%  Algorithm for Clustering from Pairwise Similarities.
%  Loc?, Yr?.
% Ostrovsky, Rabani, Schulman, and Swamy. The Effectiveness
%  of Lloyd-type Methods for the k-Means Problem. Loc?, Yr?.

% A is matrix, rows are unitized docs, cols are terms.
% Output is col vec of tags associated with each doc.

function z = cluster_spectral_ckvw_orss(A,k)
    % Make CKVW tree.
    T = cluster_spectral_ckvw_make_tree(A);
    % Find ORSS seeds.
    s = cluster_seed_orss(A,k);
    % Find maximum subtrees containing only one seed per
    %  subtree.
    W = alg_recurse(T,s);
    % Return as tags.
    m = size(A,1);
    z = zeros(m,1);
    for i = 1:length(W)
        for j = W{i}
            z(j) = i;
        end
    end
%

function Z = alg_recurse(T,s)
    if iscell(T)
        P = alg_recurse(T{1},s);
        Q = alg_recurse(T{2},s);
        if (~ iscell(P)) && (~ iscell(Q))
            R = [P Q];
            if length(intersect(R,s)) > 1
                Z = [{P} {Q}];
            else
                Z = R;
            end
        else
            if ~ iscell(P)
                P = {P};
            end
            if ~ iscell(Q)
                Q = {Q};
            end
            Z = [P Q];
        end
    else
        Z = T;
    end
%

% function Z = alg_add_to_cluster(x,A,C,s)
%     %Z = [{x} C];
%     %
%     j = 0; d = inf;
%     for i = 1:length(C)
%         seed = intersect(C{i},s);
%         if norm(A(
%     end
% %
