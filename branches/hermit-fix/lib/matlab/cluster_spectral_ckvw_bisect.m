
% Spectral clustering, using paper and Michael's bisecting
%  idea. Use bisecting cut with minimum conductance.
% Cheng, Kannan, Vempala, and Wang. On a Recursive Spectral
%  Algorithm for Clustering from Pairwise Similarities.
%  Loc?, Yr?.

% A is matrix, rows are unitized docs, cols are terms.
% Output is col vec of tags associated with each doc.

function z = cluster_spectral_ckvw_bisect(A,k)
    M = 1:size(A,1);
    C = {M};
    while length(C) < k
        % v is value of min phi cut; A, B are pieces of min phi cut.
        v = 0; d = 0; i = 0; Q = []; R = [];
        for j = 1:length(C)
            if length(C{j}) < 2
                continue
            end
            [w S T] = alg_do_min_cut(C{j},A);
            if d == 0 || w > v
                d = 1; v = w; i = j; Q = S; R = T;
            end
        end
        % Put C back together.
        C = [C(1:(i-1)) {Q} {R} C((i+1):length(C))];
    end
    z = zeros(size(A,1),1);
    for i = 1:k
        J = C{i};
        for j = J
            z(j) = i;
        end
    end
%

function [w S T] = alg_do_min_cut(c,O)
    A = O(c,:);
    m = size(A,1);
    if m == 1
        % If only one index, return it.
        w = 0;
        S = c;
        T = [];
        return
    end
    %%%
    AAT = A * A';
    rho = sum(AAT,2);
    pi = rho / sum(rho);
    R = diag(rho);
    D = diag(sqrt(pi));
    %[V S] = eig(D*inv(R)*AAT*inv(D));
    [V S] = eig_sort(D*inv(R)*AAT*inv(D));
    vp = V(:,2);
    % Sort indices by second eig, increasing.
    W = sortrows([(inv(D)*vp) (1:m)']);
    q = W(:,2); P = pmat(q);
    B = P * A;
    [w t] = alg_find_min_cut(B);
    %%%
%     %AAT = A * A';
%     AAT = normrows(A*A',1);
%     [V D] = eig_sort(AAT);
%     % Sort indices by second largest eig, increasing.
%     o = AAT * V(:,2);
%     W = sortrows([o (1:m)']);
%     q = W(:,2); P = pmat(q);
%     B = P * A;
%     [w t] = alg_find_min_cut(B);
    %%%
    % Keep the correct indices for the permuted matrix rows.
    S = c(q(1:t)); T = c(q((t+1):m));
%

function [v z] = alg_find_min_cut(A)
    m = size(A,1);
    w = zeros(1,m-1);
    for i = 1:(m-1)
        w(i) = conductance(A,1:i,setdiff(1:m,1:i));
    end
    [v z] = min(w);
%
