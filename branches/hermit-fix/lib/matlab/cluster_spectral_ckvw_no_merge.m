
% Spectral clustering.
% Cheng, Kannan, Vempala, and Wang. A Divide-and-Merge
%   Methodology for Clustering. ???

% Q is matrix, rows are docs, cols are terms.
% al and ep are the bicriteria parameters for the algorithm;
%  set either to -1 to optimize for that parameter.
% Output is col vec of tags associated with each doc.

function z = cluster_spectral_ckvw_no_merge(A,alpha)
    z = cluster_spectral_ckvw(A,0);
%
