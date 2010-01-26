
% Creates the permutation matrix of a permutation vector.

function A = pmat(v)
    n = length(v);
    A = zeros(n,n);
    for i = 1:n
        A(i,v(i)) = 1;
    end
%
