
% Spectral clustering.
% Cheng, Kannan, Vempala, and Wang. A Divide-and-Merge
%   Methodology for Clustering. ???

% A is unitized rows matrix, rows are docs, cols are terms.
% Output is col vec of tags associated with each doc.

function z = cluster_spectral_ckvw_merge_on_correlation(A,T,sep)
    merge_fn = @(U,B) merge_on_correlation_fn(U,B,sep);
    z = cluster_spectral_ckvw_merge(A,T,merge_fn);
%

% DP merge. sim if ab >= sep, dissim if ab < sep.
function Z = merge_on_correlation_fn(D,A,sep)
    Z = merge_on_correlation_fn_recpart(D,A,sep);
%

% DP merge. sim if ab >= sep, dissim if ab < sep.
function Z = merge_on_correlation_fn_recpart(D,A,sep)
    Z = ckvw_lr_dp_tree_min(D,A,sep,@correlation_dp_opt_fn);
%

function z = correlation_dp_opt_fn(C,A,sep)
    z = 0;
    for i = 1:length(C)
        % Intra-cluster dissimilarity.
        for a = 1:length(C{i})
            for b = (a+1):length(C{i})
                q = C{i}(a);
                r = C{i}(b);
                if A(q,:)*A(r,:)' < sep
                    z = z + 1;
                end
            end
        end
        % Inter-cluster similarity.
        for j = (i+1):length(C)
            for a = 1:length(C{i})
                for b = 1:length(C{j})
                    q = C{i}(a);
                    r = C{j}(b);
                    if A(q,:)*A(r,:)' >= sep
                        z = z + 1;
                    end
                end
            end
        end
    end
%
