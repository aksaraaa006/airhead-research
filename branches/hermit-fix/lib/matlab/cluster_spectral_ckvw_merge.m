
% Spectral clustering.
% Cheng, Kannan, Vempala, and Wang. A Divide-and-Merge
%   Methodology for Clustering. Loc?, Yr?.

% A is matrix, rows are docs, cols are terms.
% T is unmerged tree.
% Output is col vec of tags associated with each doc.

function z = cluster_spectral_ckvw_merge(A,T,merge_fn)
    M = merge_fn(T,A);
    z = zeros(size(A,1),1);
    % Extract the tags from the merged tree.
    for i = 1:length(M)
        for j = 1:length(M{i})
            z(M{i}(j)) = i;
        end
    end
%
