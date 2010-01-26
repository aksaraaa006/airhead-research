
% Spectral clustering.
% Cheng, Kannan, Vempala, and Wang. A Divide-and-Merge
%   Methodology for Clustering. ???

% Q is matrix, rows are docs, cols are terms.
% Output is col vec of tags associated with each doc.

function z = cluster_spectral_ckvw_merge_on_kmeans(A,T,k)
    merge_fn = @(U,B) merge_on_kmeans_fn(U,B,k);
    z = cluster_spectral_ckvw_merge(A,T,merge_fn);
%

% DP merge.
function Z = merge_on_kmeans_fn(D,A,k)
    Z = merge_on_kmeans_fn_recpart(D,A,k);
%

% DP merge.
function Z = merge_on_kmeans_fn_recpart(D,A,k)
    Z = ckvw_ij_dp_tree_min(D,A,k,@kmeans_dp_opt_fn);
%

% Minimizes sum of squared distances, as in paper.
function z = kmeans_dp_opt_fn(C,A,junk)
    z = 0;
    for i = 1:length(C)
        if isempty(C{i}) ~= 1
            p = zeros(1,size(A,2));
            for a = 1:length(C{i})
                p = p + A(a,:);
            end
            p = p ./ length(C{i});
            for a = 1:length(C{i})
                z = z + (norm(p-A(a,:)).^2);
            end
        end
    end
%
