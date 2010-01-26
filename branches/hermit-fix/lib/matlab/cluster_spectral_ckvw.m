
% Spectral clustering.
% Cheng, Kannan, Vempala, and Wang. A Divide-and-Merge
%   Methodology for Clustering. Loc?, Yr?.

% A is matrix, rows are docs, cols are terms.
% Output is col vec of tags associated with each doc.

function z = cluster_spectral_ckvw(A,merge_fn)
    [m n] = size(A);
    D = cluster_spectral_ckvw_divide(A,(1:m)');
    if isequal(merge_fn,0)
        % Return the entire tree.
        z = D;
        return
    end
    M = merge_fn(D,A);
    z = zeros(m,1);
    % Extract the tags from the merged tree.
    for i = 1:length(M)
        for j = 1:length(M{i})
            z(M{i}(j)) = i;
        end
    end
%

% Z will be a binary tree of vector indices.
function Z = cluster_spectral_ckvw_divide(A,J)
    [m n] = size(A);
    if m == 1
        % If only one index, return it.
        Z = J;
        return
    end
    AAT = A * A';
    rho = sum(AAT,2);
    pi = rho / sum(rho);
    R = diag(rho);
    D = diag(pi);
    [V S] = eig(D*inv(R)*AAT*inv(D));
    % Sort indices by second eig, increasing.
    W = sortrows([(inv(D)*V(:,2)) (1:m)']);
    P = pmat(W(:,2));
    B = P * A;
    t = min_phi_cut(B);
    % Keep the correct indices for the permuted matrix rows.
    Z = {cluster_spectral_ckvw_divide(B(1:t,:),J(W(1:t,2))) cluster_spectral_ckvw_divide(B((t+1):m,:),J(W((t+1):m,2)))};
%

function z = min_phi_cut(A)
    [m n] = size(A);
    w = zeros(1,m-1);
    for i = 1:(m-1)
        w(i) = conductance(A,1:i,setdiff(1:m,1:i));
    end
    [v z] = min(w);
%
