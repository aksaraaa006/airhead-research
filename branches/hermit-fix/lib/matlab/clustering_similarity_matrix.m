
% Similarity matrix.
% Input is set of row vecs.
% Output is matrix of similarities using cos(deg(a,b)) as the measure.

function Z = clustering_similarity_matrix(A)
    A = normrows(A,2);
    Z = A*A';
%
