
% Spectral clustering.
% Cheng, Kannan, Vempala, and Wang. A Divide-and-Merge
%   Methodology for Clustering. Loc?, Yr?.

% A is matrix, rows are docs, cols are terms.
% Output is unmerged tree.

function Z = cluster_spectral_ckvw_make_tree(A)
    m = size(A,1);
    Z = alg_divide(A,(1:m)');
%

% Z will be a binary tree of vector indices.
function Z = alg_divide(A,J)
    m = size(A,1);
    if m == 1
        % If only one index, return it.
        Z = J;
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
     Z = {alg_divide(B(1:t,:),J(W(1:t,2))) alg_divide(B((t+1):m,:),J(W((t+1):m,2)))};
%

function [v z] = alg_find_min_cut(A)
    m = size(A,1);
    w = zeros(1,m-1);
    for i = 1:(m-1)
        w(i) = conductance(A,1:i,setdiff(1:m,1:i));
    end
    [v z] = min(w);
%
