
% Spectral clustering.
% Cheng, Kannan, Vempala, and Wang. A Divide-and-Merge
%   Methodology for Clustering. ???

% Q is matrix, rows are docs, cols are terms.
% al and ep are the bicriteria parameters for the algorithm;
%  set either to -1 to optimize for that parameter.
% Output is col vec of tags associated with each doc.

function z = cluster_spectral_ckvw_merge_on_alpha(A,T,alpha)
    merge_fn = @(U,B) merge_on_alpha_fn(U,B,alpha);
    z = cluster_spectral_ckvw_merge(A,T,merge_fn);
%

% Greedy merge.
function Z = merge_on_alpha_fn(D,A,alpha)
    [m n] = size(A);
    if iscell(D)
        P = merge_on_alpha_fn(D{1},A,alpha);
        Q = merge_on_alpha_fn(D{2},A,alpha);
        if isempty(P) || isempty(Q)
            Z = [];
        else
            if length(P) > 1 || length(Q) > 1
                Z = [P Q];
            else
                R = [P{1}; Q{1}];
                if conductance(A,R',setdiff(1:m,R')) >= alpha
                    Z = {R};
                else
                    Z = [P Q];
                end
            end
        end
    else
        if conductance(A,D,setdiff(1:m,D)) < alpha
            Z = [];
        else
            Z = {D};
        end
    end
%
